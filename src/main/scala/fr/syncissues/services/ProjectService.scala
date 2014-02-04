package fr.syncissues.services

import fr.syncissues.utils.FJ._
import fr.syncissues.model.{ Issue, Project }
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalaz.\/
import scalaz.Scalaz._

trait ProjectService {

  def projects: Future[Seq[Throwable \/ Project]]

  def createProject(pr: Project): Future[Throwable \/ Project]

  def deleteProject(pr: Project): Future[Throwable \/ Boolean]

  protected def projectId(p: Project)(implicit e: ExecutionContext): Future[Throwable \/ Int] =
    projects map {
      s ⇒ {
        for {
          ei ← s find (_ exists (_.name == p.name))
          prj ← ei.toOption
        } yield prj.id
      } \/> (new Exception("Project %s doesn't exist".format(p.name)))
    }

  protected implicit def throwProm[E <: Throwable,T]: E ⇒ E \/ T = (e: E) ⇒ e.left

  protected def withProjectId[P](p: Project)(f: Int => Future[P])
    (implicit e: ExecutionContext, toP: Throwable => P): Future[P] =
    projectId(p) flatMap { ei ⇒
      ei.fold[Future[P]](t ⇒ Future(toP(t)), f)
    }
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
