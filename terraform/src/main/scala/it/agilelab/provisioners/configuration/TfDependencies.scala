package it.agilelab.provisioners.configuration

import it.agilelab.provisioners.features.provider.TfProvider
import it.agilelab.provisioners.features.validation.SpecificDescriptorValidator
import it.agilelab.provisioners.terraform.{ Terraform, TerraformLogger, TerraformModuleLoader }
import it.agilelab.spinframework.app.features.compiler.DescriptorValidator
import it.agilelab.spinframework.app.features.provision.CloudProvider

trait TfDependencies {

  private val terraformBuilder = Terraform()
    .withLogger(TerraformLogger.logOnConsole)
    .outputInJson()

  def descriptorValidator: DescriptorValidator                       = new SpecificDescriptorValidator()
  def cloudProvider(moduleId: String): Either[String, CloudProvider] =
    TerraformModuleLoader.from(moduleId).map(module => new TfProvider(terraformBuilder, module))

}
