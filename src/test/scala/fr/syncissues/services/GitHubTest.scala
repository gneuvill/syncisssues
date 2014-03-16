package fr.syncissues.services

import java.util.concurrent.TimeUnit
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import dispatch._
import fr.syncissues.model._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scalaz._
import Scalaz._

class GitHubSpec extends Specification {
  sequential
  // stopOnFail

  val owner = "gneuvill"
  val repo = "testsync-test"
  val github = GitHub(owner, "guessWhat", owner)
  val title = "CreateIssue1"
  val body = "Descr CreateIssue1"

  var project = Project(999, repo)

  "GitHub with %s/%s".format(owner, repo).title

  lazy val createdIssue =
      github.createIssue(Issue(title = title, body = body, project = project)).attemptRun
        //.attemptRunFor(Duration(4, TimeUnit.SECONDS))

  lazy val ghIssue =
    createdIssue flatMap { is =>
        github.issue(is.number.toString, Some(project)).attemptRun
          //.attemptRunFor(Duration(4, TimeUnit.SECONDS))
    }

  lazy val ghIssues =
    createdIssue flatMap { is =>
      github.issues(project).attemptRun //.attemptRunFor(Duration(4, TimeUnit.SECONDS))
    } fold (e => Seq(e.left), s => s map (_.right))

  lazy val closedIssue =
    createdIssue flatMap { is =>
      github.closeIssue(is.copy(state = "closed", project = project)).attemptRun
        //.attemptRunFor(Duration(4, TimeUnit.SECONDS))
    }

  step {
    github.createProject(project).attemptRun match { //.attemptRunFor(Duration(4, TimeUnit.SECONDS)) match {
      case \/-(pr) => project = pr; true
      case -\/(t) => t.printStackTrace; false
    }

  // step {
  //   github.createProject(project).attemptRun match { //.attemptRunFor(Duration(4, TimeUnit.SECONDS)) match {
  //     case \/-(pr) => project = pr; true
  //     case -\/(t) => t.printStackTrace; false
  //   }
  // }

  "The createIssue method" should {

    "return an issue" in {

      (createdIssue match {
        case \/-(is) ⇒ println(is); true
        case -\/(t) ⇒ t.printStackTrace; false 
      }) aka "and not an error" must beTrue

      createdIssue forall (_.title == title) aka "with the right title" must beTrue

      createdIssue forall (_.body == body) aka "with the right body" must beTrue
    }
  }

  "The issue method" should {

    "return an Issue" in {

      ghIssue.isRight aka "and not an error" must beTrue

      ghIssue forall (_.number == 1) aka "with the right id" must beTrue

      ghIssue forall (_.title == title) aka "with the right title" must beTrue

      ghIssue forall (_.body == body) aka "with the right body" must beTrue
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
          cli <- closedIssue
          cri <- createdIssue
        } yield cli.number == cri.number 

      rightId forall (_ == true) aka "with the right id" must beTrue

      closedIssue forall (_.title == title) aka "with the right title" must beTrue

      closedIssue forall (_.body == body) aka "with the right body" must beTrue

      closedIssue forall (_.state == "closed") aka "with the right state" must beTrue
    }
  }

  step {
      github.deleteProject(project).attemptRun //.attemptRunFor(Duration(4, TimeUnit.SECONDS))
      .fold (e => {e.printStackTrace; false}, b => b)
  }
}
