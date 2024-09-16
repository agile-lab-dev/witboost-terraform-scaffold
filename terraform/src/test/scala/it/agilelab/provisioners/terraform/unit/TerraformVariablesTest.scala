package it.agilelab.provisioners.terraform.unit

import io.circe.{ parser, Json }
import it.agilelab.provisioners.features.descriptor.TerraformOutputsDescriptor
import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.{ Terraform, TerraformModule, TerraformModuleLoader }
import it.agilelab.spinframework.app.features.compiler.circe.{ CirceParsedCatalogInfo, CirceParsedDescriptor }
import it.agilelab.spinframework.app.features.compiler.{
  ComponentDescriptor,
  Parser,
  ParserFactory,
  ParsingResult,
  YamlDescriptor
}
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus
import it.agilelab.spinframework.app.features.support.test._
import it.agilelab.spinframework.app.utils.JsonPathUtils
import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.nio.file.Path

class TerraformVariablesTest extends AnyFlatSpec with should.Matchers with FrameworkTestSupport {

  private val descriptor: ComponentDescriptor = descriptorFrom(
    """
      |dataProduct:
      |    dataProductOwnerDisplayName: Jhon Doe
      |    intField: 33
      |    doubleField: 33.9
      |    environment: development
      |    domain: healthcare
      |    kind: dataproduct
      |    domainId: urn:dmb:dmn:healthcare
      |    id: urn:dmb:dp:healthcare:vaccinations-nb:0
      |    description: DP about vaccinations
      |    devGroup: popeye
      |    ownerGroup: jhon.doe_agilelab.it
      |    dataProductOwner: user:jhon.doe_agilelab.it
      |    email: jhon.doe@gmail.com
      |    version: 0.1.0
      |    fullyQualifiedName: Vaccinations NB
      |    name: Vaccinations NB
      |    informationSLA: 2BD
      |    maturity: Tactical
      |    useCaseTemplateId: urn:dmb:utm:dataproduct-template:0.0.0
      |    infrastructureTemplateId: urn:dmb:itm:dataproduct-provisioner:1
      |    billing: {}
      |    tags: []
      |    specific: {}
      |    components:
      |      - kind: outputport
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
      |        description: Output Port for vaccinations data using Hasura
      |        name: Hasura Output Port
      |        fullyQualifiedName: Hasura Output Port
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:hasura-outputport-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:hasura-outputport-template:0.0.0
      |        dependsOn:
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-output-port
      |        platform: Hasura
      |        technology: Hasura
      |        outputPortType: GraphQL
      |        creationDate: 2023-06-12T12:52:11.737Z
      |        startDate: 2023-06-12T12:52:11.737Z
      |        dataContract:
      |          schema:
      |            - name: date
      |              dataType: DATE
      |            - name: location_key
      |              dataType: TEXT
      |              constraint: PRIMARY_KEY
      |            - name: new_persons_vaccinated
      |              dataType: NUMBER
      |            - name: new_persons_fully_vaccinated
      |              dataType: NUMBER
      |            - name: new_vaccine_doses_administered
      |              dataType: NUMBER
      |            - name: cumulative_persons_vaccinated
      |              dataType: NUMBER
      |            - name: cumulative_persons_fully_vaccinated
      |              dataType: NUMBER
      |            - name: cumulative_vaccine_doses_administered
      |              dataType: NUMBER
      |        tags: []
      |        sampleData: {}
      |        semanticLinking: []
      |        specific:
      |            complex:
      |              foo: bar
      |              fuz: buz
      |            list:
      |              - harry
      |              - potter
      |            customTableName: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
      |            select: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations
      |            selectByPk: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations_by_pk
      |            selectAggregate: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations_agg
      |            selectStream: healthcare_vaccinationsnb_0_hasuraoutputportvaccinations_stream
      |            resourceGroup: healthcare_rg
      |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
      |
      |""".stripMargin
  )

  val outputString             =
    "{\"@level\":\"info\",\"@message\":\"Plan: 2 to import, 0 to add, 1 to change, 0 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":2,\"change\":0,\"import\":1,\"remove\":0,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
  private val mockProcessor    = new MockProcessor(0, outputString)
  private val terraformBuilder = Terraform()
    .processor(mockProcessor)
  private val tfProvider       =
    new TfProvider(terraformBuilder, TerraformModuleLoader.from("urn:dmb:utm:airbyte-standard:0.0.0").getOrElse(null))

  private val mockProcessorFail    = new MockProcessor(1, "failure")
  private val terraformBuilderFail = Terraform()
    .processor(mockProcessorFail)
  private val tfProviderFail       =
    new TfProvider(
      terraformBuilderFail,
      TerraformModuleLoader.from("urn:dmb:utm:airbyte-standard:0.0.0").getOrElse(null)
    )

  "variablesFrom" should "return correct vars" in {

    val variableMappings = Some(
      Map(
        "resource_group_name" -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup",
        "some_type"           -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].dataContract.schema[1].name",
        "some_double"         -> "$.dataProduct.doubleField"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor, variableMappings)

    vars.isLeft shouldBe false
    vars.getOrElse(null).toOptions should (include("""-var some_type='location_key'""") and include(
      """-var some_double='33.9'"""
    ))

  }

  "variablesFrom" should "return correct complex vars" in {

    val variableMappings = Some(
      Map(
        "complex" -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.complex"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor, variableMappings)

    vars.isLeft shouldBe false
    vars.getOrElse(null).toOptions.replace("\n", "") should (include(
      """-var complex='{  "foo" : "bar",  "fuz" : "buz"}'"""
    ))

  }

  "variablesFrom" should "return correct var of type list" in {

    val variableMappings = Some(
      Map(
        "list" -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.list"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor, variableMappings)
    print(vars.toOption.getOrElse(null).toOptions)
    vars.isLeft shouldBe false
    vars.getOrElse(null).toOptions.replace("\n", "") should (include(
      """-var list='[ "harry", "potter" ]'"""
    ))

  }

  "variablesFrom" should "return a failure" in {

    val variableMappings = Some(
      Map(
        "resource_group_name" -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup",
        "some_type"           -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].dataContract.schema[1].name",
        "let_it_fail"         -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].dataContract.schema[1].doesnt_exist"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor, variableMappings)

    println(vars.left.getOrElse(null))

    vars.isLeft shouldBe true
    vars.left.getOrElse(null).size shouldBe 1

  }

  "variablesFrom" should "should take mappings from configs" in {

    val vars = tfProvider.variablesFrom(descriptor)

    vars.isLeft shouldBe false
    vars.getOrElse(null).toOptions should (include("""-var some_type='location_key'"""))

  }

  "variablesFrom" should "fail with jsonPath exception" in {

    val variableMappings = Some(
      Map(
        "owner_name" -> "'$.dataProduct.dataProductOwnerDisplayName'"
      )
    )
    val vars             = tfProvider.variablesFrom(descriptor, variableMappings)
    vars.isLeft shouldBe true

  }

  "variablesFrom" should "fail with generic exception" in {

    val variableMappings = Some(
      Map(
        "resource_group_name" -> ""
      )
    )
    val vars             = tfProvider.variablesFrom(descriptor, variableMappings)
    vars.isLeft shouldBe true

  }

  "provisioning" should "succeed with variables from config" in {

    val res = tfProvider.provision(descriptor, Set.empty)
    res.provisioningStatus shouldBe ProvisioningStatus.Completed

  }

  "validate" should "succeed with variables from config" in {

    val res = tfProvider.validate(descriptor)
    res.isSuccessful shouldBe true

  }

  "validate" should "fail but acl folder exists" in {

    val mockProcessorFail = new MockProcessor(1, "failure")

    val terraformBuilderFail = Terraform()
      .processor(mockProcessorFail)

    val tfProviderFail =
      new TfProvider(
        terraformBuilderFail,
        TerraformModule(
          Path.of("terraform/src/test/resources/terraform/dummy-acl").toString,
          Map.empty[String, String],
          Map.empty[String, String],
          ""
        )
      )
    val res            = tfProviderFail.validate(descriptor)
    res.isSuccessful shouldBe false

  }

  "validate" should "succeed but acl folder exists" in {

    val mockProcessorFail = new MockProcessor(0, outputString)

    val terraformBuilderFail = Terraform()
      .processor(mockProcessorFail)

    val tfProviderFail =
      new TfProvider(
        terraformBuilderFail,
        TerraformModule(
          Path.of("terraform/src/test/resources/terraform/dummy-acl").toString,
          Map.empty,
          Map(
            "key" -> "$.dataProduct.name",
            "foo" -> "$.dataProduct.intField"
          ),
          "key"
        )
      )

    val res = tfProviderFail.validate(descriptor)
    res.isSuccessful shouldBe true

  }

  "validate" should "fails with variables from config" in {

    val res = tfProviderFail.validate(descriptor)
    res.isSuccessful shouldBe false

  }

  "unprovisioning" should "succeed with variables from config" in {

    val res = tfProvider.unprovision(descriptor, Set(), true)
    res.provisioningStatus shouldBe ProvisioningStatus.Completed

  }

  "TerraformOutputsDescriptor" should "extracts the outputs from the descriptor" in {

    val descriptor = descriptorFrom(
      """
        |id: urn:dmb:dp:sales:invoices:0
        |info:
        |  publicInfo:
        |    taskId:
        |      type: string
        |      label: Task ID
        |      value: b4a8a5a2-af34-4b51-aeb8-7b9e9c9f48d4
        |  privateInfo:
        |    outputs:
        |      comp_id:
        |        value: "/subscriptions/61eabe24-6c0f-40d4-bd5c-4a7f9026e819/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/tfspecificprovisioner"
        |      foo:
        |        value: bar
        |  latestProvisioningOperation:
        |    status: Successful
        |    operation: Deploy
        |    kind: dataproduct
        |name: Invoices
        |tags: []
        |""".stripMargin
    )

    val r = TerraformOutputsDescriptor(descriptor).mapOutputs

    r.isRight shouldBe true
    r.getOrElse(null).size shouldBe 2
    r.getOrElse(null).tail shouldBe Map("foo" -> "bar")
  }

  "The parser" should "parse the descriptor in json format correctly" in {

    val s =
      "{\"status\":\"COMPLETED\",\"result\":\"\",\"info\":{\"publicInfo\":{},\"privateInfo\":{\"outputs\":{\"comp_id\":{\"value\":\"/subscriptions/61eabe24-6c0f-40d4-bd5c-4a7f9026e819/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/tfspecificprovisionertwo\"},\"comp_name\":{\"value\":\"tfspecificprovisionertwo\"}}}},\"logs\":null}"

    val parser: Parser = ParserFactory.parser()
    val p              = parser.parseJson(s)

    p.isInvalidInput shouldBe false
    p.descriptor.sub("info").sub("publicInfo").succeeded shouldBe true

  }

  private val descriptor3: ComponentDescriptor = descriptorFrom(
    """
      |dataProduct:
      |    dataProductOwnerDisplayName: Jhon Doe
      |    intField: 33
      |    doubleField: 33.9
      |    environment: development
      |    domain: healthcare
      |    kind: dataproduct
      |    domainId: urn:dmb:dmn:healthcare
      |    id: urn:dmb:dp:healthcare:vaccinations-nb:0
      |    components:
      |      - kind: outputport
      |        id: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
      |        description: Output Port for vaccinations data using Hasura
      |        name: Hasura Output Port
      |        fullyQualifiedName: Hasura Output Port
      |        version: 0.0.0
      |        infrastructureTemplateId: urn:dmb:itm:hasura-outputport-provisioner:0
      |        useCaseTemplateId: urn:dmb:utm:hasura-outputport-template:0.0.0
      |        dependsOn:
      |          - urn:dmb:cmp:healthcare:vaccinations-nb:0:snowflake-output-port
      |        platform: Hasura
      |        technology: Hasura
      |        outputPortType: GraphQL
      |        creationDate: 2023-06-12T12:52:11.737Z
      |        startDate: 2023-06-12T12:52:11.737Z
      |        tags: []
      |        sampleData: {}
      |        semanticLinking: []
      |        specific:
      |            adls:
      |             sa1:
      |                resourcetype: dls
      |                account_tier: Standard
      |                account_kind: StorageV2
      |                access_tier: Hot
      |                account_replication_type: RAGRS
      |                min_tls_version: TLS1_2
      |                is_hns_enabled: true
      |                rand_value: dls01
      |                sas_policy_expiration: 00.08:00:00
      |                sas_expiration_action: Log
      |                dls:
      |                   - raw
      |                   - enr
      |                   - dev
      |             sa2:
      |                resourcetype: dls
      |                account_tier: Standard
      |                account_kind: StorageV2
      |                access_tier: Hot
      |                account_replication_type: RAGRS
      |                min_tls_version: TLS1_2
      |                is_hns_enabled: true
      |                rand_value: dls01
      |                sas_policy_expiration: 00.08:00:00
      |                sas_expiration_action: Log
      |                dls:
      |                   - raw
      |                   - enr
      |                   - dev
      |                private_link_resource_access:
      |                   enr:
      |                     private_link_access:
      |                       - endpoint_resource_id: /subscriptions/00000000-0000-0000-0000-000000000000/resourcegroups/Fabric/providers/Microsoft.Fabric/workspaces/c8628ad0-3e1c-46ba-a7f4-1288c612a172
      |                   dev:
      |                     private_link_access:
      |                       - endpoint_resource_id: /subscriptions/00000000-0000-0000-0000-000000000001/resourcegroups/Fabric/providers/Microsoft.Fabric/workspaces/c8628ad0-3e1c-46ba-a7f4-1288c612a170
      |
      |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
      |
      |""".stripMargin
  )

  "variablesFrom" should "return correct complex vars with nested maps and lists" in {

    val variableMappings = Some(
      Map(
        "adls" -> "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.adls"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor3, variableMappings)
    vars.isLeft shouldBe false

    val s    = vars.getOrElse(null).variables.getOrElse("adls", null)
    val json = parser.parse(s).getOrElse(null)

    json.hcursor
      .downField("sa2")
      .downField("private_link_resource_access")
      .downField("enr")
      .downField("private_link_access")
      .values
      .orNull
      .size shouldBe 1
    json.hcursor
      .downField("sa2")
      .downField("private_link_resource_access")
      .downField("dev")
      .downField("private_link_access")
      .values
      .orNull
      .size shouldBe 1
    json.hcursor.downField("sa2").downField("dls").values.orNull.size shouldBe 3

  }

  private val descriptor4: ComponentDescriptor = descriptorFrom(
    """
      |check:
      |  bad: it's not so ok
      |  good: this is ok
    """.stripMargin
  )

  "variablesFrom" should "return correct complex var and escape single quote" in {

    val variableMappings = Some(
      Map(
        "check" -> "$.check"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor4, variableMappings)
    vars.isLeft shouldBe false

    vars.getOrElse(null).toOptions should include("it\\'s not so ok")

  }

  "variablesFrom" should "return correct var using a catalogInfo" in {

    val str = """
                |{"check":{
                |  "bad": "it's not so ok",
                |  "good": "this is ok"
                |  }
                |}
    """.stripMargin

    val descriptor5: CirceParsedDescriptor = CirceParsedCatalogInfo(parser.parse(str).getOrElse(Json.Null))

    val variableMappings = Some(
      Map(
        "check" -> "$.check.good"
      )
    )

    val vars = tfProvider.variablesFrom(descriptor5, variableMappings)
    vars.isLeft shouldBe false
    vars.value.toOptions should include("this is ok")

  }

}
