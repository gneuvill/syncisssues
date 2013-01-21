package fr.syncissues.services

import dispatch.{Promise => _, _}

import fj.control.parallel.{Promise, Strategy}
import Promise._
import fr.syncissues._
import beans._
import utils.FJ._

trait IssueService {
  def user: String
  def password: String
  def url: String

  implicit def strat: Strategy[fj.Unit]

  def exists(project: String, title: String): Promise[Either[Throwable, Boolean]] =
    issues(project) fmap { (s: Seq[Either[Throwable, Issue]]) =>
      (s map (_.right map (_.title == title))).fold[Either[Throwable, Boolean]](Right(false)) {
        (ei1, ei2) => ei1.right flatMap (b1 => ei2.right map (b2 => b1 || b2))
      }
    }

  def createIssue_?(project: String, is: Issue): Promise[Either[Throwable, Issue]] =
    exists(project, is.title) bind { (eib: Either[Throwable, Boolean]) =>
      eib match {
        case Right(false) => createIssue(project, is)
        case Right(true) =>
          promise[Either[Throwable, Issue]](strat, Left(new Exception("Issue already exists")))
        case Left(t) => promise[Either[Throwable, Issue]](strat, Left(t))
      }
    }

  def projects: Promise[Seq[Either[Throwable, Project]]]

  def issue(project: String, id: String): Promise[Either[Throwable, Issue]]

  def issues(project: String): Promise[Seq[Either[Throwable, Issue]]]

  def createIssue(project: String, is: Issue): Promise[Either[Throwable, Issue]]

  def closeIssue(project: String, is: Issue): Promise[Either[Throwable, Issue]]
}



















