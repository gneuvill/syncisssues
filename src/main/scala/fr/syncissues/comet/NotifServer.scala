package fr.syncissues.comet

import fr.syncissues.beans.Message

import net.liftweb.actor.LiftActor
import net.liftweb.http.ListenerManager

object NotifServer extends LiftActor with ListenerManager {
  
  private var msgs: List[Message] = Nil

  def createUpdate = msgs

  override def lowPriority = {
    case msg: Message =>
      msgs ::= msg
      println(msg)
      //updateListeners()
  }
}
