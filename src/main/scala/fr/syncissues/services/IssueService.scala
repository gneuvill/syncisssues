package fr.syncissues.services

import dispatch.{Promise => _, _}

import fj.control.parallel.{Promise, Strategy}
import Promise._
import fr.syncissues._
import model._
import utils.FJ._

trait IssueService {
  def user: String
  def password: String
  def url: String

  implicit def strat: Strategy[fj.Unit]

  def exists(project: Project, title: String): Promise[Either[Throwable, Boolean]] =
    issues(project) fmap { (s: Seq[Either[Throwable, Issue]]) =>
      (s map (_.right map (_.title == title))).fold[Either[Throwable, Boolean]](Right(false)) {
        (ei1, ei2) => ei1.right flatMap (b1 => ei2.right map (b2 => b1 || b2))
      }
    }

  def createIssue_?(is: Issue): Promise[Either[Throwable, Issue]] =
    exists(is.project, is.title) bind { (eib: Either[Throwable, Boolean]) =>
      eib match {
        case Right(false) => createIssue(is)
        case Right(true) =>
          promise[Either[Throwable, Issue]](strat, Left(new Exception("Issue already exists")))
        case Left(t) => promise[Either[Throwable, Issue]](strat, Left(t))
      }
    }

  def issue(id: String, project: Option[Project]): Promise[Either[Throwable, Issue]]

  def issues(project: Project): Promise[Seq[Either[Throwable, Issue]]]

  def createIssue(is: Issue): Promise[Either[Throwable, Issue]]

  def closeIssue(is: Issue): Promise[Either[Throwable, Issue]]
}



















