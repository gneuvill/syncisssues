package fr.syncissues.services

import fj.control.parallel.{Promise, Strategy}
import Promise._
import fr.syncissues._
import model._
import utils.FJ._

trait IssueService {
  def user: String
  def password: String
  def url: String

  def exists(is: Issue): Promise[Either[Throwable, Boolean]] =
    issues(is.project) fmap { (s: Seq[Either[Throwable, Issue]]) =>
      (s map (_.right map (_.title == is.title))).fold[Either[Throwable, Boolean]](Right(false)) {
        (ei1, ei2) => for {
          b1 <- ei1.right
          b2 <- ei2.right
        } yield b1 || b2
      }
    }

  def createIssue_?(is: Issue)(implicit strat: Strategy[fj.Unit]): Promise[Either[Throwable, Issue]] =
    exists(is) bind { (eib: Either[Throwable, Boolean]) =>
      eib match {
        case Right(false) => createIssue(is)
        case Right(true) =>
          promise[Either[Throwable, Issue]](strat, Left(new Exception("Issue already exists")))
        case Left(t) => t.printStackTrace; promise[Either[Throwable, Issue]](strat, Left(t))
      }
    }

  def issue(id: String, project: Option[Project]): Promise[Either[Throwable, Issue]]

  def issues(project: Project): Promise[Seq[Either[Throwable, Issue]]]

  def createIssue(is: Issue): Promise[Either[Throwable, Issue]]

  def closeIssue(is: Issue): Promise[Either[Throwable, Issue]]
}
