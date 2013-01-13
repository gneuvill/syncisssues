package fr.syncissues.utils

import fr.syncissues.beans.Issue
import net.liftweb.json._
import net.liftweb.json.MappingException
import biz.futureware.mantis.rpc.soap.client.IssueData
import biz.futureware.mantis.rpc.soap.client.ObjectRef
import java.math.BigInteger

object Conversions {

  implicit def IntToBigInteger(i: Int) = new BigInteger(i.toString())

  implicit def BigIntegerToInt(bi: BigInteger) = bi.intValue

  def toIssue(json: JValue)(implicit format: Formats): Either[Throwable, Issue] =
    try {
      Right(json.extract[Issue])
    } catch {
      case e: MappingException => Left(e)
    }

  def toIssue(isData: IssueData): Issue = {
    Issue(isData.getId, "open", isData.getSummary, isData.getDescription)
  }

  def toIssueData(project: String, category: String, is: Issue): IssueData = new IssueData() {
    setSummary(is.title)
    setDescription(is.body)
    setStatus(new ObjectRef() { setName(is.state) })
    setCategory(category)
    setProject(new ObjectRef() { setId(project.toInt) })
  }

}



















