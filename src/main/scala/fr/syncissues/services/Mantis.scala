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

object Mantis {

  val pool = Executors.newFixedThreadPool(4)

  val strat = Strategy.executorStrategy[fj.Unit](pool)

  private val mantis = new MantisConnectLocator().getMantisConnectPort(new URL("http://localhost/mantisbt-1.2.12/api/soap/mantisconnect.php"))

  private def tryIssue(username: String, password: String, issueId: Int): Either[Exception, IssueData] =
    try {
      Right(mantis.mc_issue_get(username, password, issueId))
    } catch {
      case e: Exception => Left(e)
    }

  private def tryIssues(
    username: String, password: String, projectId: Int, pageNb: Int = 1, perPage: Int = 100): Either[Exception, Array[IssueData]] =
    try {
      Right(mantis.mc_project_get_issues(username, password, projectId, pageNb, perPage))
    } catch {
      case e: Exception => Left(e)
    }

  def issue(username: String, password: String, issueId: Int): Promise[Either[Exception, Issue]] =
    promise(strat, tryIssue(username, password, issueId)) fmap ((ei: Either[Exception, IssueData]) => ei.right map toIssue)

}
