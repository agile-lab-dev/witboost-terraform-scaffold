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

}
