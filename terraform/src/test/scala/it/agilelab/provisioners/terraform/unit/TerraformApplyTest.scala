package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformModule, TerraformResult, TerraformVariables }
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ParserFactory, YamlDescriptor }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.nio.file.Files

class TerraformApplyTest extends AnyFlatSpec with should.Matchers {

  val tempFolder = Files.createTempDirectory("tmp-")

  "Terraform" should "perform apply" in {
    val outputString = "Apply complete!"

    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory(tempFolder.toString)

    val result: TerraformResult = terraform.doApply()

    result.isSuccess shouldBe true
    result.buildOutputString shouldBe outputString
    mockProcessor.command should include(s"terraform -chdir=${tempFolder} apply")
  }

  "Terraform" should "perform apply with variables" in {
    val outputString  = "Apply complete!"
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory(tempFolder.toString)

    terraform.doApply(
      TerraformVariables.variables(
        "var1" -> "value1",
        "var2" -> "value2"
      )
    )

    mockProcessor.command should include(
      s"""terraform -chdir=${tempFolder} apply -var var1="value1" -var var2="value2""""
    )
  }

  "Terraform" should "perform apply and report an error" in {
    val mockProcessor = new MockProcessor(1, "error")

    val terraform = Terraform()
      .processor(mockProcessor)
      .onDirectory(tempFolder.toString)

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
      .onDirectory(tempFolder.toString)

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
      .onDirectory(tempFolder.toString)

    terraform.doApply()

    mockProcessor.command should include("-json")
  }

  "Terraform" should "perform apply with no -json option" in {
    val outputString  = "Apply complete!"
    val mockProcessor = new MockProcessor(0, outputString)

    val terraform = Terraform()
      .processor(mockProcessor)
      .outputInPlainText()
      .onDirectory(tempFolder.toString)

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
      .onDirectory(tempFolder.toString)

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
      .onDirectory(tempFolder.toString)

    terraform.doApply()

    mockLogger.lastLine shouldBe empty
  }

  "Terraform" should "perform apply and return non-sensitive outputs only" in {

    val outputString =
      """
        |{
        |  "@level":"info",
        |  "@message":"Outputs: 2",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2023-09-04T14:19:04.774029+02:00",
        |  "outputs":{
        |	   "foo":{
        |      "sensitive":true,
        |      "type":"string",
        |      "value":"bar"
        |    },
        |	   "fiz":{
        |        "sensitive":false,
        |        "type":"string",
        |        "value":"biz"
        |    }
        |	 },
        |  "type":"outputs"
        |}
        |""".stripMargin.replace("\n", "")

    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Nicolò Bidotti
        |    intField: 33
        |    doubleField: 33.9
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
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
        |            customTableName: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new MockProcessor(0, outputString)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val terraformModule =
      TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.dataProduct.dataProductOwnerDisplayName"), "key")

    val tfProvider      = new TfProvider(terraformBuilder, terraformModule)
    val res             = tfProvider.provision(descriptor)

    res.outputs.size shouldBe 1
    res.outputs.head.name shouldBe "fiz"
    res.outputs.head.value shouldBe "biz"

  }

  "Terraform" should "succeed but return no outputs due to output parsing failure" in {

    val outputString =
      """
        |{
        |  "@level":"info",
        |  "@message":"Outputs: 2",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2023-09-04T14:19:04.774029+02:00",
        |  "outputs":{
        |	   "foo":{
        |      "sensitive":true,
        |      "type":"string",
        |      "value":"bar"
        |    },
        |	   "fiz":{
        |        "type":"string",
        |        "value":"biz"
        |    }
        |	 },
        |  "type":"outputs"
        |}
        |""".stripMargin.replace("\n", "")

    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Nicolò Bidotti
        |    intField: 33
        |    doubleField: 33.9
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
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
        |            customTableName: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new MockProcessor(0, outputString)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map.empty, "")

    val tfProvider = new TfProvider(terraformBuilder, terraformModule)
    val res        = tfProvider.provision(descriptor)

    res.outputs.size shouldBe 0

  }

  "Terraform" should "fail for non-existing folder" in {

    val outputString =
      """
        |{
        |  "@level":"info",
        |  "@message":"Outputs: 2",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2023-09-04T14:19:04.774029+02:00",
        |  "outputs":{
        |	   "foo":{
        |      "sensitive":true,
        |      "type":"string",
        |      "value":"bar"
        |    },
        |	   "fiz":{
        |        "type":"string",
        |        "value":"biz"
        |    }
        |	 },
        |  "type":"outputs"
        |}
        |""".stripMargin.replace("\n", "")

    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Nicolò Bidotti
        |    intField: 33
        |    doubleField: 33.9
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
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
        |            customTableName: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new MockProcessor(0, outputString)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)
      .withLogger(noLog)

    val terraformModule = TerraformModule("doesnt-exist", Map.empty, Map.empty, "")

    val tfProvider = new TfProvider(terraformBuilder, terraformModule)
    val res        = tfProvider.provision(descriptor)

    res.isSuccessful shouldBe false

  }

}
