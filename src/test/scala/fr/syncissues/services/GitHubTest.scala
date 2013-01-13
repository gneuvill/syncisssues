package fr.syncissues.services

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import dispatch._
import fr.syncissues.beans.Issue
import java.util.concurrent.TimeUnit

class GitHubSpec extends Specification {

  val owner = "gneuvill"
  val repo = "testsync"

  "GitHub with %s/%s".format(owner, repo).title

  val github = GitHub("gneuvill", "toto", "gneuvill", "testsync")

  lazy val ghIssue =
    github.issue("23")
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !")))

  lazy val ghIssues =
    github.issues
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Vector(Left(new Exception("Time out !"))))

  lazy val createdIssue =
    github.createIssue(Issue(title = "CreateIssue1", body = "Created Issue CreateIssue1"))
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !")))

  lazy val closedIssue =
    createdIssue.right flatMap (is => github.closeIssue(is.copy(state = "closed"))
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !"))))

  "The issue method" should {

    "return an Issue" in {

      ghIssue.isRight aka "and not an error" must beTrue

      ghIssue.right forall (_.number == 23) aka "with the right id" must beTrue

      ghIssue.right forall (_.title == "Issue4") aka "with the right title" must beTrue

      ghIssue.right forall (_.body == "Text4") aka "with the right body" must beTrue
    }
  }

  "The issues method" should {

    "return a list of Issues" in {

      ghIssues map (_.isRight) forall (_ == true) aka "and not Errors" must beTrue

      ghIssues.size == 3 aka "of the right size" must beTrue
    }
  }

  "The createIssue method" should {

    "return an issue" in {

      createdIssue.isRight aka "and not an error" must beTrue

      createdIssue.right forall (_.title == "CreateIssue1") aka "with the right title" must beTrue

      createdIssue.right forall (_.body == "Created Issue CreateIssue1") aka "with the right body" must beTrue
    }
  }

  "The closeIssue method" should {

    "return an issue" in {

      closedIssue.isRight aka "and not an error" must beTrue

      for {
        cli <- closedIssue.right
        cri <- createdIssue.right
      } yield cli.number == cri.number aka "with the right id" must beTrue

      closedIssue.right forall (_.title == "CreateIssue1") aka "with the right title" must beTrue

      closedIssue.right forall (_.body == "Created Issue CreateIssue1") aka "with the right body" must beTrue

      closedIssue.right forall (_.state == "closed") aka "with the right state" must beTrue
    }
  }

}
