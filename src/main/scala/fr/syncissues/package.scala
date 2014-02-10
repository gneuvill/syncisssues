package fr

package object syncissues {
  import scala.concurrent._
  import duration.Duration
  import scalaz.Semigroup
  import scalaz.concurrent.Task

  implicit val firstThrowable = new Semigroup[Throwable] {
    def append(e1: Throwable, e2: => Throwable) = e1
  }

  implicit class MyFuture[T](fut: Future[T]) {
    def asTask: Task[T] = Task(Await.result(fut, Duration.Inf))
  }
}
