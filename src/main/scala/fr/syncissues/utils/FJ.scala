package fr.syncissues.utils

import scala.language.{implicitConversions, higherKinds}
import scala.collection.JavaConverters._

import dispatch.Promise
import net.liftweb.actor.LiftActor

import fj.{Unit => FJUnit, P1, F, Effect}
import fj.data.{List => FJList}
import fj.control.parallel.{Actor, Strategy, Promise => FJPromise}
import FJPromise._


object FJ {

  implicit def funcToP1[A](sf: => A): P1[A] = new P1[A] { def _1() = sf }

  implicit def fjFTofunc[A, B](fjf: F[A, B]): A => B = a => fjf.f(a)

  implicit def funcTofjF[A, B](sf: A => B): F[A, B] = new F[A, B] { def f(a: A) = sf(a) }

  implicit def dispatchPromiseToFJPromise[A](dp: Promise[A])
    (implicit strat: Strategy[FJUnit]): FJPromise[A] =  promise(strat, dp())

  implicit def fjPromfjListTofjPromSeq[A](prom: FJPromise[FJList[A]]) =
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
