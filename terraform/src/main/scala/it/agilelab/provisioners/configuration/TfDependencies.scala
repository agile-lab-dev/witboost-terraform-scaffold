package it.agilelab.provisioners.configuration

import it.agilelab.provisioners.configuration.TfConfiguration._
import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.features.validation.SpecificDescriptorValidator
import it.agilelab.provisioners.terraform.{ Terraform, TerraformLogger }
import it.agilelab.spinframework.app.config.SynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler.DescriptorValidator
import it.agilelab.spinframework.app.features.provision.CloudProvider

import java.nio.file.{ Files, Paths }

class TfDependencies extends SynchronousSpecificProvisionerDependencies {

  require(Files.exists(Paths.get(provisionerConfig.getString(terraform_repositoryPath))))

  private val terraform = Terraform()
    .withLogger(TerraformLogger.logOnConsole)
    .outputInJson()
    .onDirectory(provisionerConfig.getString(terraform_repositoryPath))

  private val terraformAcl = Terraform()
    .withLogger(TerraformLogger.logOnConsole)
    .outputInJson()
    .onDirectory(provisionerConfig.getString(s"$terraform_repositoryPath") + "/acl")

  private val tfProvider: TfProvider = new TfProvider(terraform, terraformAcl)

  override def descriptorValidator: DescriptorValidator = new SpecificDescriptorValidator()
  override def cloudProvider: CloudProvider             = tfProvider

}
