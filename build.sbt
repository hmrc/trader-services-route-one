import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.traderservices\.wiring;uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

val bootstrapVersion = "8.4.0"

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc"                  %% "bootstrap-backend-play-30" % bootstrapVersion,
  "org.typelevel"                %% "cats-core"                 % "2.10.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.16.1"
)

def testDeps: Seq[ModuleID] =
  Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatest"          %% "scalatest"              % "3.2.17",
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.62.2",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.1",
    "org.wiremock"  % "wiremock"      % "3.4.2"
  ).map(_ % Test)

lazy val root = (project in file("."))
  .settings(
    name := "trader-services-route-one",
    organization := "uk.gov.hmrc",
    PlayKeys.playDefaultPort := 9380,
    libraryDependencies ++= compileDeps ++ testDeps,
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .settings(libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  .settings(headerSettings(Test): _*)
  .settings(automateHeaderSettings(Test))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= testDeps)
  .settings(inConfig(Test)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings))
