package fr.syncissues.services

import fr.syncissues._
import model._
import utils.FJ._
import utils.Conversions._
import dispatch.{Promise => _, url => durl, _}
import net.liftweb.json._

import fj.control.parallel.Promise
import Promise._
import fj.control.parallel.Strategy
import java.util.concurrent.Executors

case class GitHub(
  user: String,
  password: String,
  owner: String,
  url: String = "https://api.github.com",
  strategy: Strategy[fj.Unit] = Strategy.executorStrategy[fj.Unit](
      Executors.newFixedThreadPool(4))) extends IssueService with ProjectService {

  implicit val strat = strategy

  private val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes())

  private val headers = Map("Accept" -> "application/json", "Authorization" -> ("Basic " + auth))

  private def withProject(pr: Project)(json: JValue): JValue = json ++ JField("project",
    JObject(JField("id", JInt(pr.id)) :: JField("name", JString(pr.name)) :: Nil))

  def projects =
    Http(durl(url) / "users" / owner / "repos" <:< headers OK as.lift.Json).either.right map { jvalue =>
    for {
      JArray(jprojects) <- jvalue
      jproject <- jprojects
    } yield jproject
  } map (_ fold (e => Vector(Left(e)), Vector() ++ _ map toProject))

  def createProject(pr: Project) =
    Http(durl(url) / "user" / "repos" << write(pr) <:< headers OK as.lift.Json)
      .either map (_.right flatMap toProject)

  def deleteProject(pr: Project) =
    Http((durl(url) / "repos" / owner / pr.name).DELETE <:< headers > (_.getStatusCode == 204)).either

  def issue(number: String, project: Option[Project]) =
    project.toRight(new Exception("Missing Project value")).right
      .map(pr => Http(durl(url) / "repos" / owner / pr.name / "issues" / number <:< headers OK as.lift.Json).either)
      .fold(e => Http.promise(Left(e)), _ map (_.right flatMap (withProject(project.get) _ andThen toIssue)))

  def issues(project: Project) =
    Http(durl(url) / "repos" / owner / project.name / "issues" <:< headers <<? Map("per_page" -> "100") OK as.lift.Json)
      .either.right map  { jvalue =>
      for {
        JArray(jissues) <- jvalue
        jissue <- jissues
      } yield jissue
    } map (_ fold (e => Vector(Left(e)), Vector() ++ _ map (withProject(project) _ andThen toIssue)))

  def createIssue(is: Issue) =
    Http(durl(url) / "repos" / owner / is.project.name / "issues" << write(is) <:< headers OK as.lift.Json)
      .either map (_.right flatMap (withProject(is.project) _ andThen toIssue))

  def closeIssue(is: Issue) =
      Http((durl(url) / "repos" / owner / is.project.name / "issues" / is.number.toString)
        .PATCH
        .setBody(write(is.copy(state = "closed"))) <:< headers OK as.lift.Json)
        .either map (_.right flatMap (withProject(is.project) _ andThen toIssue))
}
