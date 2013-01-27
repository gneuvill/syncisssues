package fr.syncissues.utils.json

import fr.syncissues.model._
import net.liftweb.json._
import java.io.Writer

trait Serializer[T, S] {
  def serialize(t: T): S
  def format: Formats
}

object Serializer {
  implicit def defaultSerializer[T <: AnyRef]: Serializer[T, String] = new Serializer[T, String] {
    def serialize(t: T) = Serialization.write(t)(DefaultFormats)
    def format = DefaultFormats
  }

  def apply[T <: AnyRef](f: Formats): Serializer[T, String] = new Serializer[T, String] {
    def serialize(t: T) = Serialization.write(t)(f)
    def format = f
  }

  def apply[T <: AnyRef, W <: Writer](f: Formats, out: W): Serializer[T, W] = new Serializer[T, W] {
    def serialize(t: T) = Serialization.write(t, out)(f)
    def format = f
  }
}
