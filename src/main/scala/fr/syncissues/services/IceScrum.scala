package fr.syncissues.services

import fr.syncissues._
import beans.Issue
import utils.Conversions._
import utils.FJ._
import dispatch.{Promise => _, url => durl, _}
import net.liftweb.json._
import Serialization.write
import FieldSerializer._
import net.liftweb.json.CustomSerializer

import fj.control.parallel.Promise
import Promise._
import fj.control.parallel.Strategy
import java.util.concurrent.Executors

case class IceScrum(
  user: String,
  password: String,
  project: String,
  url: String = "http://localhost:8181/icescrum/ws/p",
  strategy: Strategy[fj.Unit] = Strategy.executorStrategy[fj.Unit](
    Executors.newFixedThreadPool(4))) extends IssueService {

  implicit val strat = strategy
  
  implicit val formats = DefaultFormats + new CustomSerializer[Issue](formats => (
    {
      case JObject(children) =>
        (for {
          JField("id", JInt(id)) <- children
          JField("name", JString(name)) <- children
          JField("description", JString(descr)) <- children
          JField("state", JInt(state)) <- children
        } yield Issue(id.toInt, if (state == 7) "closed" else "open", name, descr)).head
    },
    {
      case Issue(number, state, title, body) =>
        JObject(JField("story", JObject(List(
          JField("type", JInt(2)),
          JField("id", JInt(number)),
          JField("state", JInt(if (state == "closed") 7 else 1)),
          JField("name", JString(title)),
          JField("description", JString(body))))) :: Nil)
    }))

  val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes)

  val headers = Map("Content-Type" -> "application/json", "Authorization" -> ("Basic " + auth))

  def issue(id: String) =
    Http(durl(url) / project / "story" / id <:< headers OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def issues = {
    for {
      jvalue <- Http(durl(url) / project / "story" <:< headers OK as.lift.Json)
      JArray(jissues) <- jvalue
      jissue <- jissues
      if jissue.children.size > 1 && jissue \\ "state" != JInt(7) // we want correct and opened issues only
    } yield Http.promise(jissue).either map (_.right flatMap toIssue)
  } map (Vector() ++ _)

  def createIssue(is: Issue) =
    Http(durl(url) / project / "story" <:< headers << write(is) OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def closeIssue(is: Issue) =
    Http {
      (durl(url) / project / "story" / is.number.toString / "done" <:< headers).POST OK as.lift.Json
    }.either map (_.right flatMap toIssue)

}
