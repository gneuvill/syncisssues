package fr.syncissues.services

import fr.syncissues._
import beans.Issue
import Issue._
import utils.FJ._
import utils.Conversions._

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
  strategy: Strategy[fj.Unit] =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))) extends IssueService {

  private val mantisConnect = new MantisConnectLocator().getMantisConnectPort(new URL(url))

  private def cat(project: String) = mantisConnect.mc_project_get_categories(user, password, project.toInt)(0)

  private def tryProjects =
    try {
      Seq() ++ mantisConnect.mc_projects_get_user_accessible(user, password) map (Right(_))
    } catch {
      case e: Exception => Seq(Left(e))
    }

  private def tryIssue(issueId: String) =
    try {
      Right(mantisConnect.mc_issue_get(user, password, issueId.toInt))
    } catch {
      case e: Exception => Left(e)
    }

  private def tryIssues(project: String, pageNb: Int = 1, perPage: Int = 100) =
    try {
      Vector() ++ (mantisConnect.mc_project_get_issues(
        user, password, project.toInt, pageNb, perPage) map (Right(_)))
    } catch {
      case e: Exception => Vector(Left(e))
    }

  private def tryCreate(project: String, is: Issue): Either[Exception, Int] =
    try {
      Right(mantisConnect.mc_issue_add(
        user,
        password,
        toIssueData(project.toInt, cat(project), is)))
    } catch {
      case e: Exception => Left(e)
    }

  private def tryClose(project: String, is: Issue): Either[Exception, Issue] = {
    val cli = is.copy(state = "closed")
    try {
      val closed = mantisConnect.mc_issue_update(
        user,
        password,
        cli.number,
        toIssueData(project.toInt, cat(project), cli))
      if (closed) Right(cli)
      else Left(new Exception("Issue could not be updated"))
    } catch {
      case e: Exception => Left(e)
    }
  }

  def projects = promise[Seq[Either[Exception, ProjectData]]](strategy, tryProjects) fmap {
      l: Seq[Either[Exception, ProjectData]] => l map (_.right map toProject)
    }

  def issue(project: String, issueId: String) =
    promise(strategy, tryIssue(issueId)) fmap ((_: Either[Exception, IssueData]).right map toIssue)

  def issues(project: String) = promise[Seq[Either[Exception, IssueData]]](strategy, tryIssues(project)) fmap {
      l: Seq[Either[Exception, IssueData]] => l map (_.right map toIssue)
    }

  def createIssue(project: String, is: Issue) =
    promise(strategy, tryCreate(project, is)) fmap {
      (_: Either[Exception, Int]).right map (Issue(_, is.state, is.title, is.body))
    }

  def closeIssue(project: String, is: Issue) = promise(strategy, tryClose(project, is))

}
