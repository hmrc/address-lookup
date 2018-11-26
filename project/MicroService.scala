
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.sbt.PlayScala
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.SbtArtifactory

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import uk.gov.hmrc.ShellPrompt

  val appName: String


  lazy val appDependencies: Seq[ModuleID] = Seq.empty
  lazy val plugins: Seq[Plugins] = Seq(PlayScala)
  lazy val playSettings: Seq[Setting[_]] = Seq.empty

  lazy val microservice: Project = Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
    .enablePlugins(plugins: _*)
    .settings(majorVersion := 4)
    .settings(playSettings: _*)
    .settings(scalaSettings: _*)
    .settings(scalaVersion := "2.11.11")
    .settings(scalacOptions ++= Seq("-Xlint:-missing-interpolator"))
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
    )
    .settings(Provenance.setting)

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
    .enablePlugins(SbtDistributablesPlugin, SbtAutoBuildPlugin, SbtGitVersioning)
}

private object TestPhases {
  def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
    tests map {
      test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}