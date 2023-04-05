package it.agilelab.provisioners.terraform.local

import it.agilelab.provisioners.terraform.TerraformResult
import org.scalatest.Assertion
import org.scalatest.matchers.should

import java.io.File
import java.nio.file.Files

trait TerraformLocalTestBase extends should.Matchers {

  private val resources = "terraform/src/test/resources/terraform"

  def folder(folderName: String) = s"$resources$folderName"

  def file(fileName: String): File = new File(s"${folder("/local-file")}/$fileName")

  def shouldNotExist(someFile: File): Assertion =
    Files.exists(someFile.toPath) shouldBe false

  def shouldExist(file: File, content: String): Assertion = {
    Files.exists(file.toPath) shouldBe true
    Files.readAllLines(file.toPath).get(0) shouldBe content
  }

  def shouldBeSuccess(result: TerraformResult, outputFragments: String*): Unit = {
    result.isSuccess shouldBe true
    val output = result.buildOutputString
    output should not be empty
    outputFragments.foreach(fragment => output should include(fragment))

    // When an error occurs, Terraform returns an output with an "Error: <some short description>"
    output should not include "Error:"
  }
}
