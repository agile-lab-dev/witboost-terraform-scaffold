package it.agilelab.provisionermock

import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.api.generated.definitions.{
  DescriptorKind,
  ProvisioningRequest,
  ProvisioningStatus
}
import it.agilelab.spinframework.app.features.support.test.HttpResponse

class SynchronousSpMockTest extends SpMockSuite {
  override def specificProvisioner: SpecificProvisioner = SynchronousSpMock

  "The synchronous spmock" should "accept a provision request and return the provisioning status" in {
    val provisionResponse: HttpResponse[ProvisioningStatus] = httpClient.post(
      endpoint = "/v1/provision",
      request =
        ProvisioningRequest(descriptorKind = DescriptorKind.ComponentDescriptor, descriptor = "container: somename"),
      bodyClass = classOf[ProvisioningStatus]
    )

    provisionResponse.status shouldBe 200
    provisionResponse.body.status shouldBe ProvisioningStatus.Status.Completed
  }

}
