package fr.syncissues.services

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import net.liftweb.json._
import dispatch._
import fr.syncissues.beans.Issue
import java.util.concurrent.TimeUnit
import net.liftweb.json.MappingException


class IceScrumSpec extends Specification {

  val team = "TSI"

  val project = "testsync"

  "IceScrum with %s".format(team).title

  val icescrum = IceScrum("gneuvill", "toto", team)

  lazy val iscIssue =
    icescrum.issue(project, "1")
      .claim(2L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !")))

  lazy val iscIssues =
    icescrum.issues(project)
      .claim(2L, TimeUnit.SECONDS)
      .orSome(Vector(Left(new Exception("Time out !"))))

  lazy val createdStory =
    icescrum.createIssue(project, Issue(title = "CreateStory1", body = "Created Story CreateStory1"))
      .claim(2L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !")))

  lazy val closedStory =
    createdStory.right flatMap (is => icescrum.closeIssue(project, is.copy(state = "closed"))
      .claim(4L, TimeUnit.SECONDS)
      .orSome(Left(new Exception("Time out !"))))

  // Helpers needed to test the closeIssue method
  lazy val cisId = createdStory.fold(e => "", _.number.toString)
  implicit val formats = icescrum.formats
  lazy val jvalues = Seq(
    "accept" -> parse(""" {"type": "story"} """),
    "estimate" -> parse(""" {"story": {"effort": 5}} """),
    "plan" -> parse(""" {"sprint": {"id": 3}} """)
  ) map {
    tuple => Http(url(icescrum.url) / project / "story" / cisId / tuple._1 <:< icescrum.headers << Serialization.write(tuple._2) OK as.lift.Json)()
  }


  "The issue method" should {

    "return an Issue" in {

      iscIssue.isRight aka "and not an error" must beTrue

      iscIssue.right forall (_.number == 1) aka "with the right id" must beTrue

      iscIssue.right forall (_.title == "Story1") aka "with the right title" must beTrue

      iscIssue.right forall (_.body == "Problème1") aka "with the right body" must beTrue
    }
  }

  "The issues method" should {

    "return a list of Issues" in {

      iscIssues map (_.isRight) forall (_ == true) aka "and not Errors" must beTrue

      iscIssues.size == 3 aka "of the right size" must beTrue
    }
  }

  "The createIssue method" should {

    "return an issue" in {

      createdStory.isRight aka "and not an error" must beTrue

      createdStory.right forall (_.title == "CreateStory1") aka "with the right title" must beTrue

      createdStory.right forall (_.body == "Created Story CreateStory1") aka "with the right body" must beTrue

      jvalues forall (_.isInstanceOf[JValue]) aka "closeIssue prerequisites done" must beTrue
    }
  }

  "The closeIssue method" should {

    "return an issue" in {

      jvalues forall (_.isInstanceOf[JValue]) aka "closeIssue prerequisites done" must beTrue

      closedStory.isRight aka "and not an error" must beTrue

      for {
        cli <- closedStory.right
        cri <- createdStory.right
      } yield cli.number == cri.number aka "with the right id" must beTrue

      closedStory.right forall (_.title == "CreateStory1") aka "with the right title" must beTrue

      closedStory.right forall (_.body == "Created Story CreateStory1") aka "with the right body" must beTrue

      closedStory.right forall (_.state == "closed") aka "with the right state" must beTrue
    }
  }
}
