package com.lightbend.lagom.docs

import java.io.File
import java.nio.file.Files

import play.twirl.api.{Html, Template0}

object DocumentationGenerator extends App {

  println("Generating documentation...")

  val outputDir = new File(args(0))

  def generatePage(name: String, template: Template0[Html]) = {
    val rendered = template.render()
    val file = new File(outputDir, name)
    file.getParentFile.mkdirs()
    Files.write(file.toPath, rendered.body.getBytes("utf-8"))
    println("Generated " + name)
    file
  }

  val generated = Seq(
    generatePage("index.html", html.index)
  )

  val generatedSet = generated.toSet

  def cleanOldFiles(file: File): Unit = {
    if (file.isDirectory) {
      file.listFiles().foreach(cleanOldFiles)
      if (file.listFiles().isEmpty) {
        file.delete()
      }
    } else {
      if (!generatedSet.contains(file)) {
        println("Deleting " + file)
        file.delete()
      }
    }
  }

  cleanOldFiles(outputDir)

}
