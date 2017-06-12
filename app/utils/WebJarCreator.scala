package utils

import java.io._

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.{ArchiveOutputStream, ArchiveStreamFactory}
import org.apache.commons.compress.utils.IOUtils


object WebJarCreator {

  private def createDir(dir: String, jar: ArchiveOutputStream): Unit = {
    val ze = new ZipArchiveEntry(dir)
    jar.putArchiveEntry(ze)
    jar.closeArchiveEntry()
  }

  private def createFileEntry(path: String, jar: ArchiveOutputStream, contents: String): Unit = {
    val ze = new ZipArchiveEntry(path)
    jar.putArchiveEntry(ze)
    jar.write(contents.getBytes)
    jar.closeArchiveEntry()
  }


  def createWebJar(in: InputStream, contentsInSubdir: Boolean, exclude: Set[String], pom: String, groupId: String, name: String, version: String): Array[Byte] = {

    val byteArrayOutputStream = new ByteArrayOutputStream()

    val bufferedByteArrayOutputStream = new BufferedOutputStream(byteArrayOutputStream)

    val jar = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.JAR, bufferedByteArrayOutputStream)

    val bufferedInputStream = new BufferedInputStream(in)

    val archive = new ArchiveStreamFactory().createArchiveInputStream(bufferedInputStream)

    createDir(s"META-INF/", jar)
    createDir(s"META-INF/maven/", jar)
    createDir(s"META-INF/maven/$groupId/", jar)
    createDir(s"META-INF/maven/$groupId/$name/", jar)

    createFileEntry(s"META-INF/maven/$groupId/$name/pom.xml", jar, pom)

    val properties = s"""
        |#Generated by WebJar Sync
        |version=$version
        |groupId=$groupId
        |artifactId=$name
       """.stripMargin

    createFileEntry(s"META-INF/maven/$groupId/$name/pom.properties", jar, properties)

    val webJarPrefix = s"META-INF/resources/webjars/$name/$version/"

    createDir(s"META-INF/resources/", jar)
    createDir(s"META-INF/resources/webjars/", jar)
    createDir(s"META-INF/resources/webjars/$name/", jar)
    createDir(webJarPrefix, jar)

    // copy zip to jar
    Stream.continually(archive.getNextEntry).takeWhile(_ != null).foreach { ze =>
      val name = if (contentsInSubdir) {
        val baseName = ze.getName.split("/").tail.mkString("/")
        if (ze.isDirectory) {
          baseName + "/"
        }
        else {
          baseName
        }
      } else {
        ze.getName
      }

      if (!exclude.exists(name.startsWith)) {
        val path = webJarPrefix + name
        val nze = new ZipArchiveEntry(path)
        jar.putArchiveEntry(nze)
        if (!ze.isDirectory) {
          IOUtils.copy(archive, jar)
        }
        jar.closeArchiveEntry()
      }
    }

    archive.close()
    bufferedInputStream.close()
    in.close()

    jar.close()
    bufferedByteArrayOutputStream.close()
    byteArrayOutputStream.close()

    byteArrayOutputStream.toByteArray
  }

  def emptyJar(): Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream()

    val jar = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.JAR, byteArrayOutputStream)
    jar.close()

    byteArrayOutputStream.toByteArray
  }

}