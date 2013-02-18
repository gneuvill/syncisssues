package fr.syncissues.services

import fr.syncissues.model.Project
import fj.control.parallel.Promise

import scala.annotation.tailrec

trait ProjectService {

  def projects: Promise[Seq[Either[Throwable, Project]]]

  def createProject(pr: Project): Promise[Either[Throwable, Project]]

  def deleteProject(pr: Project): Promise[Either[Throwable, Boolean]]

}

object ProjectService {

  def commonProjects(llProjects: Seq[Seq[Project]]): Seq[Project] =
    if (llProjects.size <= 1)
      llProjects.headOption getOrElse Seq()
    else
      llProjects.head filter { p =>
        llProjects.tail forall (_ exists (_.name == p.name))
      }
}
