
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.sbt.PlayScala
import uk.gov.hmrc.SbtAutoBuildPlugin

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.ShellPrompt

  val appName: String
  val appVersion: String

  lazy val appDependencies: Seq[ModuleID] = Seq.empty
  lazy val plugins: Seq[Plugins] = Seq(PlayScala)
  lazy val playSettings: Seq[Setting[_]] = Seq.empty

  lazy val microservice: Project = Project(appName, file("."))
    .enablePlugins(SbtAutoBuildPlugin)
    .enablePlugins(plugins: _*)
    .settings(playSettings: _*)
    .settings(version := appVersion)
    .settings(scalaSettings: _*)
    .settings(scalaVersion := "2.11.8")
    .settings(scalacOptions ++= Seq("-Xlint:-missing-interpolator"))
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      shellPrompt := ShellPrompt(appVersion),
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true
    )
    .settings(Provenance.setting)
    .settings(Repositories.playPublishingSettings: _*)

    .configs(Test)
    .settings(
      unmanagedSourceDirectories in Test <<= (baseDirectory in Test) (base => Seq(base / "test" / "unit")),
      addTestReportOption(Test, "test-reports"))

    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in Test)(base => Seq(base / "test" / "it")),
      unmanagedResourceDirectories in IntegrationTest <<= (baseDirectory in Test) (base => Seq(base / "test" / "resources")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      parallelExecution in IntegrationTest := false)

    .settings(
      sources in doc in Compile := List(),
      sources in doc in Test := List(),
      publishArtifact in(Compile, packageDoc) := false,
      publishArtifact in(Compile, packageSrc) := true,
      publishArtifact in(Test, packageDoc) := false,
      publishArtifact in(Test, packageSrc) := true,
      publishArtifact in(Compile, packageBin) := true
    )
    .settings(resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.jcenterRepo
    ))
    .settings(evictionWarningOptions in update :=
      EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
        .withWarnDirectEvictions(false).withWarnScalaVersionEviction(false))
}

private object TestPhases {
  def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
    tests map {
      test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}

private object Repositories {

  import uk.gov.hmrc._
  import PublishingSettings._

  lazy val playPublishingSettings: Seq[sbt.Setting[_]] = sbtrelease.ReleasePlugin.releaseSettings ++ Seq(

    credentials += SbtCredentials,

    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := true

  ) ++
    publishAllArtefacts
}
