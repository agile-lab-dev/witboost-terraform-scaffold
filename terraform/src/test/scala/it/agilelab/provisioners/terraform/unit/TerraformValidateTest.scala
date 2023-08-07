package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformResult }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformValidateTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform validate" in {

    val outputString  = "Success! The configuration is valid."
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doValidate()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe outputString
    mockProcessor.command should include("terraform -chdir=folder validate")
  }

  "Terraform" should "perform validate when -json is provided" in {
    val outputString =
      """
        |{
        |format_version: 1.0,
        |valid: true,
        |error_count: 0,
        |warning_count: 0,
        |diagnostics: []
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    val result: TerraformResult = terraform.doValidate()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe outputString
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

  "Terraform" should "perform validate and report an error when -json option is provided" in {

    val outputString =
      """
        |{
        |  "format_version": "1.0",
        |  "valid": false,
        |  "error_count": 1,
        |  "warning_count": 0,
        |  "diagnostics": [
        |    {
        |      "severity": "error",
        |      "summary": "Invalid reference",
        |      "detail": "A reference to a resource type must be followed by at least one attribute access, specifying the resource name.",
        |      "range": {
        |        "filename": "main.tf",
        |        "start": {
        |          "line": 3,
        |          "column": 22,
        |          "byte": 89
        |        },
        |        "end": {
        |          "line": 3,
        |          "column": 25,
        |          "byte": 92
        |        }
        |      },
        |      "snippet": {
        |        "context": "resource \"random_string\" \"random\"",
        |        "code": "  special          = tru",
        |        "start_line": 3,
        |        "highlight_start_offset": 21,
        |        "highlight_end_offset": 24,
        |        "values": []
        |      }
        |    }
        |  ]
        |}
        |""".stripMargin.replace("\n", "")

    val firstRow =
      """
        |{
        |format_version: 1.0,
        |valid: true,
        |error_count: 0,
        |warning_count: 0,
        |diagnostics: []
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(1, firstRow.concat("\n").concat(outputString))

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    val result: TerraformResult = terraform.doValidate()

    result.isSuccess shouldBe false
    result.errorMessages.size should be > 0
  }

  "Terraform" should "perform validate with -json option" in {
    val outputString =
      """
        |{
        |format_version: 1.0,
        |valid: true,
        |error_count: 0,
        |warning_count: 0,
        |diagnostics: []
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doValidate()

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform validate with no -json option" in {
    val outputString  = "Success! The configuration is valid."
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doValidate()

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform validate and log output" in {
    val outputString  = "Success! The configuration is valid."
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doValidate()

    mockLogger.lastLine shouldBe outputString
  }

  "Terraform" should "perform validate and log no output" in {
    val outputString  = "Success! The configuration is valid."
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
      .onDirectory("folder")

    terraform.doValidate()

    mockLogger.lastLine shouldBe empty
  }

}
