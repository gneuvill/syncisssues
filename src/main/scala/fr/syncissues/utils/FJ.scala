package fr.syncissues.utils

import fj.P1
import fj.F
import dispatch.Promise
import fj.control.parallel.{Strategy, Promise => FJPromise}
import FJPromise._
import fj.{Unit => FJUnit}

object FJ {

  implicit def scalaFToP1[A](sf: => A): P1[A] = new P1[A] { def _1() = sf }

  implicit def fjFToScalaF[A, B](fjf: F[A, B]): A => B = a => fjf.f(a)

  implicit def scalaFtoFjF[A, B](sf: A => B): F[A, B] = new F[A, B] { def f(a: A) = sf(a) }

  implicit def dispatchPromiseToFJPromise[A](dp: Promise[A])
    (implicit strat: Strategy[FJUnit]): FJPromise[A] =  promise(strat, dp())
}



















