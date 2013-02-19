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

import net.liftweb._
import http._
import common._
import util.{ Cell => _, _ }
import Helpers._
import js._
import JE._
import JsCmds._
import actor.SpecializedLiftActor

import reactive._
import web._
import html._

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
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))

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
      case seq => seq foreach { tuple =>
        tuple match {
          case (srv, prom) => servIssues(srv)() =
            prom.claim flatMap (_.right.toSeq map (is => is))
        }
      }
    }
  }

  val serviceRepeat = Repeater {
    SeqSignal(Val(services map { srv =>
      val click = DomEventSource.click
      val className =
        PropertyVar("className", "class")("srvname")
      click ->> {
        if (selectedServices.value contains srv) {
          projects() = Seq(dummyProject)
          selectedServices.value -= srv
          className() = (className.now split " ")
            .filterNot(_ == "selected") mkString " "
        } else {
          projects() = Seq(dummyProject)
          selectedServices.value += srv
          className() = "selected" + " " + className.now
        }
      }
      ".srvname" #> click &
        ".srvname" #> className &
        ".srvname *" #> srv.getClass.getSimpleName
    }))
  }

  val projectSelect = Select(Some(dummyProject), projects, (prj: Project) => prj.name) {
    case Some(prj)  if (prj.id == -999) => for (srv <- selectedServices.value) servIssues(srv)() = Seq()
    case Some(prj) => issueActor ! {
      for (srv <- selectedServices.value) yield (srv, srv.issues(prj))
    }
    case None => alert("got None")
  }

  val issueRepeat = Repeater {
    srvsOpts.now map { tuple =>
      ".service" #> {
        tuple match {
          case (Some(sv), idx) =>
            ".name *" #> sv.getClass.getSimpleName &
            ".issues" #> Repeater {
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
            } & ".buttons" #> Noop
          case (None, idx) =>
            ".buttons *" #> {
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
              NotifServer ! SuccessM("", "%s has been created in %s".format(is.title, srv1.getClass.getSimpleName))
            })
      })
    }
  }

  def render =
    ".selservice" #> serviceRepeat &
      ".selproject" #> projectSelect &
      ".services" #> issueRepeat

}
