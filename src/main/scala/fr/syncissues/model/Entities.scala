package fr.syncissues.model

case class Issue(
  number: Int = 999999,
  state: String = "",
  title: String = "",
  body: String = "",
  project: Project = Project())

case class Project(id: Int = 999999, name: String = "")
