package it.agilelab.provisioners.terraform

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{ FileVisitResult, Files, Path, Paths, SimpleFileVisitor }
import scala.util.{ Failure, Success, Try }

object FilesUtils {

  /** This utility creates a mirror of the terraform configuration, in a separate folder.
    * This permits to avoid problems about clashing files (i.e. tf state) when multiple components of the same useCaseTemplate are managed
    * @param inputPath is the terraform configuration folder
    * @return if successful, the new context folder
    */
  def createTfContext(inputPath: String): Try[String] =
    for {
      tempDir  <- Try(Files.createTempDirectory("tf-"))
      destPath <- copyDirectory(inputPath, tempDir.toString)
    } yield (destPath)

  protected[terraform] def copyDirectory(
    sourceDirectoryLocation: String,
    destinationDirectoryLocation: String
  ): Try[String] = {

    val sourceDirectoryLocationPath      = Paths.get(sourceDirectoryLocation)
    val destinationDirectoryLocationPath =
      Paths.get(destinationDirectoryLocation, sourceDirectoryLocationPath.getFileName.toString)

    Try {

      Files.walk(Paths.get(sourceDirectoryLocation)).forEach { (source: Path) =>
        def copy(source: Path) = {
          val destination = Paths
            .get(destinationDirectoryLocationPath.toString, source.toString.substring(sourceDirectoryLocation.length))
          Files.copy(source, destination)
        }
        copy(source)

      }
    } match {
      case Failure(e) => Failure(e)
      case Success(_) => Success(destinationDirectoryLocationPath.toString)
    }

  }

  /** this method is used to recursively delete the folder and its content
    * @param root is the path of the directory
    * @return if successful, the starting directory
    */
  def deleteDirectory(root: Path): Try[String] =
    Try {
      Files.walkFileTree(
        root,
        new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.delete(file)
            FileVisitResult.CONTINUE
          }
          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          }
        }
      )
    } match {
      case Failure(e) => Failure(e)
      case Success(s) => Success(s.toString)
    }

}
