package fr.syncissues
package snippet

import net.liftweb._
import http._
import net.liftweb.util._
import net.liftweb.common._
import Helpers._
import org.specs2.mutable.Specification
import org.specs2.specification.AroundExample
import org.specs2.execute.Result


object CreateIssueSpec extends Specification with AroundExample {
  val session = new LiftSession("", randomString(20), Empty)
  val stableTime = now

  /**
   * For additional ways of writing tests,
   * please see http://www.assembla.com/spaces/liftweb/wiki/Mocking_HTTP_Requests
   */
  def around[T <% Result](body: => T) = {
    S.initIfUninitted(session) {
      body
    }
  }

  "HelloWorld Snippet" should {
    "Put the time in the node" in {
      true
    }
  }
}
