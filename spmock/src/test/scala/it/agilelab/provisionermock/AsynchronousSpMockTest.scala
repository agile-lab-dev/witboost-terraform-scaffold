package it.agilelab.provisionermock

import io.circe.Json
import io.circe.parser.parse
import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.api.generated.definitions.{
  DescriptorKind,
  ProvisioningRequest,
  ProvisioningStatus,
  ReverseProvisioningRequest,
  ReverseProvisioningStatus
}
import it.agilelab.spinframework.app.features.support.test.HttpResponse

class AsynchronousSpMockTest extends SpMockSuite {

  override def specificProvisioner: SpecificProvisioner = AsynchronousSpMock

  "The asynchronous spmock" should "accept a provision request and return its status from token" in {
    val provisionResponse: HttpResponse[String] = httpClient.post(
      endpoint = "/v1/provision",
      request = ProvisioningRequest(
        descriptorKind = DescriptorKind.ComponentDescriptor,
        descriptor = """
                       |dataProduct:
                       |  dataProductOwner: user:name.surname_email.com
                       |  devGroup: group:dev
                       |  components:
                       |    - kind: workload
                       |      id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
                       |      useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
                       |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
                       |""".stripMargin
      ),
      bodyClass = classOf[String]
    )

    provisionResponse.status shouldBe 202
    val componentToken = provisionResponse.body

    val provisionStatusResponse = httpClient.get(s"/v1/provision/$componentToken/status", classOf[ProvisioningStatus])

    provisionStatusResponse.status shouldBe 200
    provisionStatusResponse.body.status shouldBe ProvisioningStatus.Status.Completed
  }

  "The asynchronous spmock" should "accept an unprovision request and return its status from token" in {
    val provisionResponse: HttpResponse[String] = httpClient.post(
      endpoint = "/v1/unprovision",
      request = ProvisioningRequest(
        descriptorKind = DescriptorKind.ComponentDescriptor,
        descriptor = """
                       |dataProduct:
                       |  dataProductOwner: user:name.surname_email.com
                       |  devGroup: group:dev
                       |  components:
                       |    - kind: workload
                       |      id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
                       |      useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
                       |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
                       |""".stripMargin
      ),
      bodyClass = classOf[String]
    )

    provisionResponse.status shouldBe 202
    val componentToken = provisionResponse.body

    val provisionStatusResponse = httpClient.get(s"/v1/provision/$componentToken/status", classOf[ProvisioningStatus])

    provisionStatusResponse.status shouldBe 200
    provisionStatusResponse.body.status shouldBe ProvisioningStatus.Status.Completed
  }

  "The asynchronous spmock" should "accept a reverse provision request and return its status from token" in {

    val cInfo       =
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
              |""".stripMargin).getOrElse(Json.Null)
    val inputParams =
      parse(
        """
          |{
          |          "importBlocks" : [
          |          {
          |                "id": "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/witboost/providers/Microsoft.Storage/storageAccounts/storageacctestreverse",
          |                "to": "azurerm_storage_account.storage_account"
          |            }
          |            ]
          |}
          |""".stripMargin
      ).getOrElse(Json.Null)

    val request = ReverseProvisioningRequest(
      useCaseTemplateId = "urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload",
      environment = "dev",
      params = Some(inputParams),
      catalogInfo = Some(cInfo)
    )

    val provisionResponse: HttpResponse[String] = httpClient.post(
      endpoint = "/v1/reverse-provisioning",
      request = request,
      bodyClass = classOf[String]
    )

    provisionResponse.status shouldBe 202
    val componentToken = provisionResponse.body

    val provisionStatusResponse =
      httpClient.get(s"/v1/reverse-provisioning/$componentToken/status", classOf[ReverseProvisioningStatus])

    provisionStatusResponse.status shouldBe 200
    provisionStatusResponse.body.status shouldBe ReverseProvisioningStatus.Status.Completed
  }
}
