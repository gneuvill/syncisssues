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
import biz.futureware.mantis.rpc.soap.client.MantisConnectLocator
import javax.xml.soap.SOAPFault
import biz.futureware.mantis.rpc.soap.client.IssueData

case class Mantis(
  user: String,
  password: String,
  project: String,
  url: String = "http://localhost/mantisbt-1.2.12/api/soap/mantisconnect.php",
  strategy: Strategy[fj.Unit] =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))) extends IssueService {

  require(
    try {
      project.toInt
      true
    } catch {
      case e: NumberFormatException => false
    }, "Project Ids are represented by numbers in Mantis !")

  private val mantisConnect = new MantisConnectLocator().getMantisConnectPort(new URL(url))

  val cat = mantisConnect.mc_project_get_categories(user, password, project.toInt)(0)

  private def tryIssue(issueId: String) =
    try {
      Right(mantisConnect.mc_issue_get(user, password, issueId.toInt))
    } catch {
      case e: Exception => Left(e)
    }

  private def tryIssues(pageNb: Int = 1, perPage: Int = 100) =
    try {
      Vector() ++ (mantisConnect.mc_project_get_issues(
        user, password, project.toInt, pageNb, perPage) map (Right(_)))
    } catch {
      case e: Exception => Vector(Left(e))
    }

  private def tryCreate(is: Issue): Either[Exception, Int] =
    try {
      Right(mantisConnect.mc_issue_add(user, password, toIssueData(project, cat, is)))
    } catch {
      case e: Exception => Left(e)
    }

  private def tryClose(is: Issue): Either[Exception, Issue] = {
    val cli = is.copy(state = "closed")
    try {
      if (mantisConnect.mc_issue_update(user, password, cli.number, toIssueData(project, cat, cli)))
        Right(cli)
      else Left(new Exception("Issue could not be updated"))
    } catch {
      case e: Exception => Left(e)
    }
  }

  def issue(issueId: String) =
    promise(strategy, tryIssue(issueId)) fmap ((_: Either[Exception, IssueData]).right map toIssue)

  def issues = promise[Vector[Either[Exception, IssueData]]](strategy, tryIssues()) fmap {
      l: Vector[Either[Exception, IssueData]] => l map (_.right map toIssue)
    }

  def createIssue(is: Issue) =
    promise(strategy, tryCreate(is)) fmap {
      (_: Either[Exception, Int]).right map (Issue(_, is.state, is.title, is.body))
    }

  def closeIssue(is: Issue) = promise(strategy, tryClose(is))

}
