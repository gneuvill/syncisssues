package fr.syncissues.services

import java.util.concurrent.TimeUnit
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import dispatch._
import fr.syncissues.model._
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GitHubSpec extends Specification {
  sequential
  stopOnFail

  val owner = "gneuvill"
  val repo = "testsync"
  val github = GitHub(owner, "toto", owner)
  val title = "CreateIssue1"
  val body = "Descr CreateIssue1"

  var project = Project(999, "ghtestsync")

  "GitHub with %s/%s".format(owner, repo).title

  lazy val createdIssue = Await.result(
      github.createIssue(Issue(title = title, body = body, project = project)),
      Duration(4, TimeUnit.SECONDS))

  lazy val ghIssue =
    createdIssue.right flatMap {
      is => Await.result(
        github.issue(is.number.toString, Some(is.project)),
        Duration(4, TimeUnit.SECONDS))
    }

  lazy val ghIssues =
    createdIssue.right map {
      is => Await.result(github.issues(is.project), Duration(4, TimeUnit.SECONDS))
    } fold (e => Seq(Left(e)), s => s)

  lazy val closedIssue =
    createdIssue.right flatMap (is => Await.result(
      github.closeIssue(is.copy(state = "closed")),
      Duration(4, TimeUnit.SECONDS)))

  step {
    Await.result(github.createProject(project), Duration(4, TimeUnit.SECONDS)) match {
      case Right(pr) => project = pr; true
      case Left(t) => t.printStackTrace; false
    }
  }

  "The createIssue method" should {

    "return an issue" in {

      createdIssue.isRight aka "and not an error" must beTrue

      createdIssue.right forall (_.title == title) aka "with the right title" must beTrue

      createdIssue.right forall (_.body == body) aka "with the right body" must beTrue
    }
  }

  "The issue method" should {

    "return an Issue" in {

      ghIssue.isRight aka "and not an error" must beTrue

      ghIssue.right forall (_.number == 1) aka "with the right id" must beTrue

      ghIssue.right forall (_.title == title) aka "with the right title" must beTrue

      ghIssue.right forall (_.body == body) aka "with the right body" must beTrue
    }
  }

  "The issues method" should {

    "return a list of Issues" in {

      ghIssues map (_.isRight) forall (_ == true) aka "and not Errors" must beTrue

      ghIssues.size == 1 aka "of the right size" must beTrue
    }
  }

  "The closeIssue method" should {

    "return an issue" in {

      closedIssue.isRight aka "and not an error" must beTrue

      val rightId =
        for {
          cli <- closedIssue.right
          cri <- createdIssue.right
        } yield cli.number == cri.number 

      rightId.right forall (_ == true) aka "with the right id" must beTrue

      closedIssue.right forall (_.title == title) aka "with the right title" must beTrue

      closedIssue.right forall (_.body == body) aka "with the right body" must beTrue

      closedIssue.right forall (_.state == "closed") aka "with the right state" must beTrue
    }
  }

  step {
    Await
      .result(github.deleteProject(project), Duration(4, TimeUnit.SECONDS))
      .fold (e => {e.printStackTrace; false}, b => b)
  }  

}
