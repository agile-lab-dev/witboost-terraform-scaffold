package it.agilelab.spinframework.app.features.provision

import it.agilelab.spinframework.app.cloudprovider.CloudProviderStub
import it.agilelab.spinframework.app.config.SynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ValidateServiceTest extends AnyFlatSpec with should.Matchers {

  val parser: Parser = ParserFactory.parser()

  "The provision service" should "return a 'completed' result for the validation component" in {
    val validator: DescriptorValidator     = _ => ValidationResult.create
    val compile                            = new CompileService(parser, validator)
    val cProvider                          = CloudProviderStub.validate(_ => ProvisionResult.completed())
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
      field1: "1"
      field2: "2"
      field3: "3"
    """)

    val validationResult: ProvisionResult = provisionService.doValidate(yamlDescriptor)

    validationResult.isSuccessful shouldBe true

  }

  it should "return an error for an invalid yaml descriptor" in {
    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val provision: ProvisionService    = new ProvisionService(compile, null, null)

    val invalidYaml = YamlDescriptor("""
      invalid-field: 1:2:3
    """)

    val validationResult: ProvisionResult = provision.doValidate(invalidYaml)

    validationResult.isSuccessful shouldBe false
    validationResult.errors.size shouldBe 1
    validationResult.errors.head shouldBe ErrorMessage("Invalid Descriptor")

  }

  it should "return a validation failure from cloud provider" in {
    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val cloudProviderErrors            = Seq(ErrorMessage("some cloud error"))
    val cProvider                      = CloudProviderStub.validate(_ => ProvisionResult.failure(cloudProviderErrors))
    val deps                           = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(cProvider)
    }
    val provision: ProvisionService    = new ProvisionService(compile, deps, null)

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1
    """)

    val validationResult: ProvisionResult = provision.doValidate(yamlDescriptor)

    validationResult.isSuccessful shouldBe false
    validationResult.errors.head shouldBe ErrorMessage("some cloud error")
  }

  it should "return a failure from cloud provider" in {
    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val cloudProviderErrors            = Seq(ErrorMessage("some cloud error"))
    val cProvider                      = CloudProviderStub.validate(_ => ProvisionResult.failure(cloudProviderErrors))
    val deps                           = new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator                       = validator
      override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Left("something bad happened here")
    }
    val provision: ProvisionService    = new ProvisionService(compile, deps, null)

    val yamlDescriptor = YamlDescriptor("""
      dataProduct:
        components:
          - kind: workload
            id: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
            useCaseTemplateId: urn:dmb:utm:airbyte-standard:0.0.0
      componentIdToProvision: urn:dmb:cmp:healthcare:vaccinations-nb:0:airbyte-workload
      some-field: 1
    """)

    val validationResult: ProvisionResult = provision.doValidate(yamlDescriptor)

    validationResult.isSuccessful shouldBe false
    validationResult.errors.head shouldBe ErrorMessage("something bad happened here")
  }

}
