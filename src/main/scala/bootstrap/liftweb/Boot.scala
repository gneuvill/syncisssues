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

import net.liftmodules.FoBo

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("fr.syncissues")

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMap(Paths.siteMap)

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
    LiftRules.jsArtifacts = JQueryArtifacts
    JQueryModule.InitParam.JQuery = JQueryModule.JQuery172
    JQueryModule.init()

    // let's use reactive-web
    Reactions.init(true)

    // let's use FoBo
    FoBo.InitParam.ToolKit = FoBo.Bootstrap222
    FoBo.InitParam.ToolKit = FoBo.FontAwesome200
    FoBo.init()

  }
}

object Paths {

  val home = Menu.i("Home") / "index"
  val create = Menu("Créer") / "issues" / "create"
  val syn = Menu("Synchroniser") / "issues" / "sync"
  val delete = Menu("Fermer") / "issues" / "close"

  val siteMap = SiteMap(
    home >> LocGroup("homeG") >> FoBo.TBLocInfo.NavHeader,
    create >> LocGroup("createG") >> FoBo.TBLocInfo.NavHeader,
    syn >> LocGroup("synG") >> FoBo.TBLocInfo.NavHeader,
    delete >> LocGroup("closeG") >> FoBo.TBLocInfo.NavHeader)

}
