package fr.syncissues.services

import fr.syncissues.utils.FJ._
import fr.syncissues.model.{ Issue, Project }
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalaz._
import scalaz.Scalaz._
import scalaz.concurrent.Task

trait ProjectService {

  def projects: Task[Vector[Project]]

  def createProject(pr: Project): Task[Project]

  def deleteProject(pr: Project): Task[Boolean]

  final def findProject(p: Project)(implicit ev: Equal[Project]): Task[Project] =
    projects flatMap { ps ⇒
      (ps find p.===).cata(
        prj ⇒ Task.now(prj),
        Task.fail(new Exception(s"Project ${p} doesn't exist")))
    }

  final def projectId(p: Project): Task[Int] =
    findProject(p)(Cord.CordEqual.contramap(_.name)) map (_.id)

  final def projectName(p: Project): Task[String] =
    findProject(p)(Equal[Int].contramap(_.id)) map (_.name)

  final def withProjectId[P](p: Project)(f: Int => Task[P]): Task[P] =
    projectId(p) flatMap f

  final def withProjectName[P](p: Project)(f: String => Task[P]): Task[P] =
    projectName(p) flatMap f
}

object ProjectService {

  def commonProjects(llProjects: Seq[Seq[Project]]): Seq[Project] =
    if (llProjects.size <= 1)
      llProjects.headOption getOrElse Seq()
    else
      llProjects.head filter { p ⇒
        llProjects.tail forall (_ exists (_.name == p.name))
      }
}
