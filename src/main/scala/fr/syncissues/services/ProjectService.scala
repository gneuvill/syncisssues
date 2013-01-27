package fr.syncissues.services

import fr.syncissues.model.Project
import fj.control.parallel.Promise

trait ProjectService {

  def projects: Promise[Seq[Either[Throwable, Project]]]

  def createProject(pr: Project): Promise[Either[Throwable, Project]]

  def deleteProject(pr: Project): Promise[Either[Throwable, Boolean]]

}
