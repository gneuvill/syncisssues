package fr.syncissues.comet

import fr.syncissues.beans._
import net.liftweb._
import http._
import common._
import util.Helpers._
import util.ClearClearable
import js._
import JsCmds._

class NotifClient extends CometListener {
  // private var msgs: List[Message] = Nil

  def registerWith = NotifServer

  def messageToHtml(m: Message) = m match {
    case SuccessM(idComp, content) => <li class="success clearable">{content}</li>
    case InfoM(idComp, content) => <li class="info clearable">{content}</li>
    case ErrorM(idComp, content) => <li class="error clearable">{content}</li>
  }

  def renderMessages: PartialFunction[List[Message], Unit] = {
    case lm => partialUpdate {
      SetHtml("notifs", lm map messageToHtml)
    }
  }

  override def lowPriority =  renderMessages.asInstanceOf[PartialFunction[Any, Unit]]

  def render = ClearClearable

}




