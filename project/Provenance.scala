import java.text.SimpleDateFormat
import java.util.Date

import sbt.Keys._
import sbt._

import scala.util.Properties._

object Provenance {

  def run(command: String): String = (command !!).trim

  def makeProvenanceSources(appVersion:String, base: File): Seq[File] = {
    val tag = envOrElse("BUILD_TAG", "DEVELOPER")
    val number = envOrElse("BUILD_NUMBER", "")
    val id = envOrElse("BUILD_ID", "")
    val url = envOrElse("JOB_URL", "")
    val commit = run("git rev-parse HEAD").replace(':', '_')
    val remote = run("git remote")
    val branch = run("git rev-parse --abbrev-ref HEAD")
    val time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())
    val file = base / "provenance.json"
    if (!file.exists()) {
      val text =
        s"""{
        |  "appName":     "${MicroServiceBuild.appName}",
        |  "version":     "${appVersion}",
        |  "buildTag":    "$tag",
        |  "buildNumber": "$number",
        |  "buildId":     "$id",
        |  "jobUrl":      "$url",
        |  "gitCommit":   "$commit",
        |  "gitBranch":   "$remote/$branch",
        |  "timestamp":   "$time"
        |}""".stripMargin
      IO.write(file, text)
    }
    Seq(file)
  }

  def provenanceTask = Def.task {
    val base = (resourceManaged in Sources).value
    makeProvenanceSources(version.value, base / "resources")
  }

  def setting: Setting[_] = resourceGenerators in Compile += provenanceTask.taskValue
}
