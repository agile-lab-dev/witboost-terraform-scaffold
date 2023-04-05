package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult, TerraformVariables }
import it.agilelab.provisioners.terraform.TerraformVariables.noVariable
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformDestroyTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform destroy" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doDestroy(noVariable())

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe "output"
    mockProcessor.command should include("terraform -chdir=folder destroy")
  }

  "Terraform" should "perform destroy with variables" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    terraform.doDestroy(
      TerraformVariables.variables(
        "var1" -> "value1",
        "var2" -> "value2"
      )
    )

    mockProcessor.command should include(
      "terraform -chdir=folder destroy -var var1=\"value1\" -var var2=\"value2\""
    )
  }

  "Terraform" should "perform destroy and report an error" in {
    val mockProcessor = new MockProcessor(1, "error")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doDestroy(noVariable())

    result.isSuccess shouldBe false
    result.buildOutputString shouldBe "error"
  }

  "Terraform" should "perform destroy with -json option" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform destroy with no -json option" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform destroy and log output" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockLogger.lastLine shouldBe "some output string"
  }

  "Terraform" should "perform destroy and log no ouptut" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockLogger.lastLine shouldBe empty
  }

}
