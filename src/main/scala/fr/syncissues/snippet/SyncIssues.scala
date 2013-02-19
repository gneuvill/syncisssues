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

  implicit val strat =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))

  implicit lazy val curPage = Page.currentPage

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  val services = github :: icescrum :: mantis :: Nil

  val selectedServices = BufferSignal[IPServ]()

  val servIssues: Map[IPServ, BufferSignal[Issue]] = Map(
    github -> BufferSignal[Issue](),
    icescrum -> BufferSignal[Issue](),
    mantis -> BufferSignal[Issue]())

  val servSelectedIssues: Map[IPServ, BufferSignal[Issue]] = Map(
    github -> BufferSignal[Issue](),
    icescrum -> BufferSignal[Issue](),
    mantis -> BufferSignal[Issue]())

  val srvsOpts = BufferSignal[(Option[IPServ], Int)]()

  def reset() = {
    servIssues.keys foreach { srv =>
      servIssues(srv) update Seq()
      servSelectedIssues(srv) update Seq()
    }
    srvsOpts() = Seq()
  }

  /**
   * TODO : faire le claim dans le LiftActor (but: ne pas utiliser les Actor fj)
   */
  selectedServices.change ->> {
    projectActor ! (selectedServices.value map (_.projects))
    // fjPSequence(strat, selectedServices.now map { srv: IPServ =>
    //   srv.projects fmap {
    //     s: Seq[Either[Throwable, Project]] =>
    //     for (ei <- s; prj <- ei.right.toSeq) yield prj
    //   }
    // } asFJList) to projectActor
  }

  val serviceRepeat = Repeater {
    SeqSignal(Val(services map { srv =>
      val click = DomEventSource.click
      val className =
        PropertyVar("className", "class")("srvname")
      click ->> {
        if (selectedServices.value contains srv) {
          //reset()
          //projects update Seq(dummyProject)
          selectedServices.value -= srv
          className() = (className.now split " ")
            .filterNot(_ == "selected") mkString " "
        } else {
          //reset()
          selectedServices.value += srv
          className() = "selected" + " " + className.now
        }
      }
      ".srvname" #> click &
        ".srvname" #> className &
        ".srvname *" #> srv.getClass.getSimpleName
    }))
  }

  val dummyProject = Project(-999, " --- Select --- ")

  val projects = BufferSignal[Project](dummyProject)

  // val projectActor = fjActor(strat, (s: Seq[Seq[Project]]) => {
  //   projects() = dummyProject :: commonProjects(s).toList
  // })

  val projectActor = new SpecializedLiftActor[Seq[Promise[Seq[Either[Throwable, Project]]]]] {
    def messageHandler = {
      case s => projects() = dummyProject :: (commonProjects {
        s map (_.claim map {
          case Right(prj) => prj
          case Left(t) => t.printStackTrace; dummyProject
        })
      } filterNot (_ == dummyProject)).toList
    }
  }

  val projectSelect = Select(Some(dummyProject), projects, (prj: Project) => prj.name) {
    //case Some(prj) if prj.id == -999 => reset()
    case Some(prj) => // fjPSequence(strat, selectedServices.now map { srv: IPServ =>
    //   srv.issues(prj) fmap {
    //     s: Seq[Either[Throwable, Issue]] =>
    //     (srv, for (ei <- s; is <- ei.right.toSeq) yield is)
    //   }
    // } asFJList) to issueActor
    case _ =>
  }

  val issueActor = fjActor(strat, (s: Seq[(IPServ, Seq[Issue])]) =>
    s foreach { t =>
      t match {
        case (srv, iss) => {
          srvsOpts() = (selectedServices.now.toIndexedSeq map (Option(_: IPServ)) intersperse None).zipWithIndex
          servIssues(srv) update iss
        }
        case _ =>
      }
    })

  // val srvsOpts = SeqSignal(selectedServices map {
  //   ds => (ds.toIndexedSeq map (Option(_: IPServ)) intersperse None).zipWithIndex
  // })
  // srvsOpts ->> {
  //   alert(srvsOpts.now.toString)
  // }

  //alert(sv + " <-> " + idx)

  val issueRepeat = Repeater {
    srvsOpts.now map { tuple =>
      (tuple match {
        case (Some(sv), idx) =>
          ".service" #> Repeater {
            servIssues(sv).now map { is =>
              val click = DomEventSource.click
              val buf = servSelectedIssues(sv)
              val className =
                PropertyVar("className", "class")("issue")
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
              ".issue" #> click &
                ".issue" #> className &
                ".issue *" #> is.title
            } signal
          } & ".buttons" #> Noop
        case (None, idx) => ".buttons *" #> {
          ".syncright" #> Button("->") {
            syncIssues(srvsOpts.now(idx + 1)._1.get, srvsOpts.now(idx - 1)._1.get)
          } &
            ".syncleft" #> Button("<-") {
              syncIssues(srvsOpts.now(idx - 1)._1.get, srvsOpts.now(idx + 1)._1.get)
            }
        }
      })
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
      ".serviceCont" #> issueRepeat

}
