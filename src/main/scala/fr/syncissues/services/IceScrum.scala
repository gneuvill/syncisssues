package fr.syncissues.services

import fr.syncissues._
import model._
import utils.Conversions._
import utils.FJ._
import utils.json.Serializer
import dispatch.{Promise => _, url => durl, _}
import net.liftweb.json._
import FieldSerializer._
import net.liftweb.json.CustomSerializer

import fj.control.parallel.Promise
import Promise._
import fj.control.parallel.Strategy
import java.util.concurrent.Executors

case class IceScrum(
  user: String,
  password: String,
  team: String,
  url: String = "http://localhost:8181/icescrum/ws/p",
  strategy: Strategy[fj.Unit] = Strategy.executorStrategy[fj.Unit](
    Executors.newFixedThreadPool(4))) extends IssueService with ProjectService {

  implicit val strat = strategy
  
  implicit val issueSerializer = Serializer[Issue](
    DefaultFormats + new CustomSerializer[Issue](formats => (
      {
        case JObject(children) => {
          for {
            JField("id", JInt(id)) <- children
            JField("name", JString(name)) <- children
            JField("description", JString(descr)) <- children
            JField("state", JInt(state)) <- children
            JField("feature", JObject(JField("id", JInt(pid)) :: Nil)) <- children
            val Array(prName, isName) = name split (':')
          } yield Issue(id.toInt, if (state == 7) "closed" else "open", isName.tail, descr,
            Project(pid.toInt, prName))
        }.head
      },
      {
        case Issue(number, state, title, body, project) =>
          JObject(JField("story", JObject(List(
            JField("type", JInt(2)),
            JField("id", JInt(number)),
            JField("state", JInt(if (state == "closed") 7 else 1)),
            JField("name", JString(project.name + ": " + title)),
            JField("description", JString(body)),
            JField("feature", JObject(JField("id", JInt(project.id)) :: Nil))))) :: Nil)
      })))

  val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes)

  val headers = Map("Content-Type" -> "application/json", "Authorization" -> ("Basic " + auth))

  def projects =
    Http(durl(url) / team / "feature" <:< headers OK as.lift.Json).either.right map { jvalue =>
      for {
        JArray(jfeatures) <- jvalue
        jfeature <- jfeatures
      } yield jfeature transform {
        case JField("name", JString(s)) => JField("name", JString(s takeWhile (_ != ':')))
      }
    } map (_ fold (e => Seq(Left(e)), Seq() ++ _ map toProject))

  def createProject(pr: Project) =
    Http(durl(url) / team / "feature" << write(pr) <:< headers OK as.lift.Json)
      .either map (_.right flatMap toProject)

  def deleteProject(pr: Project) =
    Http((durl(url) / team / "feature" / pr.id.toString).DELETE <:< headers >
      (_.getStatusCode == 204)).either

  def issue(id: String, project: Option[Project] = None) =
    Http(durl(url) / team / "story" / id <:< headers OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def issues(project: Project) =
    Http(durl(url) / team / "story" <:< headers OK as.lift.Json).either.right map { jvalue =>
      for {
        JArray(jissues) <- jvalue
        jissue <- jissues
        if (jissue.children.size > 1 &&
          jissue \\ "state" != JInt(7) &&  // we want correct and opened issues only
          ((jissue \\ "name").toString startsWith project.name))
      } yield jissue
    } map (_ fold (e => Seq(Left(e)), Seq() ++ _ map toIssue))

  def createIssue(is: Issue) =
    Http(durl(url) / team / "story" <:< headers << write(is) OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def closeIssue(is: Issue) =
    Http {
      (durl(url) / team / "story" / is.number.toString / "done" <:< headers).POST OK as.lift.Json
    }.either map (_.right flatMap toIssue)

}
