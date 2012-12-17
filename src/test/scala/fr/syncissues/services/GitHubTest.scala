package fr.syncissues.services

import org.specs2.mutable.Specification
import fr.syncissues.services.GitHub._
import dispatch._
import fr.syncissues.beans.Issue

class GitHubSpec extends Specification {

  val owner = "gneuvill"
  val repo = "testsync"

  "GitHub with %s/%s".format(owner, repo).title

  "The issue method" should {
    val ghIssue = issue(owner, repo, "1")()

    "return an Issue" in {
      ghIssue isRight
    }

    "and the right one" in {

      "with the right id" in {
        ghIssue.right forall (_.number == 1)
      }

      "with the right title" in {
        ghIssue.right forall (_.title == "Issue1")
      }

      "and the right body" in {
        ghIssue.right forall (_.body == "Text1")
      }
    }
  }

  "The issues method" should {
    val ghIssues = issues(owner, repo)()

    "return Issue objects" in {
      ghIssues map (_.isRight) forall (_ == true)
    }

    "and the right number of them" in {
      ghIssues.size == 2
    }
  }


}















