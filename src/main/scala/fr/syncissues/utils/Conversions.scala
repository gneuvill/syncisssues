package fr.syncissues.utils

import fr.syncissues.beans._
import net.liftweb.json._
import biz.futureware.mantis.rpc.soap.client._
import java.math.BigInteger

object Conversions {

  implicit def IntToBigInteger(i: Int) = new BigInteger(i.toString())

  implicit def BigIntegerToInt(bi: BigInteger) = bi.intValue

  def toBean[T](json: JValue)(implicit format: Formats, mf: Manifest[T]): Either[Throwable, T] =
    try {
      Right(json.extract[T])
    } catch {
      case e: MappingException => Left(e)
    }

  def toProject(json: JValue)(implicit format: Formats) = toBean[Project](json)

  def toIssue(json: JValue)(implicit format: Formats) = toBean[Issue](json)

  def toProject(pdata: ProjectData) = Project(pdata.getName)

  def toIssue(isData: IssueData): Issue =
    Issue(isData.getId, "open", isData.getSummary, isData.getDescription)

  def toIssueData(project: Int, category: String, is: Issue): IssueData = new IssueData() {
    setSummary(is.title)
    setDescription(is.body)
    setStatus(new ObjectRef() { setName(is.state) })
    setCategory(category)
    setProject(new ObjectRef() { setId(project) })
  }

}



















