package fr.syncissues.services

import dispatch.{ url => durl, _ }
import fr.syncissues._
import fr.syncissues.model._
import fr.syncissues.utils.Conversions._
import fr.syncissues.utils.json.Serializer
import java.util.concurrent.{ ExecutorService, Executors }
import net.liftweb.json._
import net.liftweb.json.FieldSerializer._
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

  implicit val exec = fromExecutor(executor, _.printStackTrace)

  implicit val issueSerializer = Serializer[Issue](
    DefaultFormats + new CustomSerializer[Issue](formats ⇒ (
      {
        case jo @ JObject(children) if !children.isEmpty ⇒
          val JInt(id) = (jo \ "id").toOpt getOrElse JInt(9999)
          val JString(name) = (jo \ "name").toOpt getOrElse JString("NO-NAME")
          val JString(descr) = (jo \ "description") find (_ != JNull) getOrElse JString("")
          val JInt(state) = (jo \ "state").toOpt getOrElse JInt(-1)
          val JObject(List(JField("id", JInt(pid)), _*)) =
            (jo \ "feature").toOpt getOrElse JObject(JField("id", JInt(9999)) :: Nil) // just for fun, don't need it (we don't do it in github)
          val Array(prName, isName) = if (name contains ":") name split (':') else Array("", name)
          Issue(id.toInt, if (state == 7) "closed" else "open", isName.trim, descr,
            Project(pid.toInt, prName))
      },
      {
        case Issue(number, state, title, body, project) ⇒
          JObject(JField("story", JObject(List(
            JField("type", JInt(2)),
            JField("id", JInt(number)),
            JField("state", JInt(if (state == "closed") 7 else 1)),
            JField("name", JString(project.name + ": " + title)),
            JField("description", JString(body)),
            JField("feature", JObject(JField("id", JInt(project.id)) :: Nil))))) :: Nil)
      })))

  val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes)

  val headers = Map("Content-Type" -> "application/json; charset=UTF-8", "Authorization" -> ("Basic " + auth))

  def projects = {
    val projectsList: Task[List[Project]] =
      Http(durl(url) / team / "feature" <:< headers OK as.lift.Json)
        .asTask
        .map(for {
          JArray(jfeatures) ← _
          jfeature ← jfeatures
          if (jfeature \\ "name").toOpt exists {
            case JString(name) ⇒ !name.isEmpty
            case _ ⇒ false
          }
        } yield {
          val jf = jfeature transform {
            case JField("name", JString(s)) ⇒
              JField("name", JString(s.takeWhile(_ != ':').trim))
          }
          jf.toProject
        })
    projectsList map (_.toVector)
  }

  def createProject(pr: Project) =
    Http(durl(url) / team / "feature" << write(pr) <:< headers OK as.lift.Json)
      .asTask
      .map(_.toProject)

  def deleteProject(pr: Project) =
    Http((durl(url) / team / "feature" / pr.id.toString).DELETE <:< headers >
      (_.getStatusCode == 204)).asTask

  def issue(id: String, project: Option[Project] = None) =
    Http(durl(url) / team / "story" / id <:< headers OK as.lift.Json)
      .asTask
      .map(_.toIssue)

  def issues(project: Project) = {
    withProjectId(project) { id ⇒
      Http(durl(url) / team / "story" <:< headers OK as.lift.Json)
        .asTask
        .map { jvalue ⇒
          (for {
            JArray(jissues) ← jvalue
            jissue ← jissues
            if {
              jissue.children.size > 1 &&
                jissue \\ "type" == JInt(2) && // 'défaut' (bug) type
                jissue \\ "state" != JInt(7) && // we want correct and opened issues only
                ((jissue \\ "feature").toOpt exists {
                  case JObject(List(JField("id", JInt(pid)), _*)) ⇒ pid == id
                  case _ ⇒ false
                })
            }
          } yield jissue.toIssue).toVector
        }
    }
  }

  def createIssue(is: Issue) =
    withProjectId(is.project) { id ⇒
      Http {
        durl(url) / team / "story" <:< headers <<
          write(is.copy(project = Project(id, is.project.name))) OK as.lift.Json
      }.asTask map (_.toIssue)
    }

  def closeIssue(is: Issue) =
    Http {
      (durl(url) / team / "story" / is.number.toString / "done" <:<
        headers).POST OK as.lift.Json
    }.asTask map (_.toIssue)

}
