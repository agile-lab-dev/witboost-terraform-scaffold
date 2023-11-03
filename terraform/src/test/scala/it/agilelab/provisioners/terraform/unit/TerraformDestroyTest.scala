package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformModule, TerraformResult, TerraformVariables }
import it.agilelab.provisioners.terraform.TerraformVariables.noVariable
import it.agilelab.spinframework.app.features.compiler.{ ParserFactory, YamlDescriptor }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformDestroyTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "perform destroy" in {

    val outputString  = "Destroy complete!"
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result: TerraformResult = terraform.doDestroy(noVariable())

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe outputString
    mockProcessor.command should include("terraform -chdir=folder destroy")
  }

  "Terraform" should "perform destroy with variables" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = new MockProcessor(0, outputString)

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

    val outputString  =
      """
        |{
        |  "@level": "error",
        |  "@message": "Error: Invalid reference",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-07-27T15:50:26.629471+05:30",
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

    val result: TerraformResult = terraform.doDestroy(noVariable())

    result.isSuccess shouldBe false
    result.errorMessages.size should be > 0
  }

  "Terraform" should "perform destroy with -json option" in {
    val outputString  =
      """
        |{
        |  "@level": "info",
        |  "@message": "Destroy complete! Resources: 0 destroyed.",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-07-27T15:56:01.386966+05:30",
        |  "changes": {
        |    "add": 0,
        |    "change": 0,
        |    "import": 0,
        |    "remove": 0,
        |    "operation": "destroy"
        |  },
        |  "type": "change_summary"
        |}
        |""".stripMargin.replace("\n", "")
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInJson()
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform destroy with no -json option" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockProcessor.command should not include "-json"
  }

  "Terraform" should "perform destroy and log output" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(mockLogger)
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockLogger.lastLine shouldBe outputString
  }

  "Terraform" should "perform destroy and log no output" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraform = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
      .onDirectory("folder")

    terraform.doDestroy(noVariable())

    mockLogger.lastLine shouldBe empty
  }

  "Terraform" should "fail to destroy because of non existing folder" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = new MockProcessor(0, outputString)
    val mockLogger    = new MockLogger

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val parser          = ParserFactory.parser()
    val terraformModule = TerraformModule("doesnt-exist", Map.empty, Map.empty, "")
    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.unprovision(YamlDescriptor("").parse(parser).descriptor)

    res.isSuccessful shouldBe false

  }

}
