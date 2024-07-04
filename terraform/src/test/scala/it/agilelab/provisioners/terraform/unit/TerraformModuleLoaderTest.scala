package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformModuleLoader
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ParserFactory, YamlDescriptor }
import it.agilelab.spinframework.app.utils.JsonPathUtils
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformModuleLoaderTest extends AnyFlatSpec with should.Matchers with EitherValues {

  "TerraformModuleLoader" should "return correct data for a configured module" in {
    val moduleId = "urn:dmb:utm:airbyte-standard:0.0.0"

    val eitherModule = TerraformModuleLoader.from(moduleId)

    eitherModule.value.path shouldBe "terraform/src/main/resources/terraform"
    eitherModule.value.mappings.size shouldBe 2
    eitherModule.value.mappings.keys should contain allOf ("resource_group_name", "some_type")
    eitherModule.value.mappings.values should contain allOf
      ("$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].dataContract.schema[1].name",
      "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.resourceGroup")
  }

  it should "return a Left if the configured path of the module is not existent" in {
    val moduleId = "useCaseTemplateId2"

    val eitherModule = TerraformModuleLoader.from(moduleId)

    eitherModule.isLeft shouldBe true
  }

  it should "return a Left if the moduleId is not configured" in {
    val moduleId = "useCaseTemplateIdNotConfigured"

    val eitherModule = TerraformModuleLoader.from(moduleId)

    eitherModule.isLeft shouldBe true
  }

  "TerraformModuleLoader" should "fail for missing backendConfig" in {
    val moduleId = "urn:dmb:utm:airbyte-standard:1.0.0"

    val eitherModule = TerraformModuleLoader.from(moduleId)

    eitherModule.isLeft shouldBe true
    eitherModule.left.getOrElse(null) should include("backendConfigs.configs is not existent")

  }

  "TerraformModuleLoader" should "return extract backend configs" in {
    val moduleId = "urn:dmb:utm:airbyte-standard:2.0.0"

    val eitherModule = TerraformModuleLoader.from(moduleId)

    eitherModule.isLeft shouldBe false
    eitherModule.value.backendConfigs.size shouldBe 3
    eitherModule.value.backendConfigs.keys should contain allOf ("foo", "bar")
    eitherModule.value.backendConfigs.values should contain allOf ("$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.abc", "$.dataProduct.components[?(@.id == '{{componentIdToProvision}}')].specific.def")
  }

  "TerraformModuleLoader" should "fail for missing state key" in {
    val moduleId = "urn:dmb:utm:airbyte-standard:3.0.0"

    val eitherModule = TerraformModuleLoader.from(moduleId)

    eitherModule.isLeft shouldBe true
    eitherModule.left.getOrElse(null) should include("backendConfigs.stateKey is not existent")

  }

  "TerraformModuleLoader" should "Fail for inconsistent key and config" in {
    val moduleId = "urn:dmb:utm:airbyte-standard:3.0.1"

    val eitherModule = TerraformModuleLoader.from(moduleId)

    eitherModule.isLeft shouldBe true
    eitherModule.left.getOrElse(null) should include(
      "The configured state key backendConfigs.stateKey doesn't match any item"
    )
  }

  "TerraformModuleLoader" should "extract the moduleId of the data product" in {

    val parser     = ParserFactory.parser()
    val descriptor = YamlDescriptor("""
                                      |dataProduct:
                                      |    dataProductOwnerDisplayName: Jhon
                                      |    id : asd
                                      |    intField: 33
                                      |    billing: {}
                                      |    tags: []
                                      |
                                      |""".stripMargin).parse(parser).descriptor.toString

    JsonPathUtils.isDataProductProvisioning(descriptor) shouldBe true

  }

  "TerraformModuleLoader" should "extract the moduleId of the component" in {

    val parser             = ParserFactory.parser()
    val descriptor: String =
      YamlDescriptor("""
                       |dataProduct:
                       |    dataProductOwnerDisplayName: Jhon Doe
                       |    intField: 33
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
                       |        tags: []
                       |        sampleData: {}
                       |        semanticLinking: []
                       |        specific:
                       |            resourceGroup: healthcare_rg
                       |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:hasura-output-port
                       |
                       |""".stripMargin).parse(parser).descriptor.toString

    JsonPathUtils.isDataProductProvisioning(descriptor) shouldBe false

  }

  "TerraformModuleLoader" should "handle bad descriptor" in {

    val parser     = ParserFactory.parser()
    val descriptor = YamlDescriptor("""
                                      |dataPr!!
                                      |
                                      |""".stripMargin).parse(parser).descriptor.toString

    JsonPathUtils.isDataProductProvisioning(descriptor) shouldBe false

  }

}
