package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult, TerraformVariables }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformApplyTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform apply" in {
    val outputString = "Apply complete!"

    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doApply()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe outputString
    mockProcessor.command should include("terraform -chdir=folder apply")
  }

  "Terraform" should "perform apply with variables" in {
    val outputString  = "Apply complete!"
    val mockProcessor = new MockProcessor(0, outputString)

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

  "Terraform" should "perform apply and report a list of error with a length 1 by including -json option" in {
    val outputString =
      """
        |{
        |  "@level": "error",
        |  "@message": "Error: Invalid reference",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-07-27T16:08:20.229806+05:30",
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

    val firstRow =
      """
        |{
        |  "@level": "info",
        |  "@message": "Apply complete! Resources: 0 added, 0 changed, 0 destroyed.",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-07-27T16:04:39.684638+05:30",
        |  "changes": {
        |    "add": 0,
        |    "change": 0,
        |    "import": 0,
        |    "remove": 0,
        |    "operation": "apply"
        |  },
        |  "type": "change_summary"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(1, firstRow.concat("\n").concat(outputString))

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    val result: TerraformResult = terraform.doApply()

    result.isSuccess shouldBe false
    result.errorMessages.size shouldBe 1
  }

  "Terraform" should "perform apply with -json option" in {
    val outputString  =
      """
        |{
        |  "@level": "info",
        |  "@message": "Apply complete! Resources: 0 added, 0 changed, 0 destroyed.",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-07-27T16:04:39.684638+05:30",
        |  "changes": {
        |    "add": 0,
        |    "change": 0,
        |    "import": 0,
        |    "remove": 0,
        |    "operation": "apply"
        |  },
        |  "type": "change_summary"
        |}
        |""".stripMargin.replace("\n", "")
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doApply()

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform apply with no -json option" in {
    val outputString  = "Apply complete!"
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doApply()

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform apply and log output" in {
    val outputString  = "Apply complete!"
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doApply()

    mockLogger.lastLine shouldBe outputString
  }

  "Terraform" should "perform apply and log no output" in {
    val outputString  = "Apply complete!"
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
