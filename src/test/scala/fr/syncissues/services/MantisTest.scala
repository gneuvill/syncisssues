package fr.syncissues.services

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import fr.syncissues.model._
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MantisSpec extends Specification {

  val username = "administrator"
  val password = "root"
  val project = Project(99999, "testsync")

  "Mantis with %s/%s".format(username, password).title

  val mantis = Mantis(username, password)

  lazy val mantisIssue =
    Await.result(mantis.issue("1"), Duration(4, TimeUnit.SECONDS))

  lazy val mantisIssues =
    Await.result(mantis.issues(project), Duration(4, TimeUnit.SECONDS))

  lazy val createdIssue = Await.result(
    mantis.createIssue(Issue(title = "CreatedBug1", body = "Descr CreatedBug1", project = project)),
    Duration(4, TimeUnit.SECONDS))

  lazy val closedIssue = Await.result(
    createdIssue.right flatMap (is => mantis.closeIssue(is.copy(state = "closed"))),
    Duration(4, TimeUnit.SECONDS))

  "The issue method" should {

    "return an Issue" in {

      mantisIssue.isRight aka "and not an error" must beTrue

      mantisIssue.right exists (_.number == 1) aka "with the right id" must beTrue

      mantisIssue.right exists (_.title == "Bug1") aka "with the right title" must beTrue

      mantisIssue.right exists (_.body == "TextBug1") aka "and the right body" must beTrue
    }
  }

  "The issues method" should {

    "return a list of Issues" in {

      mantisIssues forall (_.isRight) aka "and not Errors" must beTrue

      mantisIssues.filter(_.isRight).size == 3 aka "and the right number of them" must beTrue
    }
  }

  "The createIssue method" should {

    "return an Issue" in {

      createdIssue.isRight aka "and not an error" must beTrue

      createdIssue.right exists (_.number.toString != "") aka "with an id" must beTrue
    }
  }

  "The closeIssue method" should {

    "return an issue" in {

      closedIssue.isRight aka "and not an error" must beTrue

      for {
        cli <- closedIssue.right
        cri <- createdIssue.right
      } yield cli.number == cri.number aka "with the right id" must beTrue

      closedIssue.right forall (_.title == "CreatedBug1") aka "with the right title" must beTrue

      closedIssue.right forall (_.body == "Created Bug CreatedBug1") aka "with the right body" must beTrue

      closedIssue.right forall (_.state == "closed") aka "with the right state" must beTrue
    }
  }
  
  
}
















