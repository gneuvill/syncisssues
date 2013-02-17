package fr.syncissues.services

import fr.syncissues._
import model._
import utils.FJ._
import utils.Conversions._

import scala.collection.GenTraversableOnce
import fj.control.parallel.Promise
import Promise._
import fj.control.parallel.Strategy
import java.util.concurrent.Executors
import java.net.URL
import javax.xml.soap.SOAPFault
import biz.futureware.mantis.rpc.soap.client._

case class Mantis(
  user: String,
  password: String,
  url: String = "http://localhost/mantisbt-1.2.12/api/soap/mantisconnect.php",
  strategy: Strategy[fj.Unit] = Strategy.executorStrategy[fj.Unit](
    Executors.newFixedThreadPool(4))) extends IssueService with ProjectService {

  private val mantisConnect = new MantisConnectLocator().getMantisConnectPort(new URL(url))

  implicit val strat = strategy

  private def tryOne[T](fun: => T): Either[Exception, T] =
    try {
      Right(fun)
    } catch {
      case e: Exception => Left(e)
    }

  private def trySeveral[T](fun: => GenTraversableOnce[T]): Seq[Either[Exception, T]] =
    try {
      Seq() ++ fun map (Right(_))
    } catch {
      case e: Exception => Seq(Left(e))
    }

  private def tryCat(projectId: Int) = tryOne {
    mantisConnect.mc_project_get_categories(user, password, projectId)(0)
  }

  private def tryProjectId(project: Project) = tryOne {
    mantisConnect.mc_project_get_id_from_name(user, password, project.name)
  }

  private def tryProjects = trySeveral {
    mantisConnect.mc_projects_get_user_accessible(user, password)
  }

  private def tryCreateProject(pr: Project): Either[Exception, Int] = tryOne {
    mantisConnect.mc_project_add(user, password, toProjectData(pr))
  }

  private def tryDeleteProject(pr: Project) = tryOne {
    mantisConnect.mc_project_delete(user, password, pr.id)
  }

  private def tryIssue(issueId: String) = tryOne {
    mantisConnect.mc_issue_get(user, password, issueId.toInt)
  }

  private def tryIssues(project: Project, pageNb: Int = 1, perPage: Int = 100) =
      tryProjectId(project).right.toSeq flatMap { pid =>
        trySeveral {
          mantisConnect.mc_project_get_issues(user, password, pid, pageNb, perPage)
        }
      }

  private def tryCreate(is: Issue): Either[Exception, Int] =
    for {
      pid <- tryProjectId(is.project).right
      cat <- tryCat(pid).right
      int <- tryOne {
        mantisConnect.mc_issue_add(user, password, toIssueData(cat, is))
      }.right
    } yield int

  private def tryClose(is: Issue) = {
    val cli = is.copy(state = "closed")
    val eiClosed =
      for {
        cat <- tryCat(is.project.id).right
        closed <- tryOne {
          mantisConnect.mc_issue_update(user, password, cli.number, toIssueData(cat, cli))
        }.right
      } yield closed
    eiClosed fold (
      e => Left(e),
      if (_) Right(cli)
      else Left(new Exception("Issue could not be updated")))
  }

  def projects = promise[Seq[Either[Exception, ProjectData]]](strategy, tryProjects) fmap {
      l: Seq[Either[Exception, ProjectData]] => l map (_.right map toProject)
    }

  def createProject(pr: Project) =
    promise(strat, tryCreateProject(pr)) fmap {
      (_: Either[Exception, Int]).right map (Project(_, pr.name))
    }

  def deleteProject(pr: Project) = promise(strat, tryDeleteProject(pr))

  def issue(issueId: String, project: Option[Project] = None) =
    promise(strategy, tryIssue(issueId)) fmap ((_: Either[Exception, IssueData]).right map toIssue)

  def issues(project: Project) =
    promise[Seq[Either[Exception, IssueData]]](strategy, tryIssues(project)) fmap {
      l: Seq[Either[Exception, IssueData]] => l map (_.right map toIssue)
    }

  def createIssue(is: Issue) =
    promise(strategy, tryCreate(is)) fmap {
      (_: Either[Exception, Int]).right map (Issue(_, is.state, is.title, is.body, is.project))
    }

  def closeIssue(is: Issue) = promise(strategy, tryClose(is))

}
