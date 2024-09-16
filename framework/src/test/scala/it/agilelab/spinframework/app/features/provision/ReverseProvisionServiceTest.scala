package it.agilelab.spinframework.app.features.provision

import io.circe.Json
import it.agilelab.spinframework.app.cloudprovider.CloudProviderStub
import it.agilelab.spinframework.app.config.SynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler.{ CompileService, DescriptorValidator, ValidationResult, _ }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import io.circe.parser._

class ReverseProvisionServiceTest extends AnyFlatSpec with should.Matchers {

  val parser: Parser = ParserFactory.parser()

  "The provision service" should "cannot decode inputParams" in {

    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.reverse((_, _, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, null)

    val catalogInfo = parse("""
                              |{}
                              |""".stripMargin).getOrElse(Json.Null)
    val inputParams = parse("""
        {
          "foo" : "bar"
        }
        """).getOrElse(Json.Null)

    val reverseResult: ProvisionResult = provisionService.doReverse("", catalogInfo, inputParams)

    reverseResult.isSuccessful shouldBe false
    reverseResult.logs.head.message should include("""Could not decode input params""")

  }

  "The provision service" should "return a 'completed' result for the validation component" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.reverse((_, _, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, null)

    val catalogInfo =
      parse("""
              |
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
              |""".stripMargin).getOrElse(Json.Null)
    val inputParams = parse("""
        {
          "importBlocks" : [
            {
              "id" : "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/storageacctestreverse",
              "to" : "azurerm_storage_account.st_account"
            }
          ],
          "skipSafetyChecks": false
        }
        """).getOrElse(Json.Null)

    val reverseResult: ProvisionResult = provisionService.doReverse("", catalogInfo, inputParams)

    reverseResult.isSuccessful shouldBe true

  }

}
