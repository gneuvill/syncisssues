package fr.syncissues.services

import org.specs2.mutable.Specification
import fr.syncissues.services.Mantis._
import fr.syncissues.beans.Issue

class MantisSpec extends Specification {

  val username = "administrator"
  val password = "root"

  "Mantis with %s/%s".format(username, password).title

  "The issue method" should {
    val mantisIssue = issue(username, password, 1).claim()

    "return an issue" in {
      mantisIssue isRight
    }

    "and the right one" in {

      "with the right id" in {
        mantisIssue.right forall (_.number == 1)
      }

      "with the right title" in {
        mantisIssue.right forall (_.title == "Bug1")
      }

      "and the right body" in {
        mantisIssue.right forall (_.body == "TextBug1")
      }
    }
  }

  "The issues method" should {

    "return Issue objects" in {
      false
    }

    "and the right number of them" in {
      false
    }
  }
  
}










