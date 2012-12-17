package fr.syncissues.utils

import fr.syncissues.beans.Issue
import net.liftweb.json._
import net.liftweb.json.MappingException
import biz.futureware.mantis.rpc.soap.client.IssueData
import java.math.BigInteger

object Conversions {

  implicit def IntToBigInteger(i: Int) = new java.math.BigInteger(i.toString())

  implicit def BigIntegerToInt(bi: BigInteger) = bi.intValue

  def toIssue(json: JValue)(implicit format: Formats): Either[MappingException, Issue] =
    try {
      Right(json.extract[Issue])
    } catch {
      case e: MappingException => Left(e)
    }

  def toIssue(isData: IssueData): Issue =
    Issue(isData.getId, "open", isData.getSummary, isData.getDescription)

}



















