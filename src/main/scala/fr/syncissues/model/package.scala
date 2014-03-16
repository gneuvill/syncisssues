package fr.syncissues

package object model {
  import argonaut._, Argonaut._
  import com.ning.http.client.Response
  import fr.syncissues.model._
  import scalaz.\/

  implicit class JsonProject(pr: Project)(implicit pdec: EncodeJson[Project]) {
    def toJson: String = pr.asJson.nospaces
  }

  implicit class JsonIssue(is: Issue)(implicit isdec: EncodeJson[Issue]) {
    def toJson: String = is.asJson.nospaces
  }

  type Projects = Vector[Project]
  type Issues = Vector[Issue]

  def as[T : DecodeJson](r: Response): Throwable \/ T = {
    // println(r.getResponseBody)
    Parse.decodeEither[T](r.getResponseBody).swapped(_ map (new Exception(_)))
  }
}
