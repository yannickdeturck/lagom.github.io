lazy val `lagom-docs` = (project in file("."))
  .enablePlugins(SbtTwirl, SbtWeb)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.webjars" % "normalize.css" % "3.0.2",
  "org.webjars" % "foundation" % "6.2.0",
  "org.webjars" % "jquery" % "2.2.1",
  "org.webjars" % "prettify" % "4-Mar-2013"
)

val generateHtml = taskKey[Seq[File]]("Generate the site HTML")

target in generateHtml := WebKeys.webTarget.value / "generated-html"
generateHtml <<= Def.taskDyn {
  val outputDir = (target in generateHtml).value
  Def.task {
    (run in Compile).toTask(Seq(
      outputDir
    ).mkString(" ", " ", "")).value
    outputDir.***.filter(_.isFile).get
  }
}

WebKeys.pipeline ++= {
  generateHtml.value pair relativeTo((target in generateHtml).value)
}
