import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

lazy val root = Project("address-lookup", file("."))
    .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
    .settings(majorVersion := 4)
    .settings(scalaVersion := "2.12.15")
    .settings(publishingSettings)
    .settings(
      targetJvm := "jvm-1.8",
      libraryDependencies ++= AppDependencies(),
      retrieveManaged := true,
      Compile / doc / sources := List(),
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := true,
      Compile / packageBin / publishArtifact := true
    )
    .settings(
      Test / unmanagedSourceDirectories := (Test / baseDirectory) (base => Seq(base / "test" / "unit")).value,
      Test / doc / sources := List(),
      Test / packageDoc / publishArtifact := false,
      addTestReportOption(Test, "test-reports"),
      Test / fork := false,
      Test / parallelExecution := false
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      IntegrationTest / unmanagedSourceDirectories := (Test / baseDirectory) (base => Seq(base / "test" / "it")).value,
      IntegrationTest / unmanagedResourceDirectories := (Test / baseDirectory) (base => Seq(base / "test" / "resources")).value,
      addTestReportOption(IntegrationTest, "int-test-reports"),
      IntegrationTest / Keys.fork := false,
      IntegrationTest / parallelExecution := false
    )
    .settings(
      update / evictionWarningOptions := EvictionWarningOptions.default
                                                               .withWarnTransitiveEvictions(false)
                                                               .withWarnDirectEvictions(false)
                                                               .withWarnScalaVersionEviction(false)
    )
    .settings(resolvers ++= Seq(Resolver.jcenterRepo))
