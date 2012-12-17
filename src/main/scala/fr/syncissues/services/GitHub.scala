package fr.syncissues.services

import fr.syncissues._
import beans.Issue
import utils.Conversions._
import dispatch._
import net.liftweb.json._
import net.liftweb.json.MappingException

object GitHub {

  implicit val formats = DefaultFormats

  private val github = "https://api.github.com/repos"

  private val issuesReq = (owner: String, repo: String) =>
  (url(github) / owner / repo / "issues").addQueryParameter("per_page", "100")

  /**
    * Returns an issue from the specified GitHub repository owned by the specified owner
    * 
    */
  def issue(owner: String, repo: String, number: String): Promise[Either[MappingException, Issue]] =
    Http(issuesReq(owner, repo) / number OK as.lift.Json) map toIssue

  /**
    * Returns the opened issues from the specified GitHub repository owned by the specified owner
    * 
    */
  def realIssues(owner: String, repo: String): Promise[List[Either[MappingException, Issue]]] =
    (for {
      jvalue <- Http(issuesReq(owner, repo) OK as.lift.Json)
      JArray(jissue) <- jvalue
    } yield (Http.promise(jissue))).map(_.foldLeft(Nil: List[JValue])(_ ++: _) map toIssue)

  /**
    * TODO : find why we have to filter the 'Right' values
    * 
    */
  def issues(owner: String, repo: String): Promise[List[Either[MappingException, Issue]]] =
    realIssues(owner, repo) map (_ filter (_.isRight))

}




















