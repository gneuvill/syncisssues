package fr.syncissues

package object services {
  import argonaut._, Argonaut._
  import com.ning.http.client.Response
  import fr.syncissues.model._

  object as {
    object Issue extends (Response ⇒ Issue) {
      def apply(r: Response) = ???
    }

    object Issues extends (Response ⇒ Vector[Issue]) {
      def apply(r: Response) = ???
    }

    object Project extends (Response ⇒ Project) {
      def apply(r: Response) = ???
    }

    object Projects extends (Response ⇒ Vector[Project]) {
      def apply(r: Response) = ???
    }
  }
}

