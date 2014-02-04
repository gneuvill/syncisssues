package fr.syncissues.services

import biz.futureware.mantis.rpc.soap.client._
import fr.syncissues._
import fr.syncissues.model._
import fr.syncissues.utils.Conversions._
import java.net.URL
import java.util.concurrent.{ExecutorService, Executors}
import scala.collection.GenTraversableOnce
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext._
import scala.concurrent.Future
import scalaz.{\/, DisjunctionFunctions}
import scalaz.Scalaz._
import scalaz.\/._

case class Mantis(
  user: String,
  password: String,
  url: String = "http://localhost/mantisbt-1.2.12/api/soap/mantisconnect.php",
  executor: ExecutorService =
    Executors.newFixedThreadPool(4)) extends IssueService with ProjectService {

  implicit val exec = fromExecutor(executor, _.printStackTrace)

  private val mantisConnect = new MantisConnectLocator().getMantisConnectPort(new URL(url))

  private def tryOne[T](fun: => T): Throwable \/ T = fromTryCatch(fun)

  private def trySeveral[T](fun: => Seq[T]): Seq[Throwable \/ T] = // fromTryCatch(fun).sequence
    try {
      Vector() ++ fun map (_.right)
    } catch {
      case e: Exception => Vector(e.left)
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

  private def tryCreateProject(pr: Project): Throwable \/ Int = tryOne {
    mantisConnect.mc_project_add(user, password, toProjectData(pr))
  }

  private def tryDeleteProject(pr: Project) = tryOne {
    mantisConnect.mc_project_delete(user, password, pr.id)
  }

  private def tryIssue(issueId: String) = tryOne {
    mantisConnect.mc_issue_get(user, password, issueId.toInt)
  }

  private def tryIssues(project: Project, pageNb: Int = 1, perPage: Int = 100) =
    tryProjectId(project).toList flatMap { pid =>
      trySeveral {
        mantisConnect.mc_project_get_issues(user, password, pid, pageNb, perPage)
      }
    }

  private def tryCreate(is: Issue): Throwable \/ Int =
    for {
      pid <- tryProjectId(is.project)
      cat <- tryCat(pid)
      int <- tryOne {
        mantisConnect.mc_issue_add(user, password, toIssueData(cat, is))
      }
    } yield int

  private def tryClose(is: Issue) = {
    val cli = is.copy(state = "closed")
    val eiClosed =
      for {
        cat <- tryCat(is.project.id)
        closed <- tryOne {
          mantisConnect.mc_issue_update(user, password, cli.number, toIssueData(cat, cli))
        }
      } yield closed
    eiClosed fold(
      e => e.left,
      if (_) cli.right
      else new Exception("Issue could not be updated").left)
  }

  def projects = Future(tryProjects) map {
    _ map (_ map (_.toProject))
  }

  def createProject(pr: Project) = Future(tryCreateProject(pr)) map {
      _ map (Project(_, pr.name))
    }

  def deleteProject(pr: Project) = Future(tryDeleteProject(pr))

  def issue(issueId: String, project: Option[Project] = None) =
    Future(tryIssue(issueId)) map (_ map (_.toIssue))

  def issues(project: Project) =
    Future(tryIssues(project)) map { s ⇒
      for {
        ei ← s map (_ map (_.toIssue))
        if ei forall (_.state == "open")
      } yield ei
    }

  def createIssue(is: Issue) =
    withProjectId(is.project) { id =>
      Future(tryCreate(is.copy(project = Project(id, is.project.name)))).map[Throwable \/ Issue] {
        _ map {
          Issue(_, is.state, is.title, is.body, is.project.copy(id = id))
        }
      }
    }

  def closeIssue(is: Issue) = Future(tryClose(is))
}
