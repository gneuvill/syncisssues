package fr.syncissues
package snippet

import fr.syncissues.services.SyncIsInjector
import fr.syncissues.services.IssueService
import fr.syncissues.beans.Issue
import fr.syncissues.beans.Message
import fr.syncissues.utils.FJ._
import fr.syncissues.comet.NotifServer

import net.liftweb._
import http._
import common._
import util.Helpers._
import js._
import net.liftweb.actor.LiftActor

import JsCmds._
import scalaz._
import Scalaz._

import fj.control.parallel.Actor
import fj.control.parallel.Strategy
import java.util.concurrent.Executors
import fj.Effect
import fj.control.parallel.Promise

object CreateIssue {

  val strat = Strategy.executorStrategy[fj.Unit](
    Executors.newFixedThreadPool(4))

  implicit def liftActorToFJActor(la: LiftActor) =
    Actor.actor(strat, new Effect[Message] { def e(m: Message) = la ! m })

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  private object title extends RequestVar("")

  private object descr extends RequestVar("")

  private object services extends RequestVar(List(): List[IssueService])

  val selServices = SHtml.multiSelectObj(
    Seq(github -> "GitHub", icescrum -> "Icescrum", mantis -> "Mantis"),
    Seq(),
    services.set)

  def validTitle(title: String) =
    if (title == "") "Title cannot be empty".failNel else title.success

  def validDescr(descr: String) =
    if (descr == "") "Description shouln't be empty".failNel else descr.success

  def validServices(ls: List[IssueService]) =
    if (services.size == 0) "Select at least one service".failNel else ls.success

  def createIssue(title: String, descr: String, servs: List[IssueService]) =
    for {
      s <- servs
    } yield s.createIssue(Issue(title = title, body = descr))

  def showResult(res: Promise[Either[Throwable, Issue]]) = res fmap {
    (ei: Either[Throwable, Issue]) =>
    ei fold (t => Message(t.getMessage), i => Message(i.title))
  } to NotifServer

  def process() = {
    val result =
      (validTitle(title) |@|
      validDescr(descr) |@|
      validServices(services)) (createIssue)

    result.fold(_.list.foreach(S.error), _ foreach showResult)

    Noop
  }

  def render =
    "#title" #> SHtml.textElem(title) &
    "#descr" #> SHtml.textareaElem(descr) &
    "#where" #> (selServices ++ SHtml.hidden(process))

}
