package it.agilelab.provisioners.features.provider

import it.agilelab.provisioners.terraform.{ TerraformCommands, TerraformVariables }
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ErrorMessage }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

class TfProvider(terraform: TerraformCommands) extends CloudProvider {

  private lazy val terraformInitResult = terraform.doInit()

  override def provision(descriptor: ComponentDescriptor): ProvisionResult = {
    if (!terraformInitResult.isSuccess)
      return ProvisionResult.failure(Seq(ErrorMessage(terraformInitResult.buildOutputString)))

    val applyResult = terraform.doApply(variablesFrom(descriptor))

    if (!applyResult.isSuccess)
      return ProvisionResult.failure(Seq(ErrorMessage(applyResult.buildOutputString)))

    ProvisionResult.completed()
  }

  override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = {
    val result = terraform.doDestroy(variablesFrom(descriptor))

    if (result.isSuccess)
      ProvisionResult.completed()
    else
      ProvisionResult.failure(Seq(ErrorMessage(result.buildOutputString)))
  }

  private def variablesFrom(descriptor: ComponentDescriptor) =
    //val specificDescriptor = SpecificDescriptor(descriptor)
    TerraformVariables.noVariable()
  // TODO: how to map descriptor to tf variables in a generic way?
  /*
    TerraformVariables.variables(
      "resource_group_name"      -> adlsDescriptor.resourceGroup,
      "storage_account_name"     -> adlsDescriptor.storageAccountName,
      "storage_account_location" -> adlsDescriptor.region,
      "filesystem_name"          -> adlsDescriptor.containerName,
      "path"                     -> adlsDescriptor.directories.head
    ) */
}
