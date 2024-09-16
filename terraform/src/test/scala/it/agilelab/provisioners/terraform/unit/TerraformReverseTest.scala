package it.agilelab.provisioners.terraform.unit

import io.circe.Json
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.parser.parse
import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.{ ProcessResult, Processor, Terraform, TerraformModule }
import it.agilelab.spinframework.app.features.compiler.circe.CirceParsedDescriptor
import it.agilelab.spinframework.app.features.compiler.{
  ImportBlock,
  InputParams,
  Parser,
  ParserFactory,
  ReverseChanges
}
import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.nio.file.Files

class TerraformReverseTest extends AnyFlatSpec with should.Matchers {

  val parser: Parser = ParserFactory.parser()
  val tempFolder     = Files.createTempDirectory("tmp-")
  val catalogInfoRaw =
    parse("""
            |
            |  {
            |    "apiVersion": "backstage.io/v1alpha1",
            |    "kind": "Component",
            |    "metadata": {
            |      "name": "finance.cesar.0.cat-storage",
            |      "description": "Existing storage account containing cats",
            |      "annotations": {
            |        "backstage.io/techdocs-ref": "dir:."
            |      },
            |      "tags": [
            |        "azure",
            |        "adlsgen2",
            |        "storage"
            |      ]
            |    },
            |    "spec": {
            |      "instanceOf": "componenttype:default/storage",
            |      "type": "storage",
            |      "lifecycle": "experimental",
            |      "owner": "user:name.surname_agilelab.it",
            |      "system": "system:finance.cesar.0",
            |      "domain": "domain:finance",
            |      "mesh": {
            |        "name": "Cat Storage",
            |        "fullyQualifiedName": null,
            |        "description": "Existing storage account containing cats",
            |        "kind": "storage",
            |        "version": "0.0.0",
            |        "infrastructureTemplateId": "urn:dmb:itm:terraform-adls-provisioner:0",
            |        "useCaseTemplateId": "urn:dmb:utm:azure-storage-adlsgen2-template:0.0.0",
            |        "dependsOn": [],
            |        "platform": "Azure",
            |        "technology": "ADLSGen2",
            |        "tags": [],
            |        "specific": {
            |          "component": {
            |            "dpDomain": "finance",
            |            "dpNameMajorVersion": "cesar_0",
            |            "name": "cat-storage"
            |          },
            |          "resourceGroup": "some-existing-rg",
            |          "containers": [
            |            "default"
            |          ],
            |          "performance": "Standard",
            |          "redundancy": "LRS",
            |          "accessTier": "Hot",
            |          "infrastructureEncryptionEnabled": false,
            |          "allowNestedItemsToBePublic": true,
            |          "state": {
            |            "key": "finance_cesar_0_cat-storage.tfstate"
            |          }
            |        }
            |      }
            |    }
            |  }
            |""".stripMargin)
  val catalogInfo    = CirceParsedDescriptor(catalogInfoRaw.getOrElse(null))
  val inputParams    = parse("""
        {
          "importBlocks" : [
            {
              "id" : "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/storageacctestreverse",
              "to" : "azurerm_storage_account.st_account"
            }
          ],
          "skipSafetyChecks" : false
        }
        """).getOrElse(Json.Null).as[InputParams].value

  "Terraform" should "succeed to import one resource" in {

    val changes       =
      "{\"@level\":\"info\",\"@message\":\"Plan: 1 to import, 0 to add, 1 to change, 0 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":0,\"import\":1,\"remove\":0,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val catalogInfo = CirceParsedDescriptor(catalogInfoRaw.getOrElse(null))

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe true

    res.changes.as[ReverseChanges](ReverseChanges.customDecoder).value.imports.size shouldBe 1

  }

  "Terraform" should "fail to import due to 0 imports" in {

    val changes       =
      "{\"@level\":\"info\",\"@message\":\"Plan: 0 to import, 0 to add, 1 to change, 0 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":0,\"change\":1,\"import\":0,\"remove\":0,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe false
    res.logs.sizeIs > 0
    res.logs.last.message should include("Plan results in 0 resources to import")

  }

  "Terraform" should "succeed to import with 0 imports due to skipSafetyChecks" in {

    val inputParams = parse("""
        {
          "importBlocks" : [
            {
              "id" : "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/storageacctestreverse",
              "to" : "azurerm_storage_account.st_account"
            }
          ],
          "skipSafetyChecks" : true
        }
        """).getOrElse(Json.Null).as[InputParams].value

    val changes       =
      "{\"@level\":\"info\",\"@message\":\"Plan: 0 to import, 0 to add, 1 to change, 0 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":0,\"change\":1,\"import\":0,\"remove\":0,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe true

  }

  "Terraform" should "fail to import due to resources to destroy" in {

    val changes       =
      "{\"@level\":\"info\",\"@message\":\"Plan: 2 to import, 0 to add, 1 to change, 5 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":2,\"change\":0,\"import\":1,\"remove\":5,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe false
    res.logs.sizeIs > 0
    res.logs.last.message should include("Plan results in the destroy of 5 resources")
  }

  "Terraform" should "succeed to import with 5 destroys due to the presence of to skipSafetyChecks" in {

    val inputParams   = parse("""
        {
          "importBlocks" : [
            {
              "id" : "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/storageacctestreverse",
              "to" : "azurerm_storage_account.st_account"
            }
          ],
          "skipSafetyChecks" : true
        }
        """).getOrElse(Json.Null).as[InputParams].getOrElse(null)
    val changes       =
      "{\"@level\":\"info\",\"@message\":\"Plan: 2 to import, 0 to add, 1 to change, 5 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":2,\"change\":0,\"import\":1,\"remove\":5,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe true

  }

  "Terraform" should "fail to import due to empty changes" in {

    val changes       = ""
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe false
    res.logs.sizeIs > 0
    res.logs.last.message should include("It was not possible to parse the result of the plan")
  }

  "Terraform" should "fail to import due to broken changes json" in {

    val changes       =
      "{\"@level\":\"info\",\"@message\":\"Plan: 2 to import, 0 to add, 1 to change, 5 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"type\":\"change_summary\"}"
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe false
    res.logs.sizeIs > 0
    res.logs.last.message should include("It was not possible to parse the result of the plan")
  }

  "Terraform" should "properly fill the changes param" in {

    val changes       =
      "{\"@level\":\"info\",\"@message\":\"Plan: 1 to import, 0 to add, 1 to change, 0 to destroy.\",\"@module\":\"terraform.ui\",\"@timestamp\":\"2024-08-12T10:46:34.822727+02:00\",\"changes\":{\"add\":0,\"import\":1,\"remove\":0,\"operation\":\"plan\"},\"type\":\"change_summary\"}"
    val mockProcessor = new MockProcessor(0, changes)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val catalogInfo = CirceParsedDescriptor(catalogInfoRaw.value)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe true
    res.changes.findAllByKey("spec.mesh.specific.reverse.imports").head.asArray.get.size shouldBe 1
    res.changes
      .findAllByKey("spec.mesh.specific.reverse.imports")
      .head
      .as[Seq[ImportBlock]]
      .getOrElse(Seq())
      .head
      .to shouldBe "azurerm_storage_account.st_account"
  }

  "Terraform" should "fail to run plan" in {

    class CustomMockProcessor extends Processor {
      private val buffer                               = new StringBuffer()
      override def run(command: String): ProcessResult =
        if (command.contains("init"))
          new ProcessResult(0, new MockProcessOutput(""))
        else if (command.contains("plan"))
          new ProcessResult(1, new MockProcessOutput(""))
        else
          new ProcessResult(1, new MockProcessOutput(""))
    }

    val terraformBuilder = Terraform()
      .processor(new CustomMockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map.empty, Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe false

  }

  "Terraform" should "fail to run plan because of non existing mappings in the descriptor" in {

    class CustomMockProcessor extends Processor {
      private val buffer                               = new StringBuffer()
      override def run(command: String): ProcessResult =
        if (command.contains("init"))
          new ProcessResult(0, new MockProcessOutput(""))
        else if (command.contains("plan"))
          new ProcessResult(1, new MockProcessOutput(""))
        else
          new ProcessResult(1, new MockProcessOutput(""))
    }
    val terraformBuilder = Terraform()
      .processor(new CustomMockProcessor)

    val terraformModule = TerraformModule(tempFolder.toString, Map("foo" -> "bar"), Map("key" -> "$.kind"), "key")
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.reverse("", catalogInfo, inputParams)

    res.isSuccessful shouldBe false

  }

}
