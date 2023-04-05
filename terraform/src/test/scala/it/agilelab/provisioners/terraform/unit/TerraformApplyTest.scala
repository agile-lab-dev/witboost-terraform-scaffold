package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult, TerraformVariables }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformApplyTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform apply" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doApply()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe "output"
    mockProcessor.command should include("terraform -chdir=folder apply")
  }

  "Terraform" should "perform apply with variables" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    terraform.doApply(
      TerraformVariables.variables(
        "var1" -> "value1",
        "var2" -> "value2"
      )
    )

    mockProcessor.command should include("terraform -chdir=folder apply -var var1=\"value1\" -var var2=\"value2\"")
  }

  "Terraform" should "perform apply and report an error" in {
    val mockProcessor = new MockProcessor(1, "error")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doApply()

    result.isSuccess shouldBe false
    result.buildOutputString shouldBe "error"
  }

  "Terraform" should "perform apply with -json option" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doApply()

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform apply with no -json option" in {
    val mockProcessor = new MockProcessor(0, "output")

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doApply()

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform apply and log output" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doApply()

    mockLogger.lastLine shouldBe "some output string"
  }

  "Terraform" should "perform apply and log no ouptut" in {
    val mockProcessor = new MockProcessor(0, "some output string")
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
      .onDirectory("folder")

    terraform.doApply()

    mockLogger.lastLine shouldBe empty
  }

}
