package fr

package object syncissues {
<<<<<<< HEAD
  import scalaz.Semigroup
=======
  import scala.language.postfixOps
  import scala.concurrent._
  import scala.concurrent.duration.DurationInt
  import scalaz.{Semigroup, \/}
  import scalaz.concurrent.Task
>>>>>>> 6546250713e82a04a5232e28ede6f0e5f8c9a4e9

  implicit val firstThrowable = new Semigroup[Throwable] {
    def append(e1: Throwable, e2: => Throwable) = e1
  }
<<<<<<< HEAD
=======

  implicit class MyFuture[T](fut: Future[Throwable \/ T]) {
    def asTask: Task[T] = Task(Await.result[Throwable \/ T](fut, 5 seconds)) flatMap {
      _.fold(Task.fail, t ⇒ Task.now(t))
    }
  }
>>>>>>> 6546250713e82a04a5232e28ede6f0e5f8c9a4e9
}
