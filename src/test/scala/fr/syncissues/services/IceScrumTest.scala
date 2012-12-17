package fr.syncissues.services

import org.specs2.mutable.Specification
import fr.syncissues.services.IceScrum._
import dispatch._
import fr.syncissues.beans.Issue

class IceScrumSpec extends Specification {

  val project = "TSI"

  "IceScrum with %s".format(project).title

  "The story method" should {
    val iscIssue = story(project, "1")()

    "return an Issue" in {
      iscIssue isRight
    }

    "and the right one" in {

      "with the right id" in {
        iscIssue.right forall (_.number == 1)
      }

      "and the right title" in {
        iscIssue.right forall (_.title == "Story1")
      }

      "and the right body" in {
        iscIssue.right forall (_.body == "Problème1")
      }
    }
  }

  "The stories method" should {
    val iscIssues = stories(project)()

    "return Issue objects" in {
      iscIssues map (_.isRight) forall (_ == true)
    }

    "and the right number of them" in {
      iscIssues.size == 3
    }
  }
  
}







