package fr.syncissues.services

import fr.syncissues._
import java.util.concurrent.ExecutorService
import model._
import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task
import utils.FJ._
import utils.Conversions._
import dispatch.{ url => durl, _ }
import scalaz._
import Scalaz._
import java.util.concurrent.Executors

case class GitHub(
  user: String,
  password: String,
  owner: String,
  url: String = "https://api.github.com",
  executor: ExecutorService = Executors.newFixedThreadPool(4)) extends IssueService with ProjectService {

  implicit val exec = ExecutionContext.fromExecutorService(executor)

  private val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes())

  private val headers = Map("Accept" -> "application/json", "Authorization" -> ("Basic " + auth))

  def projects = Http {
    durl(url) / "users" / owner / "repos" <:< headers OK as.Projects
  }.asTask

  def createProject(pr: Project) = Http {
    durl(url) / "user" / "repos" << pr.toJson <:< headers OK as.Project
  }.asTask

  def deleteProject(pr: Project) = Http {
    (durl(url) / "repos" / owner / pr.name).DELETE <:< headers > (_.getStatusCode == 204)
  }.asTask

  def issue(number: String, project: Option[Project]) =
    Task.now(project \/> (new Exception("Missing Project value"))) flatMap {
      _.fold(
        Task.fail,
        pr ⇒ Http {
          durl(url) / "repos" / owner / pr.name / "issues" / number <:< headers OK as.Issue
        }.asTask)
    }

  def issues(project: Project) = Http {
    durl(url) / "repos" / owner / project.name / "issues" <:< headers <<?
      Map("per_page" -> "100") OK as.Issues
  }.asTask

  def createIssue(is: Issue) = Http {
    durl(url) / "repos" / owner / is.project.name / "issues" << is.toJson <:< headers OK as.Issue
  }.asTask

  def closeIssue(is: Issue) =
    Http((durl(url) / "repos" / owner / is.project.name / "issues" / is.number.toString)
      .PATCH
      .setBody(is.copy(state = "closed").toJson) <:< headers OK as.Issue)
      .asTask
}
