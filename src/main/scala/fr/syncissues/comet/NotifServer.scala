package fr.syncissues.comet

import fr.syncissues.model.Message

import net.liftweb.actor.LiftActor
import net.liftweb.http.ListenerManager

object NotifServer extends LiftActor with ListenerManager {
  
  private var msgs: List[Message] = Nil

  def createUpdate = msgs

  override def lowPriority = {
    case ("delete", msg: Message) => msgs = msgs filterNot (_ == msg)
    case msg: Message =>
      msgs ::= msg
      updateListeners()
  }
}
