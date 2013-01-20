package fr.syncissues.beans

sealed trait Message {
  def compId: String
  def content: String
}

case class SuccessM(compId: String = "", content: String = "") extends Message
case class InfoM(compId: String = "", content: String = "") extends Message
case class ErrorM(compId: String = "", content: String = "") extends Message


