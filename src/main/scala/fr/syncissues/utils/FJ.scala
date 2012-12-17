package fr.syncissues.utils

import fj.P1
import fj.F

object FJ {

  implicit def scalaFToP1[A](sf: => A): P1[A] = new P1[A] { def _1() = sf }

  implicit def fjFToScalaF[A, B](fjf: F[A, B]): A => B = a => fjf.f(a)

  implicit def scalaFtoFjF[A, B](sf: A => B): F[A, B] = new F[A, B] { def f(a: A) = sf(a) }
}



















