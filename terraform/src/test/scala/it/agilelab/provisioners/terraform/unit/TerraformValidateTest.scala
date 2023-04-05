package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformValidateTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform validate" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doValidate()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe "output"
    mockProcessor.command should include("terraform -chdir=folder validate")
  }

  "Terraform" should "perform validate and report an error" in {
    val mockProcessor = new MockProcessor(1, "error")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doValidate()

    result.isSuccess shouldBe false
    result.buildOutputString shouldBe "error"
  }

  "Terraform" should "perform validate with -json option" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doValidate()

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform validate with no -json option" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doValidate()

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform validate and log output" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doValidate()

    mockLogger.lastLine shouldBe "some output string"
  }

  "Terraform" should "perform validate and log no ouptut" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
      .onDirectory("folder")

    terraform.doValidate()

    mockLogger.lastLine shouldBe empty
  }

}
