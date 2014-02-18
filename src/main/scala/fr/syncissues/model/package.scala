package fr.syncissues

package object model {
  import argonaut._, Argonaut._

  implicit def ProjectEncodeJson: EncodeJson[Project] =
    jencode2L((p: Project) ⇒ (p.id, p.name))("id", "name")

  implicit def IssueEncodeJson: EncodeJson[Issue] =
    jencode5L((is: Issue) ⇒
      (is.number,
        is.state,
        is.title,
        is.body,
        is.project))("number", "state", "title", "body", "project")

  implicit class JsonProject(pr: Project) {
    def toJson: String = pr.asJson.nospaces
  }

  implicit class JsonIssue(is: Issue) {
    def toJson: String = is.asJson.nospaces
  }
}
