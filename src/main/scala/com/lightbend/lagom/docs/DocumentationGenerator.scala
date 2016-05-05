package com.lightbend.lagom.docs

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import com.lightbend.docs.{Context, TOC}
import play.api.libs.json.Json
import play.twirl.api.{Html, Template0}

object DocumentationGenerator extends App {

  val outputDir = new File(args(0))
  val docsDir = new File(args(1))

  def generatePage(name: String, template: Template0[Html]) = {
    val rendered = template.render()
    val file = new File(outputDir, name)
    file.getParentFile.mkdirs()
    Files.write(file.toPath, rendered.body.getBytes("utf-8"))
    file
  }

  // Discover versions
  val versions = docsDir.listFiles().toSeq.map { docsVersionDir =>
    val indexJson = new File(docsVersionDir, "index.json")

    val toc = Json.parse(Files.readAllBytes(indexJson.toPath)).as[TOC]
    docsVersionDir -> Version(docsVersionDir.getName, toc)
  }.sortBy(_._1.getName) // Will need a better sort in future

  private def getNav(ctx: Context, acc: List[Section] = Nil): List[Section] = {
    ctx.parent match {
      case None => acc
      case Some(parent) =>
        val titles = parent.children map { case (t, u) => NavLink(t, u, t == ctx.title) }
        val url = parent.children.head._2
        getNav(parent, Section(parent.title, url, titles) :: acc)
    }
  }

  def renderDocVersion(version: Version): Seq[File] = {
    val versionOutputDir = new File(outputDir, "documentation/" + version.name)
    versionOutputDir.mkdirs()

    def processDocsFile(path: String, file: File): Seq[File] = {
      if (file.isDirectory) {
        new File(versionOutputDir, path).mkdirs()
        file.listFiles().flatMap { child =>
          val childPath = if (path.isEmpty) child.getName else path + "/" + child.getName
          processDocsFile(childPath, child)
        }
      } else {
        val targetFile = new File(versionOutputDir, path)
        version.toc.mappings.get(path) match {
          case Some(context) if !context.nostyle =>
            val fileContent = Html(new String(Files.readAllBytes(file.toPath), "utf-8"))
            val versionPages = versions.map(_._2.pageFor(path))
            val nav = getNav(context)
            val rendered = html.documentation(path, fileContent, context, version.name, versionPages, nav)
            Files.write(targetFile.toPath, rendered.body.getBytes("utf-8"))
          case _ =>
            // Simply copy the file as is
            if (targetFile.lastModified() < file.lastModified()) {
              Files.copy(file.toPath, targetFile.toPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        Seq(targetFile)
      }
    }

    processDocsFile("", new File(docsDir, version.name))
  }

  val generatedDocs = versions.map(_._2).flatMap(renderDocVersion)

  val generated = Seq(
    generatePage("index.html", html.index),
    generatePage("get-involved.html", html.getinvolved)
  )

  val generatedSet = generated.toSet ++ generatedDocs

  def cleanOldFiles(file: File): Unit = {
    if (file.isDirectory) {
      file.listFiles().foreach(cleanOldFiles)
      if (file.listFiles().isEmpty) {
        file.delete()
      }
    } else {
      if (!generatedSet.contains(file)) {
        file.delete()
      }
    }
  }

  cleanOldFiles(outputDir)

}

/**
  * A version of the docs.
  *
  * @param name The name of the version.
  * @param toc The table of contents for the version.
  */
case class Version(name: String, toc: TOC) {
  def pageFor(path: String) = VersionPage(name, toc.mappings.get(path).isDefined)
}

/**
  * A link to another version from a specific page. Used when rendering the drop down to another version, if the page
  * exists in the other version we render a link to the page, otherwise, we render a link to the index.
  *
  * @param name The name of the version.
  * @param exists Whether the specific page exists in that version.
  */
case class VersionPage(name: String, exists: Boolean)

/**
  * A link to a page
  */
case class NavLink(title: String, url: String, current: Boolean)

/**
  * A documentation section
  */
case class Section(title: String, url: String, children: Seq[NavLink])