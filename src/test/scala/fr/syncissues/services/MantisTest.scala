package fr.syncissues.services

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import fr.syncissues.model._
import java.util.concurrent.TimeUnit

class MantisSpec extends Specification {

  val username = "administrator"
  val password = "root"
  val project = Project(99999, "testsync")

  "Mantis with %s/%s".format(username, password).title

  val mantis = Mantis(username, password)

  lazy val mantisIssue =
    mantis.issue("1")
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !")))

  lazy val mantisIssues =
    mantis.issues(project)
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Vector(Left(new Exception("Time out !"))))

  lazy val createdIssue =
    mantis.createIssue(Issue(title = "CreatedBug1", body = "Descr CreatedBug1", project = project))
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !")))

  lazy val closedIssue =
    createdIssue.right flatMap (is => mantis.closeIssue(is.copy(state = "closed"))
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !"))))

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
















