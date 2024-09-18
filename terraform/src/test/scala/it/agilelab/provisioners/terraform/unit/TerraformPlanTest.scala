package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{
  ProcessResult,
  Processor,
  Terraform,
  TerraformModule,
  TerraformResult,
  TerraformVariables
}
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ParserFactory, YamlDescriptor }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.nio.file.Files

class TerraformPlanTest extends AnyFlatSpec with should.Matchers {

  val tempFolder            = Files.createTempDirectory("tmp-")
  val outputString5destroys =
    "{\"@level\":\"info\",\"@message\":\"Plan: 2 to import, 0 to add, 1 to change, 5 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":2,\"change\":0,\"import\":1,\"remove\":5,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
  val outputString2Adds     =
    "{\"@level\":\"info\",\"@message\":\"Plan: 0 to import, 2 to add, 0 to change, 0 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":2,\"change\":0,\"import\":0,\"remove\":0,\"operation\":\"plan\"},\"type\":\"change_summary\"}"

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

  "Terraform" should "perform plan but block for resources to destroy and skipSafetyChecks = false" in {

    class CustomMockProcessor extends Processor {
      private val buffer                               = new StringBuffer()
      override def run(command: String): ProcessResult =
        if (command.contains("init"))
          new ProcessResult(0, new MockProcessOutput(""))
        else if (command.contains("plan"))
          new ProcessResult(0, new MockProcessOutput(outputString5destroys))
        else
          new ProcessResult(1, new MockProcessOutput(""))
    }

    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Name Surname
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |        specific:
        |            customTableName: healthcare_vaccinations_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |            reverse:
        |               imports: []
        |               skipSafetyChecks: false
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new CustomMockProcessor

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val terraformModule =
      TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.dataProductOwnerDisplayName"), "key")

    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.validate(descriptor = descriptor, mappedOwners = Set.empty)

    res.isSuccessful shouldBe false
    res.errors.sizeIs > 0
    res.errors.last.description should include(
      "The plan is proposing to destroy 5 resources, but the skipSafetyChecks is disabled."
    )

  }

  "Terraform" should "perform plan but proceed for resources to destroy and skipSafetyChecks = true" in {

    class CustomMockProcessor extends Processor {
      private val buffer                               = new StringBuffer()
      override def run(command: String): ProcessResult =
        if (command.contains("init"))
          new ProcessResult(0, new MockProcessOutput(""))
        else if (command.contains("plan"))
          new ProcessResult(0, new MockProcessOutput(outputString5destroys))
        else
          new ProcessResult(1, new MockProcessOutput(""))
    }

    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Name Surname
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |        specific:
        |            customTableName: healthcare_vaccinations_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |            reverse:
        |               imports: []
        |               skipSafetyChecks: true
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new CustomMockProcessor

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val terraformModule =
      TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.dataProductOwnerDisplayName"), "key")

    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.validate(descriptor = descriptor, Set.empty)

    res.isSuccessful shouldBe true

  }

  "Terraform" should "perform plan passing the ownerPrincipals variable" in {

    class CustomMockProcessor extends Processor {
      private val buffer  = new StringBuffer()
      def command: String = buffer.toString
      override def run(command: String): ProcessResult = {
        buffer.append(command)
        if (command.contains("init"))
          new ProcessResult(0, new MockProcessOutput(""))
        else if (command.contains("plan"))
          new ProcessResult(0, new MockProcessOutput(outputString2Adds))
        else
          new ProcessResult(1, new MockProcessOutput(""))
      }
    }

    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Name Surname
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |        specific:
        |            customTableName: healthcare_vaccinations_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new CustomMockProcessor

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val terraformModule =
      TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.dataProductOwnerDisplayName"), "key")

    val mappedOwners    = Set("user.name@email.com", "dev")

    val tfProvider = new TfProvider(terraformBuilder, terraformModule)
    val res        = tfProvider.validate(descriptor = descriptor, mappedOwners)

    res.isSuccessful shouldBe true
    mockProcessor.command should include(s"""-var ownerPrincipals='user.name@email.com,dev'""")

  }
}
