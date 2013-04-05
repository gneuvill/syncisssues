package fr.syncissues.utils

import scala.language.implicitConversions

import fr.syncissues._
import utils.json.Serializer
import model._
import net.liftweb.json.{Serializer => _, _}
import Serialization.write
import biz.futureware.mantis.rpc.soap.client._
import java.math.BigInteger

object Conversions {

  implicit def IntToBigInteger(i: Int) = new BigInteger(i.toString)

  implicit def BigIntegerToInt(bi: BigInteger) = bi.intValue

  def write[T <: AnyRef](t: T)(implicit serializer: Serializer[T, String]) =  serializer.serialize(t)

  def toEntity[T](json: JValue)(implicit serializer: Serializer[T, String], mf: Manifest[T]): Either[Throwable, T] =
    try {
      implicit val format = serializer.format
      Right(json.extract[T])
    } catch {
      case e: MappingException => Left(e)
    }

  def toProject(implicit serializer: Serializer[Project, String]) = toEntity[Project] _

  def toIssue(implicit serializer: Serializer[Issue, String]) = toEntity[Issue] _

  def toProject(pdata: ProjectData) = Project(pdata.getId, pdata.getName)

  def toIssue(isData: IssueData) =
    Issue(isData.getId,
      if (isData.getStatus.getId.toInt == 10) "open" else "closed",
      isData.getSummary,
      isData.getDescription,
      Project(isData.getProject.getId, isData.getProject.getName))

  def toIssueData(category: String, is: Issue) = new IssueData() {
    setSummary(is.title)
    setDescription(is.body)
    setStatus(new ObjectRef() {
      val (id, name) =
        if (is.state == "open") (10, "new") else (90, "closed")
      setId(id)
      setName(name)
    })
    setCategory(category)
    setProject(new ObjectRef() {
      setId(is.project.id)
      setName(is.project.name)
    })
  }

  def toProjectData(pr: Project) = new ProjectData() {
    setName(pr.name)
  }

}
