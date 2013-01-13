package fr.syncissues.services

import fj.control.parallel.Promise
import fr.syncissues.beans.Issue

trait IssueService {
  def user: String
  def password: String
  def project: String
  def url: String

  def issue(id: String): Promise[Either[Throwable, Issue]]

  def issues: Promise[Vector[Either[Throwable, Issue]]]

  def createIssue(is: Issue): Promise[Either[Throwable, Issue]]

  def closeIssue(is: Issue): Promise[Either[Throwable, Issue]]
}



















