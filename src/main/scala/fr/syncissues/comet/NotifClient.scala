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
  // private var msgs: List[Message] = Nil

  private def li(cssClass: String, m: Message) =
    <li class={"message " + cssClass + " clearable"} onclick={"$(this).fadeOut('slow', function(){alert('done')}); " + SHtml.ajaxCall("''", s => NotifServer ! ("delete", m)).toJsCmd}>{m.content}</li>

  private def messageToHtml(m: Message) = m match {
    case SuccessM(idComp, content) => li("success", m)
    case InfoM(idComp, content) => li("info", m)
    case ErrorM(idComp, content) => li("error", m)
  }

  private def renderMessages: PartialFunction[List[Message], Unit] = {
    case lm => partialUpdate {
      SetHtml("notifs", lm map messageToHtml)
    }
  }

  def registerWith = NotifServer

  override def lowPriority =  renderMessages.asInstanceOf[PartialFunction[Any, Unit]]

  def render = ClearClearable

}
