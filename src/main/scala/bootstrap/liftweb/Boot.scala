package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import net.liftmodules.JQueryModule
import net.liftweb.http.js.jquery._

import reactive.web.Reactions

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // let's use reactive-web
    Reactions.init(true)

    // where to search snippet
    LiftRules.addToPackages("fr.syncissues")

    // Build SiteMap
    val entries = List(
      Menu.i("Home") / "index" submenus (
        Menu("Créer") / "issues" / "create",
        Menu("Synchroniser") / "issues" / "sync"
    ))

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(SiteMap(entries:_*))

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    //Init the jQuery module, see http://liftweb.net/jquery for more information.
    // LiftRules.jsArtifacts = JQueryArtifacts
    // JQueryModule.InitParam.JQuery=JQueryModule.JQuery172
    // JQueryModule.init()

  }
}
