package fr.syncissues.comet

import fr.syncissues.model._
import net.liftweb._
import http._
import common._
import util.Helpers._
import util.ClearClearable
import js._
import JsCmds._

class NotifClient extends CometListener {

  private def div(cssClass: String, m: Message) =
    <div class={ "message " + cssClass + " clearable" }
      onclick={"$(this).delay(10000).fadeOut('slow', function(){ " +
        SHtml.ajaxCall("''", s =>
	  NotifServer ! ("delete", m)).toJsCmd + "; });"}>{ m.content }</div>

  private def messageToHtml(m: Message) = m match {
    case SuccessM(idComp, content) => div("alert alert-success", m)
    case InfoM(idComp, content) => div("alert alert-info", m)
    case ErrorM(idComp, content) => div("alert alert-error", m)
  }

  private def renderMessages: PartialFunction[List[Message], Unit] = {
    case lm => partialUpdate {
      SetHtml("notifs", lm map messageToHtml)
    }
  }

  def registerWith = NotifServer

  override def lowPriority = renderMessages.asInstanceOf[PartialFunction[Any, Unit]]

  def render = ClearClearable

}
