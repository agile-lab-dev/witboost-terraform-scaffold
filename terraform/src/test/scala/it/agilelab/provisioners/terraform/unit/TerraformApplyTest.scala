package it.agilelab.provisioners.terraform.unit

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.TerraformLogger.noLog
import it.agilelab.provisioners.terraform.{ Terraform, TerraformModule, TerraformResult, TerraformVariables }
import it.agilelab.spinframework.app.api.mapping.ProvisioningInfoMapper.InnerJson
import it.agilelab.spinframework.app.features.compiler.{
  ComponentDescriptor,
  ImportBlock,
  ParserFactory,
  ReverseChanges,
  YamlDescriptor
}
import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.io.File
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
      s"""terraform -chdir=${tempFolder} apply -var var1='value1' -var var2='value2'"""
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
        |    dataProductOwnerDisplayName: Jhon Doe
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
    val res             = tfProvider.provision(descriptor, Set.empty)

    res.isSuccessful shouldBe true
    res.outputs.size shouldBe 1
    res.outputs.head.name shouldEqual "fiz"
    res.outputs.head.value.asString.get shouldEqual "biz"

  }

  "Terraform" should "perform apply and return complex outputs" in {

    val outputString =
      """
        |{
        |  "@level":"info",
        |  "@message":"Outputs: 2",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2023-09-04T14:19:04.774029+02:00",
        |  "outputs":{
        |      "public_info":{"sensitive":false,"type":["object",{"aString":["object",{"label":"string","type":"string","value":"string"}]}],"value":{"aString":{"label":"Storage Account Name","type":"string","value":"/subscription/halable/foo"}}},
        |      "storage_account_id":{"sensitive":false,"type":"string","value":"/subscriptions/12345678-1234-1234-bbcc-000000111111/resourceGroups/myrg/providers/Microsoft.Storage/storageAccounts/test"}
        |  },
        |  "type":"outputs"
        |}
        |""".stripMargin.replace("\n", "")

    val parser                          = ParserFactory.parser()
    val descriptor: ComponentDescriptor = YamlDescriptor(
      """
        |dataProduct:
        |    dataProductOwnerDisplayName: Jhon Doe
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
    val res             = tfProvider.provision(descriptor, Set.empty)

    res.outputs.size shouldBe 2
    val pi   = res.outputs.head
    val said = res.outputs.last

    pi.name shouldBe "public_info"
    implicit val innerJsonDecoder: Decoder[InnerJson] = deriveDecoder[InnerJson]
    pi.value.as[Map[String, InnerJson]].isRight shouldBe true
    said.name shouldBe "storage_account_id"
    said.value.asString.get should include("myrg/providers/Microsoft.Storage/storageAccounts/test")

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
        |    dataProductOwnerDisplayName: Jhon Doe
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
    val res        = tfProvider.provision(descriptor, Set.empty)

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
        |    dataProductOwnerDisplayName: Jhon Doe
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
    val res        = tfProvider.provision(descriptor, Set.empty)

    res.isSuccessful shouldBe false

  }

  it should "perform apply passing the ownerPrincipals variable" in {
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
    val mappedOwners                    = Set("user.name@email.com", "dev")

    val res = tfProvider.provision(descriptor, mappedOwners)

    res.isSuccessful shouldBe true
    mockProcessor.command should include(s"""-var ownerPrincipals='user.name@email.com,dev'""")
  }

  it should "successfully retrieve imports from descriptor" in {

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
        |               imports:
        |                 - id: foo
        |                   to: bar
        |                 - id: fiz
        |                   to: biz
        |               skipSafetyChecks: false
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor

    val res = ReverseChanges.reverseChangesFromDescriptor(descriptor)
    res.value.imports.size shouldBe 2
    res.value.imports.shouldBe(List(ImportBlock(id = "foo", to = "bar"), ImportBlock(id = "fiz", to = "biz")))
  }

  it should "successfully retrieve 0 imports" in {

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
        |             imports: []
        |             skipSafetyChecks: false
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor
    val res                             = ReverseChanges.reverseChangesFromDescriptor(descriptor)

    res.value.imports.size shouldBe 0

  }

  it should "fail to retrieve imports" in {

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
        |               imports:
        |                 - id : foo
        |                   wrong: bar
        |                 - id : fiz
        |                   to: biz
        |               skipSafetyChecks: false
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations:0:hasura-output-port
        |
        |""".stripMargin
    ).parse(parser).descriptor
    val res                             = ReverseChanges.reverseChangesFromDescriptor(descriptor)

    res.isLeft shouldBe true

  }

  "Terraform" should "perform apply with imports" in {

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
        |    dataProductOwnerDisplayName: Jhon Doe
        |    intField: 33
        |    doubleField: 33.9
        |    components:
        |      - kind: outputport
        |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
        |        specific:
        |            customTableName: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
        |            resourceGroup: healthcare_rg
        |            imports:
        |             - id : foo
        |               to: bar
        |             - id : fiz
        |               to: biz
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
    val res             = tfProvider.provision(descriptor, Set.empty)

    res.outputs.size shouldBe 1
    res.outputs.head.name shouldEqual "fiz"
    res.outputs.head.value.asString.get shouldEqual "biz"
    res.isSuccessful shouldBe true

  }

}
