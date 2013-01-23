package fr.syncissues
package services

import net.liftweb.http.Factory
import net.liftweb.util.Props
import fj.control.parallel.Strategy
import java.util.concurrent.Executors

object SyncIsInjector extends Factory {

  def buildGitHub = GitHub(
    Props.get("github.user", "gneuvill"),
    Props.get("github.password", "toto"),
    Props.get("github.owner", "gneuvill"),
    Props.get("github.url", "https://api.github.com"),
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4)))

  def buildIceScrum = IceScrum(
    Props.get("icescrum.user", "gneuvill"),
    Props.get("icescrum.password", "toto"),
    Props.get("icescrum.team", "TSI"),
    Props.get("icescrum.url", "http://localhost:8181/icescrum/ws/p"),
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4)))

  def buildMantis = Mantis(
    Props.get("mantis.user", "gneuvill"),
    Props.get("mantis.password", "toto"),
    Props.get("mantis.url", "http://localhost/mantisbt-1.2.12/api/soap/mantisconnect.php"),
    Strategy.executorStrategy[fj.Unit](Executors.newFixedThreadPool(4)))

  val github = new FactoryMaker(buildGitHub) {}

  val icescrum = new FactoryMaker(buildIceScrum) {}

  val mantis = new FactoryMaker(buildMantis) {}
}
