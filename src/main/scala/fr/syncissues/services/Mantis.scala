package fr.syncissues.services

import biz.futureware.mantis.rpc.soap.client._
import fr.syncissues._
import fr.syncissues.model._
import fr.syncissues.utils.Conversions._
import java.net.URL
import java.util.concurrent.{ ExecutorService, Executors }
import scala.concurrent.ExecutionContext
import ExecutionContext._
import scala.concurrent.Future
import scalaz.concurrent.Task
import scalaz.{ \/, DisjunctionFunctions }
import scalaz.Scalaz._
import scalaz.\/._

case class Mantis(
  user: String,
  password: String,
  url: String = "http://localhost/mantisbt-1.2.12/api/soap/mantisconnect.php",
  executor: ExecutorService = Executors.newFixedThreadPool(4)) extends IssueService with ProjectService {

  implicit val exec = fromExecutor(executor, _.printStackTrace)

  private val mantisConnect = new MantisConnectLocator().getMantisConnectPort(new URL(url))

  private def mtFirstCat(projectId: Int) = Task {
    mantisConnect.mc_project_get_categories(user, password, projectId)(0)
  }

  private def mtProjectId(project: Project) = Task {
    mantisConnect.mc_project_get_id_from_name(user, password, project.name)
  }

  private def mtProjects = Task {
    mantisConnect.mc_projects_get_user_accessible(user, password)
  }

  private def mtCreateProject(pr: Project) = Task {
    mantisConnect.mc_project_add(user, password, toProjectData(pr))
  }

  private def mtDeleteProject(pr: Project) = Task {
    mantisConnect.mc_project_delete(user, password, pr.id)
  }

  private def mtIssue(issueId: String) = Task {
    mantisConnect.mc_issue_get(user, password, issueId.toInt)
  }

  private def mtIssues(project: Project, pageNb: Int = 1, perPage: Int = 100) =
    mtProjectId(project) flatMap { pid =>
      Task {
        mantisConnect.mc_project_get_issues(user, password, pid, pageNb, perPage)
      }
    }

  private def mtCreate(is: Issue) =
    for {
      pid <- mtProjectId(is.project)
      cat <- mtFirstCat(pid)
      int <- Task {
        mantisConnect.mc_issue_add(user, password, toIssueData(cat, is))
      }
    } yield int

  private def mtClose(is: Issue) = {
    val cli = is.copy(state = "closed")
    val willBeClosed =
      for {
        cat <- mtFirstCat(is.project.id)
        closed <- Task {
          mantisConnect.mc_issue_update(user, password, cli.number, toIssueData(cat, cli))
        }
      } yield closed
    willBeClosed flatMap {
      if (_) Task.now(is) else Task.fail(new Exception("Issue could not be closed"))
    }
  }

  def projects = mtProjects map (_.toVector map (_.toProject))

  def createProject(pr: Project) = mtCreateProject(pr) map (Project(_, pr.name))

  def deleteProject(pr: Project) = mtDeleteProject(pr)

  def issue(issueId: String, project: Option[Project] = None) =
    mtIssue(issueId) map (_.toIssue)

  def issues(project: Project) =
    mtIssues(project) map { s ⇒
      for {
        is ← s.toVector map (_.toIssue)
        if is.state == "open"
      } yield is
    }

  def createIssue(is: Issue) =
    withProjectId(is.project) { id =>
      mtCreate(is.copy(project = Project(id, is.project.name))) map {
        Issue(_, is.state, is.title, is.body, is.project.copy(id = id))
      }
    }

  def closeIssue(is: Issue) = mtClose(is)
}
