package it.agilelab.spinframework.app.features.provision

import com.typesafe.config.ConfigFactory
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.cloudprovider.CloudProviderStub
import it.agilelab.spinframework.app.config.{ PrincipalMapperPluginLoader, SynchronousSpecificProvisionerDependencies }
import it.agilelab.spinframework.app.features.compiler._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class UpdateAclServiceTest extends AnyFlatSpec with should.Matchers {
  val parser: Parser = ParserFactory.parser()

  "The provision service" should "return a 'success' result for successfully loading the plugin" in {

    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.updateAcl((_, _, _) => ProvisionResult.completed())
    val principalMapperPluginLoader        = new PrincipalMapperPluginLoader()
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator = validator

      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, principalMapperPluginLoader)

    val descriptor =
      """
        |dataProduct:
        |  components:
        |    - kind: workload
        |      id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |      useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |""".stripMargin

    val provisionResult: ProvisionResult = provisionService.doUpdateAcl(
      ProvisionInfo(descriptor, "{}"),
      Set("Alice", "Bob"),
      ConfigFactory.parseString(
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
    )

    provisionResult.isSuccessful shouldBe true
  }

  "The provision service" should "return a 'failure' result for unsuccessfully loading the plugin" in {

    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.updateAcl((_, _, _) => ProvisionResult.completed())
    val principalMapperPluginLoader        =
      new PrincipalMapperPluginLoader()
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator = validator

      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, principalMapperPluginLoader)

    val descriptor =
      """
        |dataProduct:
        |  components:
        |    - kind: workload
        |      id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |      useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |""".stripMargin

    val provisionResult: ProvisionResult = provisionService.doUpdateAcl(
      ProvisionInfo(descriptor, "{}"),
      Set("Alice", "Bob"),
      ConfigFactory.parseString("""
                                  |terraform {
                                  |  "urn:dmb:utm:airbyte-standard:0.0.0" {
                                  |    principalMappingPlugin {
                                  |      pluginClass = ""
                                  |    }
                                  |  }
                                  |}
          """.stripMargin)
    )

    provisionResult.isSuccessful shouldBe false

  }

  "The provision service" should "return a 'failure' result for unsuccessfully mapping the subjects" in {

    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.updateAcl((_, _, _) => ProvisionResult.completed())
    val principalMapperPluginLoader        = new PrincipalMapperPluginLoader()
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator = validator

      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, principalMapperPluginLoader)

    val descriptor =
      """
        |dataProduct:
        |  components:
        |    - kind: workload
        |      id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |      useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
        |componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
        |""".stripMargin

    val provisionResult: ProvisionResult = provisionService.doUpdateAcl(
      ProvisionInfo(descriptor, "{}"),
      Set("Nevil"),
      ConfigFactory.parseString("""
                                  |terraform {
                                  |  "urn:dmb:utm:airbyte-standard:0.0.0" {
                                  |    principalMappingPlugin {
                                  |      pluginClass = "it.agilelab.spinframework.app.config.FakeMapperFactory"
                                  |      fake {}
                                  |    }
                                  |  }
                                  |}
    """.stripMargin)
    )

    provisionResult.isSuccessful shouldBe false

  }

}
