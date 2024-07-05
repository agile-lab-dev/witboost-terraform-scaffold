package it.agilelab.spinframework.app.features.provision

import cats.effect.testing.scalatest.AsyncIOSpec
import com.typesafe.config.ConfigFactory
import io.circe.Json
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.config.Configuration.provisionerConfig
import it.agilelab.spinframework.app.features.compiler.{ TerraformOutput, YamlDescriptor }
import it.agilelab.spinframework.app.features.status.CacheRepository
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should

class AsyncProvisionerServiceTest extends AsyncFlatSpec with should.Matchers with AsyncIOSpec with MockitoSugar {

  "testDoProvisioning" should "store the task and return running result" in {
    val mockProvision  = mock[Provision]
    val repository     = new CacheRepository
    val asyncProvision = new AsyncProvisionerService(
      provision = mockProvision,
      taskRepository = repository,
      executionContext = this.executionContext
    )

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        dataProductOwner: user:name.surname_email.com
        devGroup: group:dev
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      field1: "1"
      field2: "2"
      field3: "3"
    """)
    val expectedResult = ProvisionResult.completed(Seq(TerraformOutput("key", Json.fromString("value"))))
    when(mockProvision.doProvisioning(yamlDescriptor)).thenReturn(expectedResult)

    val result = asyncProvision.doProvisioning(yamlDescriptor)

    result.map { result =>
      result.provisioningStatus shouldEqual ProvisioningStatus.Running
      result.componentToken.isEmpty shouldBe false
    }
  }

  "testDoUnprovisioning" should "store the task and return running result" in {
    val mockProvision  = mock[Provision]
    val repository     = new CacheRepository
    val asyncProvision = new AsyncProvisionerService(
      provision = mockProvision,
      taskRepository = repository,
      executionContext = this.executionContext
    )

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        dataProductOwner: user:name.surname_email.com
        devGroup: group:dev
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      field1: "1"
      field2: "2"
      field3: "3"
    """)
    val expectedResult = ProvisionResult.completed()
    when(mockProvision.doUnprovisioning(yamlDescriptor, removeData = true)).thenReturn(expectedResult)

    val result = asyncProvision.doUnprovisioning(yamlDescriptor, removeData = true, provisionerConfig)

    result.map { result =>
      result.provisioningStatus shouldEqual ProvisioningStatus.Running
      result.componentToken.isEmpty shouldBe false
    }
  }

  "testDoUpdateAcl" should "return immediately the update acl result" in {
    val mockProvision  = mock[Provision]
    val repository     = new CacheRepository
    val asyncProvision = new AsyncProvisionerService(
      provision = mockProvision,
      taskRepository = repository,
      executionContext = this.executionContext
    )

    val descriptor =
      """
        |dataProduct:
        |  components:
        |    - kind: workload
        |      id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |      useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |""".stripMargin

    val provisionInfo = ProvisionInfo(descriptor, "{}")
    val refs          = Set("Alice", "Bob")
    val config        = ConfigFactory.parseString(
      """
        |terraform {
        |  "urn:dmb:utm:airbyte-standard:0.0.0" {
        |    principalMappingPlugin {
        |      pluginClass = "it.agilelab.plugin.principalsmapping.impl.identity.IdentityMapperFactory"
        |      identity {}
        |    }
        |  }
        |}
        |""".stripMargin
    )

    val expectedResult = ProvisionResult.completed()
    when(mockProvision.doUpdateAcl(provisionInfo, refs, config)).thenReturn(expectedResult)

    val result = asyncProvision.doUpdateAcl(provisionInfo, refs, config)

    result.map { result =>
      result shouldEqual expectedResult
    }
  }

  "testDoValidate" should "return immediately the validate result" in {
    val mockProvision  = mock[Provision]
    val repository     = new CacheRepository
    val asyncProvision = new AsyncProvisionerService(
      provision = mockProvision,
      taskRepository = repository,
      executionContext = this.executionContext
    )
    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1
    """)

    val expectedResult = ProvisionResult.completed()
    when(mockProvision.doValidate(yamlDescriptor)).thenReturn(expectedResult)

    val result = asyncProvision.doValidate(yamlDescriptor)

    result.map { result =>
      result shouldEqual expectedResult
    }
  }

}
