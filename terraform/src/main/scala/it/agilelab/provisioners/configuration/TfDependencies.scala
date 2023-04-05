package it.agilelab.provisioners.configuration

import it.agilelab.provisioners.configuration.TfConfiguration._
import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.features.validation.SpecificDescriptorValidator
import it.agilelab.provisioners.terraform.{ Terraform, TerraformLogger }
import it.agilelab.spinframework.app.config.SynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler.DescriptorValidator
import it.agilelab.spinframework.app.features.provision.CloudProvider

import scala.reflect.io.File

class TfDependencies extends SynchronousSpecificProvisionerDependencies {

  require(File(provisionerConfig.getString(terraform_repositoryPath)).exists)

  private val terraform = Terraform()
    .withLogger(TerraformLogger.logOnConsole)
    .outputInPlainText()
    .onDirectory(provisionerConfig.getString(terraform_repositoryPath))

  private val tfProvider: TfProvider = new TfProvider(terraform)

  override def descriptorValidator: DescriptorValidator = new SpecificDescriptorValidator()
  override def cloudProvider: CloudProvider             = tfProvider
}
