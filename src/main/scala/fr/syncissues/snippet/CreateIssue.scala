package fr.syncissues
package snippet

import fr.syncissues.services.SyncIsInjector

import net.liftweb.util._
import net.liftweb.common._
import java.util.Date
import Helpers._
import net.liftweb.util.Props

object CreateIssue {

  val github = SyncIsInjector.github.vend

  val icescrum = SyncIsInjector.icescrum.vend

  val mantis = SyncIsInjector.mantis.vend

  val opt = <div>{icescrum}</div>

  def render = "#where" #> opt
  
}




