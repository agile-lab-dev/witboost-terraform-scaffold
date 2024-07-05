package it.agilelab.provisionermock

import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.api.generated.definitions.{
  DescriptorKind,
  ProvisionInfo,
  ProvisioningRequest,
  ProvisioningStatus,
  UpdateAclRequest
}
import it.agilelab.spinframework.app.features.support.test.HttpResponse

class SynchronousSpMockTest extends SpMockSuite {
  override def specificProvisioner: SpecificProvisioner = SynchronousSpMock

  "The synchronous spmock" should "accept a provision request and return the provisioning status" in {
    val provisionResponse: HttpResponse[ProvisioningStatus] = httpClient.post(
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
      bodyClass = classOf[ProvisioningStatus]
    )

    provisionResponse.status shouldBe 200
    provisionResponse.body.status shouldBe ProvisioningStatus.Status.Completed
  }

  it should "accept an unprovision request" in {
    val provisioningStatusResponse = httpClient.post(
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
      bodyClass = classOf[ProvisioningStatus]
    )

    provisioningStatusResponse.status shouldBe 200
    provisioningStatusResponse.body.status shouldBe ProvisioningStatus.Status.Completed
  }

  "The synchronous spmock" should "accept an updateacl request and return the provisioning status" in {

    val refs: Vector[String] = Vector("alice", "bob")
    val descriptor           =
      """
        |dataProduct:
        |  components:
        |    - kind: workload
        |      id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |      useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |""".stripMargin

    val provisionResponse: HttpResponse[ProvisioningStatus] = httpClient.post(
      endpoint = "/v1/updateacl",
      request = UpdateAclRequest(refs, ProvisionInfo(descriptor, "{}")),
      bodyClass = classOf[ProvisioningStatus]
    )

    provisionResponse.body.status shouldBe ProvisioningStatus.Status.Completed
  }

}
