import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / scalaVersion := "3.3.6"
ThisBuild / majorVersion := 4

lazy val microservice = Project("address-lookup", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, BuildInfoPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions ++= Seq(
      "-Xlint:-missing-interpolator",
      "-Wconf:msg=Flag.*repeatedly:s"
    ),
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "buildinfo"
  )
  .settings(
    Test / parallelExecution := false,
    Test / fork := false,
    retrieveManaged := true
  )
  .settings(PlayKeys.playDefaultPort := 9022)

lazy val it = project.in(file("it"))
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
