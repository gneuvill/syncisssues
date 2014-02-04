package fr

package object syncissues {
  import scalaz.Semigroup

  implicit val firstThrowable = new Semigroup[Throwable] {
    def append(e1: Throwable, e2: => Throwable) = e1
  }
}
