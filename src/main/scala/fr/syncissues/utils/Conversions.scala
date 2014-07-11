package fr.syncissues.utils

import scala.language.implicitConversions

import fr.syncissues._
import utils.json.Serializer
import model._
import net.liftweb.json.{Serializer => _, _}
import Serialization.write
import biz.futureware.mantis.rpc.soap.client._
import java.math.BigInteger

import scalaz.{\/, \/-, -\/}
import scalaz.Scalaz._
import scalaz.\/._

object Conversions {

  implicit def IntToBigInteger(i: Int) = new BigInteger(i.toString)

  implicit def BigIntegerToInt(bi: BigInteger) = bi.intValue

  implicit class ConvertibleProjectData(pdata: ProjectData) {
    def toProject = Project(pdata.getId, pdata.getName)
  }

  implicit class ConvertibleIssueData(isData: IssueData) {
    def toIssue =
      Issue(isData.getId,
        if (isData.getStatus.getId.toInt == 10) "open" else "closed",
        isData.getSummary,
        isData.getDescription,
        Project(isData.getProject.getId, isData.getProject.getName))
  }

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
