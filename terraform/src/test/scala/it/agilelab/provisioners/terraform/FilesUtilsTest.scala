package it.agilelab.provisioners.terraform

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.nio.file.{ Files, Path }

class FilesUtilsTest extends AnyFlatSpec with should.Matchers {

  "FilesUtils" should "copy the directory" in {

    val src: Path = Files.createTempDirectory("test-source-")
    Files.createTempFile(src, "xyz-", "")
    val dst: Path = Files.createTempDirectory("test-dest-")

    val d = FilesUtils.copyDirectory(src.toString, dst.toString)

    d.isSuccess shouldBe (true)

  }

  "FilesUtils" should "fail to copy the directory" in {

    val src: Path = Files.createTempDirectory("test-source-")
    Files.createTempFile(src, "xyz-", "")

    val d = FilesUtils.copyDirectory(src.toString, "doesnt-exist")

    d.isSuccess shouldBe (false)

  }

  "FilesUtils" should "delete a folder recursively" in {

    val src: Path = Files.createTempDirectory("test-source-")
    Files.createTempFile(src, "xyz-", "")

    val d = FilesUtils.deleteDirectory(src)

    d.isSuccess shouldBe true

  }

}
