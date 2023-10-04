package it.agilelab.spinframework.app.features.provision

import it.agilelab.spinframework.app.features.compiler.ErrorMessages.InvalidDescriptor
import it.agilelab.spinframework.app.features.compiler.ValidationResultFactory.validationResultWithErrors
import it.agilelab.spinframework.app.features.compiler._
import it.agilelab.spinframework.app.cloudprovider.CloudProviderStub
import it.agilelab.spinframework.app.features.compiler.{
  CompileService,
  DescriptorValidator,
  ErrorMessage,
  Parser,
  ParserFactory,
  ValidationResult,
  YamlDescriptor
}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ProvisionServiceTest extends AnyFlatSpec with should.Matchers {
  val parser: Parser = ParserFactory.parser()

  "The provision service" should "return a 'completed' result for the provisioned component" in {
    val validator: DescriptorValidator = componentDescriptor => ValidationResult.create
    val compile                        = new CompileService(parser, validator)
    val cloudProvider                  = CloudProviderStub.provision(desc => ProvisionResult.completed())

    val provisionService: ProvisionService =
      new ProvisionService(compile, cloudProvider, null)

    val yamlDescriptor = YamlDescriptor("""
      field1: "1"
      field2: "2"
      field3: "3"
    """)

    val provisionResult: ProvisionResult = provisionService.doProvisioning(yamlDescriptor)

    provisionResult.provisioningStatus shouldBe ProvisioningStatus.Completed
    provisionResult.errors shouldBe empty
  }

  "The provision service" should "return an error for an invalid yaml descriptor" in {
    val compile                     = new CompileService(parser, null)
    val provision: ProvisionService = new ProvisionService(compile, null, null)

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
    val cloudProvider                  = CloudProviderStub.provision(desc => ProvisionResult.failure(cloudProviderErrors))

    val provision: ProvisionService = new ProvisionService(compile, cloudProvider, null)

    val yamlDescriptor = YamlDescriptor("""
      some-field: 1
    """)

    val provisionResult: ProvisionResult = provision.doProvisioning(yamlDescriptor)

    provisionResult.isSuccessful shouldBe false
    provisionResult.errors.head shouldBe ErrorMessage("some cloud error")
  }

}
