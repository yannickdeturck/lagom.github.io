package com.lightbend.lagom.docs

import java.io.File
import java.net.URL
import java.nio.file.{Files, StandardCopyOption}

import com.lightbend.docs.{Context, TOC}
import org.pegdown.{Extensions, PegDownProcessor}
import play.api.libs.json.{Json, Reads}
import play.twirl.api.{Html, HtmlFormat, Template1}

import scala.collection.JavaConverters._

object DocumentationGenerator extends App {

  /*
   * CONFIGURATION
   */
  // Current documentation version
  val currentDocsVersion = "1.0.x"
  val currentLagomVersion = "1.0.0-M2"

  //val baseUrl = "http://jroper.github.io/lagom.github.io"
  //val context = "/lagom.github.io"
  val baseUrl = "http://www.lagomframework.com"
  val context = ""


  // Templated pages to generate
  val templatePages: Seq[(String, Template1[LagomContext, Html])] = Seq(
    "index.html" -> html.index,
    "get-involved.html" -> html.getinvolved,
    "download.html" -> html.download
  )

  // Redirects
  val redirects: Seq[(String, String)] = Seq(
    "/documentation/index.html" -> s"$context/documentation/$currentDocsVersion/Home.html"
  )

  val activatorRelease = {
    val stream = new URL("https://www.lightbend.com/activator/latest").openStream()
    try {
      Json.parse(stream).as[ActivatorRelease]
    } finally {
      stream.close()
    }
  }

  implicit val lagomContext = LagomContext(context, currentLagomVersion, currentDocsVersion, activatorRelease)

  val outputDir = new File(args(0))
  val docsDir = new File(args(1))
  val markdownDir = new File(args(2))

  def generatePage(name: String, template: Template1[LagomContext, Html]): OutputFile = {
    savePage(name, template.render(lagomContext))
  }

  def generateRedirect(from: String, to: String): OutputFile = {
    savePage(from, html.redirect(to), includeInSitemap = false)
  }

  def savePage(name: String, rendered: Html, includeInSitemap: Boolean = true): OutputFile = {
    val file = new File(outputDir, name)
    file.getParentFile.mkdirs()
    Files.write(file.toPath, rendered.body.getBytes("utf-8"))
    val sitemapUrl = name match {
      case "index.html" => ""
      case index if index.endsWith("/index.html") => index.stripSuffix("/index.html")
      case other => other
    }
    OutputFile(file, sitemapUrl, includeInSitemap)
  }

  val pegdown = new PegDownProcessor(Extensions.ALL)

  def renderMarkdownFiles(path: String, file: File): Seq[OutputFile] = {
    if (file.isDirectory) {
      new File(outputDir, path).mkdirs()
      file.listFiles().flatMap { child =>
        val childPath = if (path.isEmpty) child.getName else path + "/" + child.getName
        renderMarkdownFiles(childPath, child)
      }
    } else {
      val lines = Files.readAllLines(file.toPath).asScala.toList
      lines.dropWhile(!_.startsWith("#")) match {
        case title :: rest =>
          val strippedTitle = title.dropWhile(c => c == '#' || c == ' ')
          val rendered = pegdown.markdownToHtml(rest.mkString("\n"))
          val page = html.markdown(strippedTitle, Html(rendered))
          Seq(savePage(path.replaceAll("\\.md$", ".html"), page))
        case Nil => throw new IllegalArgumentException("Markdown files must start with a heading using the # syntax")
      }
    }
  }

  // Discover versions
  val versions = docsDir.listFiles().toSeq.map { docsVersionDir =>
    val indexJson = new File(docsVersionDir, "index.json")

    val toc = Json.parse(Files.readAllBytes(indexJson.toPath)).as[TOC]
    docsVersionDir -> Version(docsVersionDir.getName, toc)
  }.sortBy(_._1.getName) // Will need a better sort in future

  val currentVersion = versions.find(_._2.name == currentDocsVersion)

  private def getNav(ctx: Context, acc: List[Section] = Nil): List[Section] = {
    ctx.parent match {
      case None => acc
      case Some(parent) =>
        val titles = parent.children map { case (t, u) => NavLink(t, u, t == ctx.title) }
        val url = parent.children.head._2
        getNav(parent, Section(parent.title, url, titles) :: acc)
    }
  }

  def renderDocVersion(version: Version): Seq[OutputFile] = {
    val docsPath = "documentation/" + version.name
    val versionOutputDir = new File(outputDir, docsPath)
    versionOutputDir.mkdirs()

    def processDocsFile(path: String, file: File): Seq[OutputFile] = {
      if (file.isDirectory) {
        new File(versionOutputDir, path).mkdirs()
        file.listFiles().flatMap { child =>
          val childPath = if (path.isEmpty) child.getName else path + "/" + child.getName
          processDocsFile(childPath, child)
        }
      } else {
        val targetFile = new File(versionOutputDir, path)
        val rendered = version.toc.mappings.get(path) match {
          case Some(context) if !context.nostyle =>
            val fileContent = Html(new String(Files.readAllBytes(file.toPath), "utf-8"))

            val versionPages = versions.map(_._2.pageFor(path))
            val nav = getNav(context)
            val canonical = currentVersion.map(_._2.pageFor(path)).collect {
              case VersionPage(name, true) => s"$baseUrl/documentation/$name/$path"
            }

            val rendered = html.documentation(path, fileContent, context, version.name, versionPages, nav, canonical)

            Files.write(targetFile.toPath, rendered.body.getBytes("utf-8"))
            OutputFile(targetFile, docsPath + "/" + path, true)
          case _ =>
            // Simply copy the file as is
            if (targetFile.lastModified() < file.lastModified()) {
              Files.copy(file.toPath, targetFile.toPath, StandardCopyOption.REPLACE_EXISTING)
            }
            OutputFile(targetFile, docsPath + "/" + path, false)
        }
        Seq(rendered)
      }
    }

    processDocsFile("", new File(docsDir, version.name))
  }

  val generatedDocs = versions.map(_._2).map(version => version -> renderDocVersion(version))

  val generated = templatePages.map((generatePage _).tupled) ++ renderMarkdownFiles("", markdownDir)

  // sitemaps
  val mainSitemap = Sitemap("sitemap-main.xml", generated.filter(_.includeInSitemap)
    .map(file => SitemapUrl(file.sitemapUrl, "1.0")))

  val docsSitemap = Sitemap("sitemap-docs.xml", generatedDocs.find(_._1.name == currentDocsVersion).map {
    case (_, pages) =>
      pages.collect {
        case OutputFile(_, path, true) => SitemapUrl(path, "0.9")
      }
  }.getOrElse(Nil))

  val generatedSitemaps = Sitemap.generateSitemaps(outputDir, baseUrl, Seq(mainSitemap, docsSitemap))

  val generatedSet: Set[File] = generated.map(_.file).toSet ++ generatedSitemaps ++ generatedDocs.flatMap(_._2.map(_.file))

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

case class OutputFile(file: File, sitemapUrl: String, includeInSitemap: Boolean)

/**
  * The context that gets passed to every page in the documentation.
  *
  * @param currentLagomVersion The current version of Lagom.
  * @param currentDocsVersion The current version of the docs.
  * @param activatorRelease The current version of Activator.
  */
case class LagomContext(path: String, currentLagomVersion: String, currentDocsVersion: String, activatorRelease: ActivatorRelease)

case class ActivatorRelease(url: String, miniUrl: String, version: String, size: String, miniSize: String)

object ActivatorRelease {
  implicit val reads: Reads[ActivatorRelease] = Json.reads[ActivatorRelease]
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
