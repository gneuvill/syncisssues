package fr.syncissues.services

import fr.syncissues._
import java.util.concurrent.ExecutorService
import model._
import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task
import utils.FJ._
import utils.Conversions._
import dispatch.{ url => durl, as ⇒ _, _ }
import scalaz._
import Scalaz._
import java.util.concurrent.Executors

case class GitHub(
  user: String,
  password: String,
  owner: String,
  url: String = "https://api.github.com",
  executor: ExecutorService = Executors.newFixedThreadPool(4)) extends IssueService with ProjectService {

  import GitHub._

  implicit val exec = ExecutionContext.fromExecutorService(executor)

  private val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes())

  private val headers = Map("Accept" -> "application/json", "Authorization" -> ("Basic " + auth))

  def projects = Http {
    durl(url) / "users" / owner / "repos" <:< headers OK as[Projects]
  }.asTask

  def createProject(pr: Project) = Http {
    durl(url) / "user" / "repos" << pr.toJson <:< headers OK as[Project]
  }.asTask

  def deleteProject(pr: Project) = {
    Http {
      (durl(url) / "repos" / owner / pr.name).DELETE <:< headers > (_.getStatusCode == 204)
    } map (_.right)
  }.asTask

  def issue(number: String, project: Option[Project]) =
    Task.now(project \/> (new Exception("Missing Project value"))) flatMap {
      _.fold(
        Task.fail,
        pr ⇒ Http {
          durl(url) / "repos" / owner / pr.name / "issues" / number <:< headers OK as[Issue]
        }.asTask map (_.copy(project = Project(pr.id, pr.name))))
    }

  def issues(prj: Project) = Http {
    durl(url) / "repos" / owner / prj.name / "issues" <:< headers <<?
      Map("per_page" -> "100") OK as[Issues]
  }.asTask map (_ map (_.copy(project = prj)))

  def createIssue(is: Issue) = Http {
    durl(url) / "repos" / owner / is.project.name / "issues" << is.toJson <:< headers OK as[Issue]
  }.asTask map (_.copy(project = is.project))

  def closeIssue(is: Issue) =
    Http((durl(url) / "repos" / owner / is.project.name / "issues" / is.number.toString)
      .PATCH
      .setBody(is.copy(state = "closed").toJson) <:< headers OK as[Issue])
      .asTask
}

object GitHub {
  import argonaut._, Argonaut._

  implicit def ProjectCodecJson: CodecJson[Project] =
    casecodec2(Project.apply, Project.unapply)("id", "name")

  implicit def IssueCodecJson: CodecJson[Issue] =
    casecodec4[Int, String, String, String, Issue](
      Issue(_, _, _, _, project = Project()),
      is ⇒
        (is.number,
          is.state,
          is.title,
          is.body).some)("number", "state", "title", "body")
}
