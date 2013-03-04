package fr.syncissues
package snippet

import fr.syncissues._
import services._
import ProjectService._
import model._
import utils._
import FJ._
import comet._

import scala.collection.JavaConverters._
import scala.xml._
import NodeSeq._

import net.liftweb._
import http.{jquery => _, _}
import SHtml._
import common._
import util.{ Cell => _, _ }
import Helpers._
import js.JsCmds._
import actor.SpecializedLiftActor

import reactive._
import web._
import html._
import javascript._
import JsTypes._

import scalaz.syntax.std.indexedSeq._

import java.util.concurrent.Executors
import fj.control.parallel._
import Actor.{ actor => fjActor }
import Promise.{ sequence => fjPSequence }

class SyncIssues extends Observing {

  type IPServ = IssueService with ProjectService

  type IActor = SpecializedLiftActor[Seq[(IPServ, Promise[Seq[Either[Throwable, Issue]]])]]

  type PActor = SpecializedLiftActor[Seq[Promise[Seq[Either[Throwable, Project]]]]]

  implicit val strat =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(32))

  implicit lazy val curPage = Page.currentPage

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  val services = github :: icescrum :: mantis :: Nil

  val selectedServices = BufferSignal[IPServ]()
  selectedServices.change ->> {
    projectActor ! (selectedServices.value map (_.projects))
  }

  val servIssues: Map[IPServ, BufferSignal[Issue]] = Map(
    github -> BufferSignal[Issue](),
    icescrum -> BufferSignal[Issue](),
    mantis -> BufferSignal[Issue]())

  val servSelectedIssues: Map[IPServ, BufferSignal[Issue]] = Map(
    github -> BufferSignal[Issue](),
    icescrum -> BufferSignal[Issue](),
    mantis -> BufferSignal[Issue]())

  val dummyProject = Project(-999, " --- Select --- ")

  val projects = BufferSignal[Project](dummyProject)

  val srvsOpts = SeqSignal(selectedServices map {
    ds => (ds.toIndexedSeq map (Option(_: IPServ)) intersperse None).zipWithIndex
  })

  val projectActor = new PActor {
    def messageHandler = {
      case s => projects() = dummyProject :: (commonProjects {
        s map (_.claim map {
          case Right(prj) => prj
          case Left(t) => t.printStackTrace; dummyProject
        })
      } filterNot (_ == dummyProject)).toList
    }
  }

  val issueActor = new IActor {
    def messageHandler = {
      case seq => seq foreach {
        _ match {
          case (srv, prom) => servIssues(srv)() =
            prom.claim flatMap (_.right.toSeq)
        }
      }
    }
  }

  val serviceRepeat = Repeater {
    SeqSignal(Val(services map { srv =>
      val click = DomEventSource.click
      val className =
        PropertyVar("className", "class")("name")
      click ->> {
        if (selectedServices.value contains srv) {
          servIssues(srv)() = Seq()
          selectedServices.value -= srv
          className() = (className.now split " ")
            .filterNot(_ == "selected") mkString " "
        } else {
          selectedServices.value += srv
          className() = "selected" + " " + className.now
        }
      }
      ".name" #> click &
        ".name" #> className &
        ".name *" #> srv.getClass.getSimpleName
    }))
  }

  val projectSelect = Select(Some(dummyProject), projects, (prj: Project) => prj.name) {
    case Some(prj)  if (prj.id == -999) =>
      for (srv <- selectedServices.now) servIssues(srv)() = Seq()
    case Some(prj) => issueActor ! {
      for (srv <- selectedServices.value) yield (srv, srv.issues(prj))
    }
    case None => alert("got None")
  }

  def issuesRepeat(sv: IPServ) =
    Repeater {
      servIssues(sv).now map { is =>
        ".issue" #> {
          val click = DomEventSource.click
          val buf = servSelectedIssues(sv)
          val className =
            PropertyVar("className", "class")("name")
          click ->> {
            if (buf.value contains is) {
              buf.value -= is
              className() = (className.now split " ")
                .filterNot(_ == "selected") mkString " "
            } else {
              buf.value += is
              className() = "selected" + " " + className.now
            }
          }
          ".name" #> click &
          ".name" #> className &
          ".name *" #> is.title
        }
      } signal
    }

  val srvIssuesRepeat = Repeater {
    srvsOpts.now map { tuple =>
      ".service-or-buttons" #> {
        tuple match {
          case (Some(sv), idx) =>
            PropertyVar("className", "class")("service") andThen
            ".name *" #> sv.getClass.getSimpleName &
            ".issues" #>  issuesRepeat(sv) & ".sync-buttons" #> Noop
          case (None, idx) =>
            PropertyVar("className", "class")("buttons") andThen
            ".sync-buttons *" #> {
              ".syncright" #> Button("->") {
                syncIssues(srvsOpts.now(idx + 1)._1.get, srvsOpts.now(idx - 1)._1.get)
              } &
              ".syncleft" #> Button("<-") {
                syncIssues(srvsOpts.now(idx - 1)._1.get, srvsOpts.now(idx + 1)._1.get)
              }
            } &
            ".name" #> Noop &
            ".issues" #> Noop
        }
      }
    } signal
  }

  def syncIssues(srv1: IPServ, srv2: IPServ) = {
    val newIssues = servSelectedIssues(srv2).now
    newIssues foreach {
      srv1.createIssue_? _ andThen (_ fmap {
        (ei: Either[Throwable, Issue]) =>
          ei fold (
            t => NotifServer ! ErrorM("", t.getMessage),
            is => {
              servIssues(srv1).value += is
              NotifServer ! SuccessM("",
                "%s has been created in %s".format(is.title, srv1.getClass.getSimpleName))
            })
      })
    }
  }

  // ###### Styling stuff

  sealed trait Document extends JsStub
  sealed trait JQuery extends JsStub {
    var fn: JQuery
    def init(selector: $[JsString], context: JsStub): JQuery
    def attr(name: $[JsString], value: $[JsString]): $[JsString]
  }

  val doc = jsProxy[Document]('document)
  val jq = jsProxy[JQuery]('jQuery)

  val nbrSrvs = Var(0) <<: (selectedServices map { seq =>
    if (seq.size == 0) 1 else seq.size
  })
  nbrSrvs.change foreach { i => Javascript {
    jq.fn.init(""".service-or-buttons[class~="service"]""", doc)
      .attr("style", "width: " + (if (i <= 1) 100.0 else 85.0 / i) + "%;")
  }}

  val nbrBts = Var(0) <<: nbrSrvs map (_ - 1)
  nbrBts.change foreach { i => Javascript {
    jq.fn.init(""".service-or-buttons[class~="buttons"]""", doc)
      .attr("style", "width: " + 15.0 / i + "%;")
  }}

  val widthSrvs = Var(0.0) <<: {
    for {
      nbSr <- nbrSrvs
      nbBt <- nbrBts
    } yield 100 - {
      (8.5 / nbSr) + (if (nbBt == 0) 0 else 1.5 / nbBt)
    }
  }
  widthSrvs.change foreach { i => Javascript {
    jq.fn.init(".services-issues", doc).attr("style", "width: " + i + "%;")
  }}

  // ###### Bindings

  def render =
    "#services" #> serviceRepeat &
      "#project" #> projectSelect &
      ".services-issues" #> srvIssuesRepeat
}


