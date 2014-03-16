package fr

package object syncissues {
  import scala.language.postfixOps
  import scala.concurrent._
  import scala.concurrent.duration.DurationInt
  import scalaz.{Semigroup, \/}
  import scalaz.concurrent.Task

  implicit val firstThrowable = new Semigroup[Throwable] {
    def append(e1: Throwable, e2: => Throwable) = e1
  }

  implicit class MyFuture[T](fut: Future[Throwable \/ T]) {
    def asTask: Task[T] = Task(Await.result[Throwable \/ T](fut, 5 seconds)) flatMap {
      _.fold(Task.fail, t ⇒ Task.now(t))
    }
  }
}
