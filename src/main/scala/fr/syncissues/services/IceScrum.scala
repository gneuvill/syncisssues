package fr.syncissues.services

import fr.syncissues._
import beans.Issue
import utils.Conversions._
import dispatch._
import net.liftweb.json._
import FieldSerializer._
import net.liftweb.json.CustomSerializer

object IceScrum {

  private implicit val formats = DefaultFormats + new CustomSerializer[Issue](formats => (
    {
      case JObject(children) =>
        (for {
          JField("id", JInt(id)) <- children
          JField("name", JString(name)) <- children
          JField("description", JString(descr)) <- children
          JField("state", JInt(state)) <- children
        } yield (Issue(id.toInt, if (state == 7) "closed" else "open", name, descr))) head
    },
    {
      case Issue(number, state, title, body) =>
        JObject(List(JField("id", JInt(number)), JField("state", JInt(state.toInt)),
          JField("name", JString(title)), JField("description", JString(body))))
    }
  ))

  private val icescrum  = "http://localhost:8181/icescrum/ws/p"

  private val auth = new sun.misc.BASE64Encoder().encode("gneuvill:toto".getBytes())

  private val issuesReq = (project: String) => 
    url(icescrum).addHeader("Content-Type", "application/json").addHeader("Authorization", "Basic " + auth) / project / "story"

  def story(project: String, id: String): Promise[Either[MappingException, Issue]] =
    Http(issuesReq(project) / id OK as.lift.Json) map toIssue

  /**
    * TODO : find why we have to filter the 'Right' values
    * 
    */
  def stories(project: String): Promise[List[Either[MappingException, Issue]]] =
    (for {
      jvalue <- Http(issuesReq(project) OK as.lift.Json)
      JArray(jissue) <- jvalue
    } yield (Http.promise(jissue))) map (_.foldLeft(Nil: List[JValue])(_ ++: _) map toIssue) map (_ filter (_ isRight))
}
