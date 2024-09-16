package it.agilelab.spinframework.app.features.provision

import cats.effect.testing.scalatest.AsyncIOSpec
import com.typesafe.config.{ Config, ConfigFactory }
import io.circe.Json
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.features.compiler.{ TerraformOutput, YamlDescriptor }
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should

class AsyncProvisionTest extends AsyncFlatSpec with should.Matchers with AsyncIOSpec with MockitoSugar {
  val result: ProvisionResult  = ProvisionResult.completed(Seq(TerraformOutput("key", Json.fromString("value"))))
  val syncProvision: Provision = new Provision {
    override def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config): ProvisionResult = result

    override def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config): ProvisionResult = result

    override def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config): ProvisionResult = result

    override def doValidate(yamlDescriptor: YamlDescriptor): ProvisionResult = result

    override def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): ProvisionResult = result
  }

  "fromSyncProvision" should "return the same provision output as the sync provision but wrapped in IO" in {
    val asyncProvision = AsyncProvision.fromSyncProvision(syncProvision)
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
    asyncProvision.doProvisioning(yamlDescriptor).map(res => res shouldEqual result)
  }

  it should "return the same unprovision output as the sync provision but wrapped in IO" in {
    val asyncProvision = AsyncProvision.fromSyncProvision(syncProvision)
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
    asyncProvision.doUnprovisioning(yamlDescriptor, removeData = false).map(res => res shouldEqual result)
  }

  it should "return the same validate output as the sync provision but wrapped in IO" in {
    val asyncProvision = AsyncProvision.fromSyncProvision(syncProvision)
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
    asyncProvision.doValidate(yamlDescriptor).map(res => res shouldEqual result)
  }

  it should "return the same update acl output as the sync provision but wrapped in IO" in {
    val asyncProvision = AsyncProvision.fromSyncProvision(syncProvision)
    val descriptor     =
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
    asyncProvision.doUpdateAcl(provisionInfo, refs, config).map(res => res shouldEqual result)
  }
}
