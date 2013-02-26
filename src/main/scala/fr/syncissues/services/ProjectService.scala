package fr.syncissues.services

import fr.syncissues.utils.FJ._
import fr.syncissues.model.{ Issue, Project }

import fj.control.parallel.{ Promise, Strategy }
import Promise.promise

trait ProjectService {

  def projects: Promise[Seq[Either[Throwable, Project]]]

  def createProject(pr: Project): Promise[Either[Throwable, Project]]

  def deleteProject(pr: Project): Promise[Either[Throwable, Boolean]]

  protected def projectId(p: Project) = projects fmap {
    s: Seq[Either[Throwable, Project]] => {
        s find (_.right exists (_.name == p.name)) flatMap (_.right.toOption map (_.id))
      }.toRight(new Exception("Project %s doesn't exist".format(p.name)))
  }

  protected def withProjectId(p: Project)(f: Int => Promise[Either[Throwable, Issue]])
    (implicit strat: Strategy[fj.Unit]) =
    projectId(p) bind { ei: Either[Throwable, Int] =>
      ei.fold[Promise[Either[Throwable, Issue]]](t => promise(strat, Left(t)), f)
    }
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
