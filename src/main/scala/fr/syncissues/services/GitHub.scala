package fr.syncissues.services

import fr.syncissues._
import java.util.concurrent.ExecutorService
import model._
import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task
import utils.FJ._
import utils.Conversions._
import dispatch.{ url => durl, _ }
import net.liftweb.json._
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

  private def withProject(pr: Project)(json: JValue): JValue = json ++ JField("project",
    JObject(JField("id", JInt(pr.id)) :: JField("name", JString(pr.name)) :: Nil))

  def projects =
    Http(durl(url) / "users" / owner / "repos" <:< headers OK as.lift.Json)
      .asTask
      .map { jvalue =>
        (for {
          JArray(jprojects) <- jvalue
          jproject <- jprojects
        } yield jproject.toProject).toVector
      }

  def createProject(pr: Project) =
    Http(durl(url) / "user" / "repos" << write(pr) <:< headers OK as.lift.Json)
      .asTask
      .map(_.toProject)

  def deleteProject(pr: Project) =
    Http((durl(url) / "repos" / owner / pr.name).DELETE <:< headers >
      (_.getStatusCode == 204)).asTask

  def issue(number: String, project: Option[Project]) =
    Task.now(project \/> (new Exception("Missing Project value"))) flatMap {
      _.fold(Task.fail, pr ⇒
        Http(durl(url) / "repos" / owner / pr.name / "issues" / number <:< headers OK as.lift.Json)
          .asTask
          .map(_.toIssue))
    }

  def issues(project: Project) =
    Http(durl(url) / "repos" / owner / project.name / "issues" <:< headers <<?
      Map("per_page" -> "100") OK as.lift.Json)
      .asTask
      .map { jvalue =>
        (for {
          JArray(jissues) <- jvalue
          jissue <- jissues
        } yield jissue.toIssue).toVector
      }

  def createIssue(is: Issue) =
    Http {
      (durl(url) / "repos" / owner / is.project.name / "issues" << write(is) <:< headers) OK as.lift.Json
    }.asTask map (_.toIssue)

  def closeIssue(is: Issue) =
    Http((durl(url) / "repos" / owner / is.project.name / "issues" / is.number.toString)
      .PATCH
      .setBody(write(is.copy(state = "closed"))) <:< headers OK as.lift.Json)
      .asTask
      .map(_.toIssue)
}
