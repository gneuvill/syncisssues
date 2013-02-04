package fr.syncissues
package snippet

import fr.syncissues._
import services._
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

// import scalaz._
// import Scalaz._

import java.util.concurrent.Executors
import fj.control.parallel._
// import fj.Effect

// import scala.collection.SeqLike._
// import scala.annotation.tailrec

class SyncIssues extends Observing {

  type IPServ = IssueService with ProjectService

  implicit val strat =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))
  
  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  implicit lazy val curPage = Page.currentPage

  val actor = new LiftActor {
    def handleIssues: PartialFunction[(IPServ, Seq[Issue]), Unit] = {
      case (srv, isseq) =>
        Reactions.inServerScope(curPage) {
          servIssues(srv) update isseq // Seq(Issue(title = "Toto-" + randomString(4)))
        }
    }
    def messageHandler =
      handleIssues.asInstanceOf[PartialFunction[Any, Unit]]
  }

  val servIssues: Map[IPServ, BufferSignal[Issue]] = Map(
      github -> BufferSignal[Issue](),
      icescrum -> BufferSignal[Issue](),
      mantis -> BufferSignal[Issue]())

  val servSelectedIssues: Map[IPServ, BufferSignal[Issue]] = Map(
      github -> BufferSignal[Issue](),
      icescrum -> BufferSignal[Issue](),
      mantis -> BufferSignal[Issue]())

  def initIssues() = Seq(github, icescrum, mantis) foreach {
    srv => srv.issues(Project(name = "testsync")) fmap {
      (sei: Seq[Either[Throwable, Issue]]) => {
        val isseq = for {
          ei <- sei
          is <- ei.right.toSeq
        } yield is
        (srv, isseq)
      }
    } to actor
  }

  def issueRepeat(srv: IPServ) =
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
        ".buttons" #> Noop
      } signal
    }

  def syncIssues(srv1: IPServ, srv2: IPServ) = {
    val oldIssues = servIssues(srv1).now
    val newIssues = servSelectedIssues(srv2).now
    servIssues(srv1).value ++= newIssues diff oldIssues
  }

  val srvsList: List[Option[IPServ]] =
    Some(github) :: None :: Some(icescrum) :: None :: Some(mantis) :: Nil

  def render = {
    initIssues()
    ".service" #> (srvsList.zipWithIndex map {
      case (Some(srv), _) => issueRepeat(srv)
      case (None, idx) => ".buttons *" #> {
        ".syncright" #> Button("->") {
          syncIssues(srvsList(idx + 1).get, srvsList(idx - 1).get)
        } &
        ".syncleft" #> Button("<-") {
          syncIssues(srvsList(idx - 1).get, srvsList(idx + 1).get)
        }
      }
    })
  }
}
