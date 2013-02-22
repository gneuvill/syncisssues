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
import util.Helpers._
import util.ClearClearable
import js._
import JE._
import JsCmds._

import scalaz._
import Scalaz._

import java.util.concurrent.Executors
import fj.control.parallel._
import fj.Effect

import scala.collection.SeqLike._

object CreateIssue {

  type IPServ = IssueService with ProjectService

  implicit val strat =
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(32))

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  val availableServs =
    Seq(github -> "GitHub", icescrum -> "Icescrum", mantis -> "Mantis")

  private object allProjects extends RequestVar(Seq(): Seq[(IPServ, Project)])

  private object projectName extends RequestVar("")

  private object title extends RequestVar("")

  private object descr extends RequestVar("")

  private object services extends RequestVar(List(): Seq[IPServ])

  def validServices(ls: Seq[IPServ]) =
    if (services.size == 0) "Select at least one service".failNel else ls.success

  def validProject(projectName: String) =
    if (projectName == "") "You must choose a project".failNel else projectName.success

  def validTitle(title: String) =
    if (title == "") "Title cannot be empty".failNel else title.success

  def validDescr(descr: String) =
    if (descr == "") "Description cannot be empty".failNel else descr.success

  def getAllProjects(srvs: Seq[IPServ]) = srvs map { srv =>
    srv -> (
      for {
        ei <- srv.projects.claim
        p <- ei.right.toSeq
      } yield p)
  }

  def createIssue(servs: Seq[IPServ], projectName: String, title: String, descr: String) =
    for {
      srv <- servs
      srvPr <- allProjects.get find (t => t._1 == srv && t._2.name == projectName)
    } yield srv.createIssue_?(
      Issue(title = title, body = descr, project = Project(srvPr._2.id, srvPr._2.name)))

  def showResult(promise: Promise[Either[Throwable, Issue]]) = promise fmap {
    (ei: Either[Throwable, Issue]) =>
      ei fold (t => ErrorM("", t.getMessage), i => SuccessM("", i.title))
  } to NotifServer

  def clearForm =
    ("project" :: "title" :: "descr" :: Nil map (SetValById(_, ""))).fold(Noop) { _ & _ }

  def process() = {
    val result = {
      validServices(services) |@|
        validProject(projectName) |@|
        validTitle(title) |@|
        validDescr(descr)
    } apply createIssue

    result fold (_.list foreach (S.error), _ foreach showResult)
  }

  val updateServices = (servs: String) => {
    val chosenServs = for {
      str <- servs split ("\\|")
      (srv, name) <- availableServs
      if (str == name)
    } yield srv

    services update (_ => chosenServs)
  }

  def updateProjects(srvs: Seq[IPServ]) = {
    val srvProjects = getAllProjects(srvs)

    allProjects update { _ =>
      for {
        t <- srvProjects
        p <- t._2
      } yield t._1 -> p
    }

    val common = commonProjects(srvProjects map (_._2)).toList

    ReplaceOptions("project", common map (p => (p.name, p.name)), Empty)
  }

  val servsVal = JsRaw(
    """Array.prototype.slice.call(this.selectedOptions)
       .map(function(opt) { return opt.text })
       .reduce(function(str1, str2){ return str1 + "|" + str2 }, "")""")

  def selServices = SHtml.multiSelectObj(
    availableServs,
    Nil,
    services.set,
    "id" -> "servs",
    "onclick" -> {
      SHtml.ajaxCall(
        servsVal,
        updateServices andThen updateProjects)
    }.toJsCmd)

  def selProjects = SHtml.untrustedSelect(
    Seq(),
    Empty,
    projectName.set,
    "id" -> "project")

  def render =
    "#servs" #> selServices &
      "#project" #> selProjects &
      "#clear" #> SHtml.ajaxButton("Effacer", () => clearForm) &
      "#title" #> FocusOnLoad(SHtml.textElem(title, "id" -> "title")) &
      "#descr" #> (SHtml.textareaElem(descr, "id" -> "descr") ++ SHtml.hidden(process))
}
