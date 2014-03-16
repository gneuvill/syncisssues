package fr.syncissues.services

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import dispatch._
import fr.syncissues.model._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scalaz._
import Scalaz._

class IceScrumSpec extends Specification {
  sequential

  val team = "TSI"

  val project = Project(99999, "testsync")

  "IceScrum with %s".format(team).title

  val icescrum = IceScrum("gneuvill", "toto", team)

  lazy val createdStory =
    icescrum.createIssue(Issue(title = "CreateStory1", body = "Descr CreateStory1", project = project)).attemptRun

  lazy val iscIssue =
    icescrum.issue("1").attemptRun

//   lazy val iscIssues =
//     icescrum.issues(project)
//       .claim(2L, TimeUnit.SECONDS)
//       .orSome(Vector(Left(new Exception("Time out !"))))

//   lazy val closedStory =
//     createdStory.right flatMap (is => icescrum.closeIssue(is.copy(state = "closed"))
//       .claim(4L, TimeUnit.SECONDS)
//       .orSome(Left(new Exception("Time out !"))))

//   // Helpers needed to test the closeIssue method
//   lazy val cisId = createdStory.fold(e => "", _.number.toString)
//   implicit val formats = DefaultFormats
//   lazy val jvalues = Seq(
//     "accept" -> parse(""" {"type": "story"} """),
//     "estimate" -> parse(""" {"story": {"effort": 5}} """),
//     "plan" -> parse(""" {"sprint": {"id": 3}} """)
//   ) map {
//     tuple => Http(url(icescrum.url) / project.name / "story" / cisId / tuple._1 <:< icescrum.headers << Serialization.write(tuple._2) OK as.lift.Json)()
//   }

  "The createIssue method" should {
    "return an issue" in {

      (createdStory match {
        case \/-(is) ⇒ true
        case -\/(t) ⇒ t.printStackTrace; false 
      }) aka "and not an error" must beTrue

      createdStory forall (_.title == "CreateStory1") aka "with the right title" must beTrue

      createdStory forall (_.body == "Descr CreateStory1") aka "with the right body" must beTrue

      // jvalues forall (_.isInstanceOf[JValue]) aka "closeIssue prerequisites done" must beTrue
    }
  }

  "The issue method" should {

    "return an Issue" in {

      iscIssue.isRight aka "and not an error" must beTrue

      iscIssue forall (_.number == 1) aka "with the right id" must beTrue

      iscIssue forall (_.title == "Icescrum bug 1") aka "with the right title" must beTrue

      iscIssue forall (_.body == "gros bug") aka "with the right body" must beTrue
    }
  }

  // "The issues method" should {

  //   "return a list of Issues" in {

  //     iscIssues map (_.isRight) forall (_ == true) aka "and not Errors" must beTrue

  //     iscIssues.size == 3 aka "of the right size" must beTrue
  //   }
  // }

  // "The closeIssue method" should {

  //   "return an issue" in {

  //     jvalues forall (_.isInstanceOf[JValue]) aka "closeIssue prerequisites done" must beTrue

  //     closedStory.isRight aka "and not an error" must beTrue

  //     for {
  //       cli <- closedStory.right
  //       cri <- createdStory.right
  //     } yield cli.number == cri.number aka "with the right id" must beTrue

  //     closedStory.right forall (_.title == "CreateStory1") aka "with the right title" must beTrue

  //     closedStory.right forall (_.body == "Created Story CreateStory1") aka "with the right body" must beTrue

  //     closedStory.right forall (_.state == "closed") aka "with the right state" must beTrue
  //   }
  // }
}
