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

  @tailrec
  def commonProjects(llProjects: List[List[Project]], acc: List[Project] = Nil): List[Project] =
    llProjects match {
      case Nil => Nil
      case lp :: Nil => acc.distinct
      case lp :: rest =>
        val sameNames = lp filter (p => { rest forall (_ exists (_.name == p.name)) })
        commonProjects(rest, acc ++ sameNames)
    }
}
