package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.TerraformModuleLoader
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

}
