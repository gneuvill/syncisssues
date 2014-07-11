package fr.syncissues.services

import dispatch.{ url => durl, as ⇒ _, _ }
import fr.syncissues._
import fr.syncissues.model._
import fr.syncissues.utils.Conversions._
import fr.syncissues.utils.json.Serializer
import java.util.concurrent.{ ExecutorService, Executors }
import scala.concurrent.ExecutionContext._

import scalaz.\/
import scalaz.Scalaz._
import scalaz.\/._
import scalaz.concurrent.Task

case class IceScrum(
  user: String,
  password: String,
  team: String,
  url: String = "http://localhost:8181/icescrum/ws/p",
  executor: ExecutorService = Executors.newFixedThreadPool(4)) extends IssueService with ProjectService {

  import IceScrum._

  implicit val exec = fromExecutor(executor, _.printStackTrace)

  val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes)

  val headers = Map("Content-Type" -> "application/json; charset=UTF-8", "Authorization" -> ("Basic " + auth))

  def storiesParams(projectId: Int) = Map(
    "withStories" → "false",
    "story.type" → "2",
    "story.feature" → projectId.toString)

  def projects = Http {
    durl(url) / team / "feature" <:< headers OK as[Projects]
  }.asTask

  def createProject(pr: Project) = Http {
    durl(url) / team / "feature" << pr.toJson <:< headers OK as[Project]
  }.asTask

  def deleteProject(pr: Project) = {
    Http {
      (durl(url) / team / "feature" / pr.id.toString).DELETE <:< headers > (_.getStatusCode == 204)
    } map (_.right)
  }.asTask

  def issue(id: String, project: Option[Project] = None) = for {
    is ← Http {
      durl(url) / team / "story" / id <:< headers OK as[Issue]
    }.asTask
    prName ← projectName(is.project)
  } yield is.copy(project = is.project.copy(name = prName))

  def issues(project: Project) = withProjectId(project) { id ⇒
    for {
      iss ← Http {
        durl(url) / team / "finder" <<? storiesParams(id) <:< headers OK as[Issues]
      }.asTask
      prName ← projectName(project)
    } yield iss map (_.copy(project = Project(id, prName)))
  }

  def createIssue(is: Issue) = withProjectId(is.project) { id ⇒
    val p = is.project.copy(id = id)
    Http {
      durl(url) / team / "story" <:< headers << is.copy(project = p).toJson OK as[Issue]
    }.asTask map (_.copy(project = p))
  }

  def closeIssue(is: Issue) =
    Http {
      (durl(url) / team / "story" / is.number.toString / "done" <:< headers).POST OK as[Issue]
    }.asTask
}

object IceScrum {
  import argonaut._, Argonaut._

  implicit class IceScrumState(s: String) {
    def asIceScrum: Int = if (s == "open") 1 else 7
  }

  implicit def ProjectCodecJson: DecodeJson[Project] =
    DecodeJson(c ⇒ for {
      id ← (c --\ "id").as[Int]
      name ← (c --\ "name").as[String].option
    } yield Project(id, name getOrElse ""))

  implicit def IssueCodecJson: DecodeJson[Issue] =
    jdecode5L[Int, Int, String, String, Project, Issue] {
      (id, st, n, d, p) ⇒ Issue(id, st.toString, n, d, p)
    }("id", "state", "name", "description", "feature")

  implicit def IssuesCodecJson: DecodeJson[Issues] =
    DecodeJson(c ⇒ for {
      stories ← (c --\ "stories").as[Issues]
      val temp = println(c)
    } yield stories)

  implicit def ProjectEncodeJson: EncodeJson[Project] =
    jencode2L((p: Project) ⇒ (p.id, p.name))("id", "name")

  implicit def IssueEncodeJson: EncodeJson[Issue] =
    EncodeJson(is ⇒
      Json("story" :=
        Json("id" := is.number,
          "state" := is.state.asIceScrum,
          "name" := is.title,
          "description" := is.body,
          "feature" := is.project,
          "type" := 2)))
}
