package it.agilelab.spinframework.app.features.provision

import it.agilelab.plugin.principalsmapping.api.Mapper
import it.agilelab.spinframework.app.cloudprovider.CloudProviderStub
import it.agilelab.spinframework.app.config.{ PrincipalMapperPluginLoader, SynchronousSpecificProvisionerDependencies }
import it.agilelab.spinframework.app.features.compiler.ErrorMessages.InvalidDescriptor
import it.agilelab.spinframework.app.features.compiler.ValidationResultFactory.validationResultWithErrors
import it.agilelab.spinframework.app.features.compiler._
import org.mockito.Mockito.when
import org.mockito.{ ArgumentMatchers, ArgumentMatchersSugar, IdiomaticMockito }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.util.{ Failure, Success }

class ProvisionServiceTest extends AnyFlatSpec with should.Matchers with IdiomaticMockito with ArgumentMatchersSugar {
  val parser: Parser = ParserFactory.parser()

  "The provision service" should "return a 'completed' result for the provisioned component" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.provision((_, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val principalMapperPluginLoader        = new PrincipalMapperPluginLoader()
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, principalMapperPluginLoader)

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

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.provisioningStatus shouldBe ProvisioningStatus.Completed
    provisionResult.errors shouldBe empty
  }

  "The provision service" should "return an error for an invalid yaml descriptor" in {
    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val provision: ProvisionService    = new ProvisionService(compile, null, null)

    val invalidYaml = YamlDescriptor("""
      invalid-field: 1:2:3
    """)

    val provisionResult: ProvisionResult = provision.doProvisioning(invalidYaml)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors shouldBe Seq(InvalidDescriptor)
  }

  "The provision service" should "return all validation errors for a descriptor" in {
    val validationResult: ValidationResult = validationResultWithErrors("field1", "field2")
    val validator: DescriptorValidator     = _ => validationResult
    val compile                            = new CompileService(parser, validator)
    val provision: ProvisionService        = new ProvisionService(compile, null, null)

    val yamlDescriptor = YamlDescriptor("""
      some-field: 1
    """)

    val provisionResult: ProvisionResult = provision.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors.size shouldBe validationResult.errors.size
  }

  "The provision service" should "return a provision failure from cloud provider" in {
    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val cloudProviderErrors            = Seq(ErrorMessage("some cloud error"))
    val cProvider                      = CloudProviderStub.provision((_, _) => ProvisionResult.failure(cloudProviderErrors))
    val deps                           = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val principalMapperPluginLoader    = new PrincipalMapperPluginLoader()
    val provision: ProvisionService    = new ProvisionService(compile, deps, principalMapperPluginLoader)

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        dataProductOwner: user:name.surname_email.com
        devGroup: group:dev
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1
    """)

    val provisionResult: ProvisionResult = provision.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors.head shouldBe ErrorMessage("some cloud error")
  }

  "The provision service" should "return a failure for a descriptor without useCaseTemplateId present" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.provision((_, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, null)

    val yamlDescriptor = YamlDescriptor("""
         dataProduct:
           components:
             - kind: workload
               id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
         componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
         field1: "1"
         field2: "2"
         field3: "3"
      """)

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
  }

  it should "return a failure if the dataProductOwner extraction fails" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.provision((_, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, null)

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
    """)

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors.size shouldBe 1
    provisionResult.errors.foreach(
      _.description shouldEqual "Terraform variables could not be extracted from the descriptor. No results for path: $.dataProduct.dataProductOwner"
    )
  }

  it should "return a failure if the devGroup extraction fails" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.provision((_, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, null)

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        dataProductOwner: user:name.surname_email.com
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
    """)

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors.size shouldBe 1
    provisionResult.errors.foreach(
      _.description shouldEqual "Terraform variables could not be extracted from the descriptor. No results for path: $.dataProduct.devGroup"
    )
  }

  it should "return a failure if the principals mapping plugin load fails" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.provision((_, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val principalMapperPluginLoader        = mock[PrincipalMapperPluginLoader]
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, principalMapperPluginLoader)
    val expectedError                      = "mapping plugin not found"
    when(principalMapperPluginLoader.load(*)) thenReturn Failure(new Throwable(expectedError))

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        dataProductOwner: user:name.surname_email.com
        devGroup: group:dev
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
    """)

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors.size shouldBe 1
    provisionResult.errors.foreach(
      _.description shouldEqual "An unexpected error occurred while instantiating the Principal Mapper Plugin. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: mapping plugin not found"
    )
  }

  it should "return a failure if the principals mapping fails" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.provision((_, _) => ProvisionResult.completed())
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val principalMapperPluginLoader        = mock[PrincipalMapperPluginLoader]
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, principalMapperPluginLoader)
    val mapper                             = mock[Mapper]
    when(principalMapperPluginLoader.load(*)) thenReturn Success(mapper)
    val expectedError                      = "timeout"
    when(mapper.map(Set("user:name.surname_email.com", "group:dev"))) thenReturn Map(
      "user:name.surname_email.com" -> Left(new Throwable(expectedError)),
      "group:dev"                   -> Right("dev")
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
    """)

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors.size shouldBe 1
    provisionResult.errors.foreach(
      _.description shouldEqual "An error occurred while mapping the subject `user:name.surname_email.com`. Detailed error: timeout"
    )
  }

  it should "return a success if the principals mapping succeeds" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = mock[CloudProvider]
    val deps                               = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val principalMapperPluginLoader        = mock[PrincipalMapperPluginLoader]
    val provisionService: ProvisionService =
      new ProvisionService(compile, deps, principalMapperPluginLoader)
    val mapper                             = mock[Mapper]
    when(principalMapperPluginLoader.load(*)) thenReturn Success(mapper)
    when(mapper.map(Set("user:name.surname_email.com", "group:dev"))) thenReturn Map(
      "user:name.surname_email.com" -> Right("name.surname@email.com"),
      "group:dev"                   -> Right("dev")
    )
    when(cProvider.provision(any, ArgumentMatchers.eq(Set("name.surname@email.com", "dev")))) thenReturn ProvisionResult
      .completed()

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        dataProductOwner: user:name.surname_email.com
        devGroup: group:dev
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
    """)

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe true
  }

}
