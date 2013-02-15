package fr.syncissues
package snippet

import fr.syncissues._
import services._
import ProjectService._
import model._
import utils._
import FJ._
import comet._

import net.liftweb._
import http._
import common._
import util._
import Helpers._
import js._
import JE._
import JsCmds._
import actor.LiftActor

import reactive._
import web._
import html._

//import scalaz._
//import Scalaz._

import java.util.concurrent.Executors
import fj._
import data.List.{list => fjList}
import control.parallel._
import Actor.{actor => fjActor}
import Promise.{sequence => fjPSequence}
// import fj.Effect

// import scala.collection.SeqLike._
// import scala.annotation.tailrec

class SyncIssues extends Observing {

  type IPServ = IssueService with ProjectService

  implicit val strat =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))
  
  implicit lazy val curPage = Page.currentPage

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  val allServices = github :: icescrum :: mantis :: Nil

  val services= BufferSignal[IPServ](allServices: _*)

  val selectedServices = BufferSignal[IPServ]()
  selectedServices ->> {
    fjPSequence(strat, selectedServices.now map { srv: IPServ =>
      srv.projects fmap {
        s: Seq[Either[Throwable, Project]] =>
        for (ei <- s; prj <- ei.right.toSeq) yield prj
      }
    }) to projectActor
  }


  val servIssues: Map[IPServ, BufferSignal[Issue]] = Map(
    github -> BufferSignal[Issue](),
    icescrum -> BufferSignal[Issue](),
    mantis -> BufferSignal[Issue]())

  val servSelectedIssues: Map[IPServ, BufferSignal[Issue]] = Map(
    github -> BufferSignal[Issue](),
    icescrum -> BufferSignal[Issue](),
    mantis -> BufferSignal[Issue]())

  val serviceRepeat = Repeater {
    services.now map { srv =>
      val click = DomEventSource.click
      val className =
        PropertyVar("className", "class")("srvname")
      click ->> {
        if (selectedServices.value contains srv) {
          selectedServices.value -= srv
          className() = (className.now split " ")
            .filterNot(_ == "selected") mkString " "
        }
        else {
          selectedServices.value += srv
          className() = "selected" + " " + className.now
        }
      }
      ".srvname" #> click &
      ".srvname" #> className &
      ".srvname *" #> srv.getClass.getSimpleName
    } signal
  }

  val projects = BufferSignal[Project]()

  val projectActor = fjActor(strat, projects update commonProjects(_: Seq[Seq[Project]]))

  val projectSelect = Select(projects.now.headOption, projects, (prj: Project) => prj.name) {
    case Some(prj) => fjPSequence(strat, selectedServices.now map { srv: IPServ =>
      srv.issues(prj) fmap {
        s: Seq[Either[Throwable, Issue]] =>
        (srv, for (ei <- s; is <- ei.right.toSeq) yield is)
      }
    }) to issueActor
    case _ =>
  }

  val issueActor = fjActor(strat, (s: Seq[(IPServ, Seq[Issue])]) => s foreach { t => t match {
    case (srv, iss) => servSelectedIssues(srv) update iss
    case _ =>
  }})

  def initIssues() = allServices foreach {
    srv => srv.issues(Project(name = "testsync")) fmap {
      (sei: Seq[Either[Throwable, Issue]]) => {
        val isseq = for {
          ei <- sei
          is <- ei.right.toSeq
        } yield is
        servIssues(srv) update isseq
      }
    }
  }

  val issueRepeat = Repeater {
    selectedServices.now map { srv =>
      val srvs = servIssues.keys.toIndexedSeq
      val idx = srvs.indexOf(srv)
      Repeater {
  servIssues(srv).now map { is =>
	  val click = DomEventSource.click
	  val buf = servSelectedIssues(srv)
	  val className =
	    PropertyVar("className", "class")("issue")
	  for (c <- click.eventStream) {
	    if (buf.value contains is) {
	      buf.value -= is
	      className() = (className.now split " ")
		.filterNot(_ == "selected") mkString " "
	    }
	    else {
	      buf.value += is
	      className() = "selected" + " " + className.now
	    }
	  }
	  ".issue" #> click &
	  ".issue" #> className &
	  ".issue *" #> is.title &
	  ".buttons *" #> {
	    ".syncright" #> Button("->") {
	      syncIssues(srvs(idx + 1), srvs(idx - 1))
	    } &
	    ".syncleft" #> Button("<-") {
	      syncIssues(srvs(idx - 1), srvs(idx + 1))
	    }
	  }
	} signal
      }
      
    } signal
  }

  def syncIssues(srv1: IPServ, srv2: IPServ) = {
    val oldIssues = servIssues(srv1).now
    val newIssues = servSelectedIssues(srv2).now
    newIssues foreach {
      srv1.createIssue_? _ andThen (_ fmap {
        (ei: Either[Throwable, Issue]) =>
        ei fold (
          t => NotifServer ! ErrorM("", t.getMessage),
          is => {
            servIssues(srv1).value ++= Seq(is)
            NotifServer ! SuccessM("", "%s has been created in %s".format(is.title, srv1.getClass.getSimpleName))
          })
      })
    }
  }

  // val srvsList = allServices  map (Option(_)) intersperse (None) // selectedServices.now.toList map (Option(_)) intersperse (None)
  

  // val thing = Repeater {
  //   selectedServices.now map { srv =>

  //   } signal
  // }


  def render = {
    ".selservice" #> serviceRepeat &
    ".selproject" #> projectSelect &
    ".service" #> issueRepeat
  }

}
