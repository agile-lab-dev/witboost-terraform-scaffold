package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformInitTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform init" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doInit()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe "output"
    mockProcessor.command should include("terraform -chdir=folder init")
  }

  "Terraform" should "perform init and report an error" in {
    val mockProcessor = new MockProcessor(1, "error")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doInit()

    result.isSuccess shouldBe false
    result.buildOutputString shouldBe "error"
  }

  "Terraform" should "perform init with no -json (not available option for this command)" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doInit()

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform init with no -json again (not available option for this command)" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doInit()

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform init and log output" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doInit()

    mockLogger.lastLine shouldBe "some output string"
  }

  "Terraform" should "perform init and log no ouptut" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
      .onDirectory("folder")

    terraform.doInit()

    mockLogger.lastLine shouldBe empty
  }

}
