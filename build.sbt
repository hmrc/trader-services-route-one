import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.SbtAutoBuildPlugin

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
val bootstrapVersion = "8.4.0"

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc"                  %% "bootstrap-backend-play-28" % bootstrapVersion,
  "org.typelevel"                %% "cats-core"                 % "2.7.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.14.2"
)

def testDeps(scope: String) =
  Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28" % bootstrapVersion % scope,
    "org.scalatest"       %% "scalatest"              % "3.2.11"         % scope,
    "com.vladsch.flexmark" % "flexmark-all"           % "0.62.2"         % scope
  )

lazy val itDeps = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"  % "it",
  "com.github.tomakehurst"  % "wiremock-jre8"      % "2.32.0" % "it"
)

lazy val root = (project in file("."))
  .settings(
    name := "trader-services-route-one",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.13.12",
    PlayKeys.playDefaultPort := 9380,
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it") ++ itDeps,
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .settings(libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / scalafmtOnCompile := true,
    majorVersion := 0
  )
  .settings(headerSettings(IntegrationTest): _*)
  .settings(automateHeaderSettings(IntegrationTest))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)

inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
