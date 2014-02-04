package fr.syncissues.services

import fr.syncissues._
import model._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalaz.{\/, \/-, -\/}
import scalaz.Scalaz._
import scalaz.std.anyVal.booleanInstance.disjunction

trait IssueService {
  def user: String
  def password: String
  def url: String

  def exists(is: Issue)(implicit e: ExecutionContext): Future[Throwable \/ Boolean] = {
    implicit val or = disjunction
    issues(is.project) map {
      _.map(_ map (_.title == is.title)).fold(false.right)(_ +++ _)
    }
  }

  def createIssue_?(is: Issue)(implicit e: ExecutionContext): Future[Throwable \/ Issue] =
    exists(is) flatMap { eib ⇒
      eib match {
        case \/-(false) => createIssue(is)
        case \/-(true) =>
          Future(new Exception("Issue already exists").left)
        case -\/(t) => t.printStackTrace; Future(t.left)
      }
    }

  def issue(id: String, project: Option[Project]): Future[Throwable \/ Issue]

  def issues(project: Project): Future[Seq[Throwable \/ Issue]]

  def createIssue(is: Issue): Future[Throwable \/ Issue]

  def closeIssue(is: Issue): Future[Throwable \/ Issue]
}
