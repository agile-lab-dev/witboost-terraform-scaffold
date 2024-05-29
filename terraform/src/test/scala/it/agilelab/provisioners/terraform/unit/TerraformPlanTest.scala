package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult, TerraformVariables }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformPlanTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform plan" in {

    val outputString  = "Plan: 1 to add, 0 to change, 0 to destroy."
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doPlan()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe outputString
    mockProcessor.command should include("terraform -chdir=folder plan")
  }

  "Terraform" should "perform plan with variables" in {
    val outputString  = "Plan: 1 to add, 0 to change, 0 to destroy."
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    terraform.doPlan(
      TerraformVariables.variables(
        "var1" -> "value1",
        "var2" -> "value2"
      )
    )

    mockProcessor.command should include("terraform -chdir=folder plan -var var1='value1' -var var2='value2'")
  }

  "Terraform" should "perform plan and report an error" in {
    val mockProcessor = new MockProcessor(1, "error")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doPlan()

    result.isSuccess shouldBe false
    result.buildOutputString shouldBe "error"
  }

  "Terraform" should "perform plan and report an error with -json option" in {
    val outputString =
      """
        |{
        |  "@level": "error",
        |  "@message": "Error: Invalid reference",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-07-27T15:28:41.002415+05:30",
        |  "diagnostic": {
        |    "severity": "error",
        |    "summary": "Invalid reference",
        |    "detail": "A reference to a resource type must be followed by at least one attribute access, specifying the resource name.",
        |    "range": {
        |      "filename": "main.tf",
        |      "start": {
        |        "line": 3,
        |        "column": 22,
        |        "byte": 89
        |      },
        |      "end": {
        |        "line": 3,
        |        "column": 25,
        |        "byte": 92
        |      }
        |    },
        |    "snippet": {
        |      "context": "resource \"random_string\" \"random\"",
        |      "code": "  special          = tru",
        |      "start_line": 3,
        |      "highlight_start_offset": 21,
        |      "highlight_end_offset": 24,
        |      "values": []
        |    }
        |  },
        |  "type": "diagnostic"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(1, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    val result: TerraformResult = terraform.doPlan()

    result.isSuccess shouldBe false
    result.errorMessages.size should be > 0
  }

  "Terraform" should "perform plan with -json option" in {
    val outputString  = "Plan: 1 to add, 0 to change, 0 to destroy."
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doPlan()

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform plan with no -json option" in {
    val outputString  = "Plan: 1 to add, 0 to change, 0 to destroy."
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doPlan()

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform plan and log output" in {
    val outputString  = "Plan: 1 to add, 0 to change, 0 to destroy."
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doPlan()

    mockLogger.lastLine shouldBe outputString
  }

  "Terraform" should "perform plan and log no output" in {
    val outputString  = "Plan: 1 to add, 0 to change, 0 to destroy."
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
      .onDirectory("folder")

    terraform.doApply()

    mockLogger.lastLine shouldBe empty
  }

}
