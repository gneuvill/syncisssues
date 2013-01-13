package fr.syncissues.services

import fr.syncissues._
import beans.Issue
import utils.Conversions._
import dispatch.{Promise => _, url => durl, _}
import net.liftweb.json._
import Serialization.write
import utils.FJ._
import utils.Conversions._

import fj.control.parallel.Promise
import Promise._
import fj.control.parallel.Strategy
import java.util.concurrent.Executors

case class GitHub(
  user: String,
  password: String,
  owner: String,
  project: String,
  url: String = "https://api.github.com/repos",
  strategy: Strategy[fj.Unit] = Strategy.executorStrategy[fj.Unit](
    Executors.newFixedThreadPool(4))) extends IssueService {

  private implicit val formats = DefaultFormats

  private implicit val strat = strategy

  private val auth = new sun.misc.BASE64Encoder().encode((user + ":" + password).getBytes())

  private val headers = Map("Accept" -> "application/json", "Authorization" -> ("Basic " + auth))

  def issue(number: String) =
    Http(durl(url) / owner / project / "issues" / number OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def issues = {
    for {
      jvalue <- Http(durl(url) / owner / project / "issues" <<? Map("per_page" -> "100") OK as.lift.Json)
      JArray(jissues) <- jvalue
      jissue <- jissues
    } yield Http.promise(jissue).either map (_.right flatMap toIssue)
  } map (Vector() ++ _)

  def createIssue(is: Issue) =
    Http(durl(url) / owner / project / "issues" << write(is) <:< headers OK as.lift.Json)
      .either map (_.right flatMap toIssue)

  def closeIssue(is: Issue) =
      Http((durl(url) / owner / project / "issues" / is.number.toString)
        .PATCH
        .setBody(write(is.copy(state = "closed"))) <:< headers OK as.lift.Json)
        .either map (_.right flatMap toIssue)

}

