package fr.syncissues.services

import fr.syncissues._
import model._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalaz._
import Scalaz._
import scalaz.concurrent.Task
import scalaz.std.anyVal.booleanInstance.disjunction

trait IssueService {
  def user: String
  def password: String
  def url: String

  def exists(is: Issue): Task[Boolean] = {
    implicit val isEq: Equal[Issue] = Cord.CordEqual.contramap(_.title) 
    issues(is.project) map (_ element is)
  }

  def createIssue_?(is: Issue): Task[Issue] =
    exists(is) flatMap {
      if (_)
        Task.fail(new Exception("Issue already exists"))
      else
        createIssue(is)
    }

  def issue(id: String, project: Option[Project] = None): Task[Issue]

  def issues(project: Project): Task[Vector[Issue]]

  def createIssue(is: Issue): Task[Issue]

  def closeIssue(is: Issue): Task[Issue]
}
