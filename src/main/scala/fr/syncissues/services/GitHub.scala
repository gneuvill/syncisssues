package fr.syncissues.services

import fr.syncissues._
import beans.Issue
import utils.FJ._
import utils.Conversions._
import dispatch.{Promise => _, url => durl, _}
import net.liftweb.json._
import Serialization.write

import fj.control.parallel.Promise
import Promise._
import fj.control.parallel.Strategy
import java.util.concurrent.Executors

case class GitHub(
  user: String,
  password: String,
  owner: String,
  url: String = "https://api.github.com",
  strategy: Strategy[fj.Unit] =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))) extends IssueService {

  private implicit val formats = DefaultFormats

  implicit val strat = strategy

  private val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes())

  private val headers = Map("Accept" -> "application/json", "Authorization" -> ("Basic " + auth))

  def projects = {
    for {
      jvalue <- Http(durl(url) / "users" / owner / "repos" OK as.lift.Json)
      JArray(jprojects) <- jvalue
      jproject <- jprojects
    } yield Http.promise(jproject).either map (_.right flatMap toProject)
  } map (Vector() ++ _)

  def issue(project: String, number: String) =
    Http(durl(url) / "repos" / owner / project / "issues" / number OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def issues(project: String) = {
    for {
      jvalue <- Http(durl(url) / "repos" / owner / project / "issues" <<? Map("per_page" -> "100") OK as.lift.Json)
      JArray(jissues) <- jvalue
      jissue <- jissues
    } yield Http.promise(jissue).either map (_.right flatMap toIssue)
  } map (Vector() ++ _)

  def createIssue(project: String, is: Issue) =
    Http(durl(url) / "repos" / owner / project / "issues" << write(is) <:< headers OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def closeIssue(project: String, is: Issue) =
      Http((durl(url) / "repos" / owner / project / "issues" / is.number.toString)
        .PATCH
        .setBody(write(is.copy(state = "closed"))) <:< headers OK as.lift.Json)
        .either map (_.right flatMap toIssue)

}

