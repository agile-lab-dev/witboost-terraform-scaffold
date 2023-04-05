package it.agilelab.provisionermock

import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.api.dtos.{ ProvisionRequestDto, ProvisioningStatusDto, ValidateResponseDto }
import it.agilelab.spinframework.app.features.support.test.LocalHttpClient
import it.agilelab.provisionermock.config.SpMockConfiguration
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

trait SpMockSuite extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {
  // By using "localhost", tests also run in Jenkins pipelines
  private val interface: String = "localhost"
  private val port              = SpMockConfiguration().getInt(SpMockConfiguration.networking_httpServer_port)

  val httpClient = new LocalHttpClient(port)

  def specificProvisioner: SpecificProvisioner

  override protected def beforeAll(): Unit = {
    specificProvisioner.main(Array())
    Thread.sleep(1000)
  }

  override protected def afterAll(): Unit = {
    specificProvisioner.teardown()
    Thread.sleep(1000)
  }

  "The spmock" should "return an health-check response" in {
    val healthCheckResponse = httpClient.get(
      endpoint = "",
      bodyClass = classOf[String]
    )

    healthCheckResponse.body shouldBe "server-running"
  }

  it should "validate a provision request" in {
    val descriptor =
      """
      region: west-europe
      container:
        name: name-container
        size: Medium
    """

    val validateResponse: ValidateResponseDto = httpClient
      .post(
        endpoint = "/validate",
        request = ProvisionRequestDto(descriptor),
        bodyClass = classOf[ValidateResponseDto]
      )
      .body

    validateResponse.valid shouldBe true
    validateResponse.error.errors shouldBe empty
  }

  it should "accept an unprovision request" in {
    val provisioningStatusResponse = httpClient.post(
      endpoint = "/unprovision",
      request = ProvisionRequestDto("container: somename"),
      bodyClass = classOf[ProvisioningStatusDto]
    )

    provisioningStatusResponse.status shouldBe 200
    provisioningStatusResponse.body.status shouldBe "COMPLETED"
  }

}
