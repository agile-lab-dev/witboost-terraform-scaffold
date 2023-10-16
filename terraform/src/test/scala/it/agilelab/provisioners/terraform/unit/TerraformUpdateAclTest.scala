package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.terraform.{ Terraform, TerraformModule }
import it.agilelab.spinframework.app.api.generated.definitions.{ ProvisioningStatus => PSto }
import it.agilelab.spinframework.app.features.compiler.{ ParserFactory, YamlDescriptor }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformUpdateAclTest extends AnyFlatSpec with should.Matchers {

  val parser = ParserFactory.parser()

  "Terraform" should "fail due to unsuccessful init" in {

    val refs       = Set("alice", "bob")
    val descriptor = YamlDescriptor("""
                                      |id: urn:dmb:dp:sales:invoices:0
                                      |  latestProvisioningOperation:
                                      |    status: Successful
                                      |    operation: Deploy
                                      |    kind: dataproduct
                                      |name: Invoices
                                      |foo: bar
                                      |""".stripMargin).parse(parser).descriptor

    val mockProcessor = new MockProcessor(1, "")

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule("folder", Map.empty)
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.updateAcl(descriptor, refs)

    res.isSuccessful shouldBe false

  }

  "Terraform" should "apply ACLs" in {

    val refs         = Set("alice", "bob")
    val outputString = "Apply complete!"
    val descriptor   = YamlDescriptor(
      """
        |id: urn:dmb:dp:sales:invoices:0
        |info:
        |  publicInfo:
        |  privateInfo:
        |    outputs:
        |      foo:
        |        value: bar
        |  latestProvisioningOperation:
        |    status: Successful
        |    operation: Deploy
        |    kind: dataproduct
        |name: Invoices
        |foo: bar
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new MockProcessor(0, outputString)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule("folder", Map.empty)
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.updateAcl(descriptor, refs)

    res.isSuccessful shouldBe true
  }

  "Terraform" should "fails to parse descriptor due to missing Info block" in {

    val refs         = Set("alice", "bob")
    val outputString = "Apply complete!"
    val descriptor   = YamlDescriptor("""
                                      |id: urn:dmb:dp:sales:invoices:0
                                      |  latestProvisioningOperation:
                                      |    status: Successful
                                      |    operation: Deploy
                                      |    kind: dataproduct
                                      |name: Invoices
                                      |foo: bar
                                      |""".stripMargin).parse(parser).descriptor

    val mockProcessor = new MockProcessor(0, outputString)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule("folder", Map.empty)
    val provider        = new TfProvider(terraformBuilder, terraformModule)

    val res = provider.updateAcl(descriptor, refs)

    res.isSuccessful shouldBe false
    res.errors.size shouldBe 1
  }

  "Terraform" should "apply ACLs if there are no outputs stored in descriptor" in {

    val refs         = Set("alice", "bob")
    val outputString = "Apply complete!"
    val descriptor   = YamlDescriptor(
      """
        |id: urn:dmb:dp:sales:invoices:0
        |info:
        |  publicInfo:
        |  privateInfo:
        |    outputs: {}
        |  latestProvisioningOperation:
        |    status: Successful
        |    operation: Deploy
        |    kind: dataproduct
        |name: Invoices
        |foo: bar
        |""".stripMargin
    ).parse(parser).descriptor

    val mockProcessor = new MockProcessor(0, outputString)

    val terraformBuilder = Terraform()
      .processor(mockProcessor)

    val terraformModule = TerraformModule("folder", Map.empty)
    val provider        = new TfProvider(terraformBuilder, terraformModule)
    val res             = provider.updateAcl(descriptor, refs)

    res.isSuccessful shouldBe true

  }

}
