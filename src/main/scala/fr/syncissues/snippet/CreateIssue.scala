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
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4))

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  val availableServs =
    Seq(github -> "GitHub", icescrum -> "Icescrum", mantis -> "Mantis")

  private object project extends RequestVar(Project(999999, ""): Project)

  private object title extends RequestVar("")

  private object descr extends RequestVar("")

  private object services extends RequestVar(List(): Seq[IPServ])

  def validServices(ls: Seq[IPServ]) =
    if (services.size == 0) "Select at least one service".failNel else ls.success

  def validProject(project: Project) =
    if (project.name == "") "You must choose a project".failNel else project.success

  def validTitle(title: String) =
    if (title == "") "Title cannot be empty".failNel else title.success

  def validDescr(descr: String) =
    if (descr == "") "Description cannot be empty".failNel else descr.success

  def getProjects(srv: IPServ) =  (srv.projects fmap {
    (sei: Seq[Either[Throwable, Project]]) => sei map (_ fold (_ => ("", ""), p => (p.name, p.name)))
  }).claim

  def createIssue(servs: Seq[IPServ], project: Project, title: String, descr: String) =
    servs map (_.createIssue_?(Issue(number = 999999, title = title, body = descr, project = project)))

  def showResult(promise: Promise[Either[Throwable, Issue]]) = promise fmap {
    (ei: Either[Throwable, Issue]) =>
    ei fold (t => ErrorM("", t.getMessage), i => SuccessM("", i.title))
  } to NotifServer

  def clearForm =
      ("project" :: "title" :: "descr" :: Nil map (SetValById(_, ""))).fold(Noop) {_ & _}

  def process() = {
    val result = {
      validServices(services) |@|
      validProject(project) |@|
      validTitle(title) |@|
      validDescr(descr)
    } apply createIssue

    result fold (_.list foreach (S.error), _ foreach showResult)
  }

  val updateServices = (servs: String) => {
    val chosenServs = for {
      str <- servs.split("\\|")
      (srv, name) <- availableServs
      if (str == name)
    } yield srv

    services.update(_ => chosenServs)
  }

  def updateProjects(srvs: Seq[IPServ]) = {
    val allProjects = (srvs map getProjects).distinct.toList

    val commonProjects =
      if (allProjects.size == 1)
        allProjects.flatten
      else {
        for {
          s1 <- allProjects
          s2 <- allProjects filter (_ != s1)
          t1 <- s1
          t2 <- s2
          if (t1 == t2)
        } yield t1
      }.distinct

    ReplaceOptions("project", commonProjects, Empty)
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
    s => s,
    "id" -> "project")

  def render =
    "#servs" #> selServices &
    "#project" #> selProjects &
    "#clear" #> SHtml.ajaxButton("Effacer", () => clearForm) &
    "#title" #> FocusOnLoad(SHtml.textElem(title, "id" -> "title")) &
    "#descr" #> (SHtml.textareaElem(descr, "id" -> "descr") ++ SHtml.hidden(process))
}
