package fr.syncissues.services

import fj.control.parallel.Promise
import fr.syncissues._
import beans._
import utils.FJ._

trait IssueService {
  def user: String
  def password: String
  def url: String

  def projects: Promise[Seq[Either[Throwable, Project]]]

  def exists(project: String, title: String): Promise[Either[Throwable, Boolean]] =
    issues(project) fmap { (s: Seq[Either[Throwable, Issue]]) =>
      (s map (_.right map (_.title == title))).fold[Either[Throwable, Boolean]](Right(true)) {
        (ei1, ei2) => ei1.right flatMap (b1 => ei2.right map (b2 => b1 && b2))
      }
    }

  def issue(project: String, id: String): Promise[Either[Throwable, Issue]]

  def issues(project: String): Promise[Seq[Either[Throwable, Issue]]]

  def createIssue(project: String, is: Issue): Promise[Either[Throwable, Issue]]

  def closeIssue(project: String, is: Issue): Promise[Either[Throwable, Issue]]
}



















