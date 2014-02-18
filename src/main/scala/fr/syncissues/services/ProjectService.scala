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

  protected def projectId(p: Project): Task[Int] =
    projects flatMap { ps ⇒
      (ps find (_.name == p.name)).cata(
        prj ⇒ Task.now(prj.id),
        Task.fail(new Exception(s"Project ${p} doesn't exist")))
    }

  protected def withProjectId[P](p: Project)(f: Int => Task[P]): Task[P] =
    projectId(p) flatMap f
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
