import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val thisScalaVersion = "2.12.15"

lazy val root = Project("address-lookup", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .enablePlugins(Seq(PlayScala): _*)
  .settings(majorVersion := 4)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := thisScalaVersion)
  .settings(scalacOptions ++= Seq("-Xlint:-missing-interpolator"))
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= AppDependencies(),
    Test / parallelExecution := false,
    Test / fork := false,
    retrieveManaged := true
  )
  .configs(Test)
  .settings(
    Test / unmanagedSourceDirectories := (Test / baseDirectory) (base => Seq(base / "test" / "unit")).value,
    addTestReportOption(Test, "test-reports"))
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories  :=  (Test / baseDirectory)(base => Seq(base / "test" / "it")).value,
    IntegrationTest / unmanagedResourceDirectories  :=  (Test / baseDirectory) (base => Seq(base / "test" / "resources")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false)
  .settings(
    Compile / doc / sources := List(),
    Test / doc / sources := List(),
    Compile / packageDoc / publishArtifact := false,
    Compile / packageSrc / publishArtifact := true,
    Test / packageDoc / publishArtifact := false,
    Test / packageSrc / publishArtifact := true,
    Compile / packageBin / publishArtifact := true
  )
  .settings(resolvers ++= Seq(Resolver.jcenterRepo))
  .settings(update / evictionWarningOptions :=
    EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
      .withWarnDirectEvictions(false).withWarnScalaVersionEviction(false))
  .enablePlugins(SbtDistributablesPlugin, SbtAutoBuildPlugin, SbtGitVersioning)
