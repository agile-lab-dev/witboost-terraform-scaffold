package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.TerraformVariables.noVariable
import it.agilelab.provisioners.terraform.{ ProcessResult, _ }
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ParserFactory, YamlDescriptor }
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockitoSugar.when
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito, Mockito }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.nio.file.Files

class TerraformDestroyTest extends AnyFlatSpec with should.Matchers with IdiomaticMockito with ArgumentMatchersSugar {

  val tempFolder = Files.createTempDirectory("tmp-")

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
      "terraform -chdir=folder destroy -var var1='value1' -var var2='value2'"
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

    val yamlDescriptor = """
      dataProduct:
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1
    """

    val parser          = ParserFactory.parser()
    val terraformModule = TerraformModule("doesnt-exist", Map.empty, Map.empty, "")
    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.unprovision(YamlDescriptor(yamlDescriptor).parse(parser).descriptor, true)

    res.isSuccessful shouldBe false

  }

  "Terraform" should "Destroy applied with (removeData = false, kind = outputport)" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = mock[Processor]
    when(mockProcessor.run(*)) thenReturn new ProcessResult(0, new MockProcessOutput(outputString))

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val yamlDescriptor = """
      dataProduct:
        foo: bar
        components:
          - kind: outputport
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1

    """

    val parser          = ParserFactory.parser()
    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.foo"), "key")
    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.unprovision(YamlDescriptor(yamlDescriptor).parse(parser).descriptor, false)

    res.isSuccessful shouldBe true

  }

  "Terraform" should "Destroy applied with (removeData = false, kind = workload)" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = mock[Processor]
    when(mockProcessor.run(*)) thenReturn new ProcessResult(0, new MockProcessOutput(outputString))

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val yamlDescriptor = """
      dataProduct:
        foo: bar
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1

    """

    val parser          = ParserFactory.parser()
    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.foo"), "key")
    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.unprovision(YamlDescriptor(yamlDescriptor).parse(parser).descriptor, false)

    res.isSuccessful shouldBe true

  }

  "Terraform" should "Destroy applied with (removeData = true, kind = storage)" in {
    val outputString  = "Destroy complete!"
    val mockProcessor = mock[Processor]
    when(mockProcessor.run(*)) thenReturn new ProcessResult(0, new MockProcessOutput(outputString))

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val yamlDescriptor = """
      dataProduct:
        foo: bar
        components:
          - kind: storage
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1

    """

    val parser          = ParserFactory.parser()
    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.foo"), "key")
    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.unprovision(YamlDescriptor(yamlDescriptor).parse(parser).descriptor, true)

    res.isSuccessful shouldBe true

  }

  "Terraform" should "Destroy skipped with (removeData = false, kind = storage)" in {
    val mockProcessor = mock[Processor]

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val yamlDescriptor = """
      dataProduct:
        components:
          - kind: storage
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1
    """

    val parser          = ParserFactory.parser()
    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map.empty, "")
    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.unprovision(YamlDescriptor(yamlDescriptor).parse(parser).descriptor, removeData = false)

    Mockito.verifyNoInteractions(mockProcessor.run(*))
    res.isSuccessful shouldBe true

  }

  it should "perform destroy passing the ownerPrincipals variable" in {
    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Name Surname
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |        description: Output Port for vaccinations data using Hasura
        |        name: Hasura Output Port
        |        fullyQualifiedName: Hasura Output Port
        |        dataContract:
        |          schema:
        |            - name: date
        |              dataType: DATE
        |            - name: location_key
        |              dataType: TEXT
        |              constraint: PRIMARY_KEY
        |        specific:
        |            customTableName: healthcare_vaccinations_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor
    val mockProcessor                   = new MockProcessor(0, "")
    val terraformBuilder                = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)
    val terraformModule                 =
      TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.dataProductOwnerDisplayName"), "key")
    val tfProvider                      = new TfProvider(terraformBuilder, terraformModule)

    val res = tfProvider.unprovision(descriptor, removeData = false)

    res.isSuccessful shouldBe true
    mockProcessor.command should include(s"""-var ownerPrincipals=''""")
  }

}
