package it.agilelab.provisioners.features.validation

import it.agilelab.spinframework.app.features.compiler._

class SpecificDescriptorValidator() extends DescriptorValidator {

  override def validate(componentDescriptor: ComponentDescriptor): ValidationResult =
    //val desc = SpecificDescriptor(componentDescriptor)

    // TODO: dynamic fields inside specific section are not exposed
    // desc.specific

    // TODO: how to make a generic validation?

    /*
    Validation.start
      .check(desc.tenantIdField, NonEmpty)
      .check(desc.subscriptionIdField, NonEmpty)
      .check(desc.resourceGroupField, NonEmpty)
      .checkOption(desc.regionField, IsValid.whenInRange(Seq("westeurope", "northeurope")))
      .check(desc.storageAccountNameField, NonEmpty && IsValid.when(isValidStorageAccount).otherwise(MalformedValue))
      .check(desc.containerNameField, NonEmpty)
      .checkOption(desc.accessLevelField, IsValid.whenInRange(Seq("Container", "Blob", "None")))
      .check(desc.directoriesField, NonEmpty)
     */

    ValidationResult.create

}
