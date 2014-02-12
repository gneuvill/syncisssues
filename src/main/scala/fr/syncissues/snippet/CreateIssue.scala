package fr.syncissues
package snippet

import fr.syncissues._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalaz.concurrent.Task
import services._
import ProjectService._
import model._
import utils._
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

  implicit val exec =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(32))

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  val availableServs =
    Vector(github -> "GitHub", icescrum -> "IceScrum", mantis -> "Mantis")

  val (hidServs, hidProject, hidTitle, hidDescr) = ("servs", "project", "title", "descr")

  private object allProjects extends RequestVar(Vector(): Seq[(IPServ, Project)])

  private object projectName extends RequestVar("")

  private object title extends RequestVar("")

  private object descr extends RequestVar("")

  private object services extends RequestVar(Vector(): Seq[IPServ])

  def validServices(ls: Seq[IPServ]) =
    if (services.size == 0) (hidServs, "Sélectionnez au moins un service !").failNel
    else ls.success

  def validProject(projectName: String) =
    if (projectName == "") (hidProject, "Choisissez un projet !").failNel
    else projectName.success

  def validTitle(title: String) =
    if (title == "") (hidTitle, "Le titre ne peut être vide !").failNel
    else title.success

  def validDescr(descr: String) =
    if (descr == "") (hidDescr, "La description ne peut être vide !").failNel
    else descr.success

  def getAllProjects(srvs: Seq[IPServ]) =
    srvs map (srv ⇒ (srv, srv.projects.attemptRun.toList.toVector.flatten))

  def createIssue(servs: Seq[IPServ], projectName: String,
    title: String, descr: String) =
    for {
      srv <- servs
      srvPr <- allProjects.get find (t => t._1 == srv && t._2.name == projectName)
    } yield (srv, srv.createIssue_?(
      Issue(title = title, body = descr, project = Project(srvPr._2.id, srvPr._2.name))))

  def showResult(res: (IPServ, Task[Issue])) = res._2 runAsync { ei => {
      val className = res._1.getClass.getSimpleName
      NotifServer ! {
        ei fold (
          t => ErrorM("", s"""|An issue could not be created in ${className}
                              | because :\n${t.getMessage}""".stripMargin),
          i => SuccessM("", s"${i.title} has been created in ${className}"))
      }
    }
  }

  def clearForm = {
    List(hidServs, hidProject, hidTitle, hidDescr) map (SetValById(_, ""))
  }.fold(Noop) { _ & _ }

  def process() = {
    val result = {
      validServices(services) |@|
        validProject(projectName) |@|
        validTitle(title) |@|
        validDescr(descr)
    } apply createIssue

    result fold (
      el => {
        val okFields =
          List(hidServs, hidProject, hidTitle, hidDescr) diff (el.list map (_._1))
        okFields foreach (id => S.appendJs(SetHtml(id + "-error", <span/>)))
        el.list foreach (t => S.error(t._1 + "-error", t._2))
      },
      _ foreach showResult)
  }

  val updateServices = (servs: String) => {
    val chosenServs = for {
      str <- (servs split ("\\|")).toVector
      (srv, name) <- availableServs
      if (str == name)
    } yield srv

    services update (_ => chosenServs)
  }

  def updateProjects(srvs: Seq[IPServ]) = {
    val srvProjects  = getAllProjects(srvs)

    allProjects update { _ =>
      for {
        t <- srvProjects
        p <- t._2
      } yield t._1 -> p
    }

    val common = commonProjects(srvProjects map (_._2)).toList

    ReplaceOptions(
      hidProject,
      ("", "---- Select ----") :: (common map (p => (p.name, p.name))),
      Empty)
  }

  val servsVal = JsRaw(
    """Array.prototype.slice.call(this.children)
       .filter(function(opt) { return opt.selected == true })
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
    Vector(("", "---- Select ----")): Vector[Tuple2[String, String]],
    Empty,
    projectName.set,
    "id" -> hidProject)

  def render =
    ("#" + hidServs) #> selServices &
      ("#" + hidProject) #> selProjects &
      "#clear" #> SHtml.ajaxButton("Ré-initialiser", () => clearForm) &
      ("#" + hidTitle) #> SHtml.textElem(title, "id" -> hidTitle, "class" -> "input-block-level") &
      ("#" + hidDescr) #> (SHtml.textareaElem(descr, "id" -> hidDescr, "class" -> "input-block-level")
        ++ SHtml.hidden(process))
}
