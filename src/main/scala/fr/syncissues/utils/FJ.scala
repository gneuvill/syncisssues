package fr.syncissues.utils

import fj.{Effect, F, P1, Unit => FJUnit}
import fj.control.parallel.{Actor, Promise => FJPromise, Strategy}
import fj.control.parallel.Promise._
import fj.data.{List => FJList}
import net.liftweb.actor.LiftActor
import scala.collection.JavaConverters._
import scala.concurrent.{promise ⇒ _, _}
import duration._
import scala.language.{higherKinds, implicitConversions, postfixOps}


object FJ {

  implicit def funcToP1[A](sf: => A): P1[A] = new P1[A] { def _1(): A = sf }

  implicit def fjFTofunc[A, B](fjf: F[A, B]): A => B = a => fjf.f(a)

  implicit def funcTofjF[A, B](sf: A => B): F[A, B] = new F[A, B] { def f(a: A): B = sf(a) }

  implicit def scalaFutureToFJPromise[A](dp: Future[A])
    (implicit strat: Strategy[FJUnit]): FJPromise[A] =  promise(strat, new P1[A]() { def _1: A = Await.result(dp, 3 seconds)})

  implicit def fjPromfjListTofjPromSeq[A](prom: FJPromise[FJList[A]]): FJPromise[Seq[A]] =
    prom fmap ((list: FJList[A]) => list.toCollection.asScala.toSeq)

  implicit def funcToEffect[A](sf: A => Unit): Effect[A] = new Effect[A] { def e(a: A) = sf(a) }

  implicit def liftActorToFJActor[M](la: LiftActor)
    (implicit strat: Strategy[FJUnit]): Actor[M] = Actor.actor(strat, (m: M) => la ! m )

  case class FJListConvertible[A, B[A] <: Seq[A]](seq: B[A]) {
    def asFJList: FJList[A] = FJList.list(seq: _*)
  }

  implicit def seqToFJListConvertible[A, B[A] <: Seq[A]](seq: B[A]): FJListConvertible[A, B] =
    FJListConvertible[A, B](seq)

}
