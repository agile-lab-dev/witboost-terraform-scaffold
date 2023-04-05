package it.agilelab.provisionermock.config

import it.agilelab.spinframework.app.features.compiler.DescriptorErrorType.MalformedValue
import it.agilelab.spinframework.app.features.compiler._

/*
  descriptor sample:

  region: west-europe
  container:
    name: name-container
    size: Medium
 */
class MockDescriptorValidator extends DescriptorValidator {
  override def validate(descriptor: ComponentDescriptor): ValidationResult = {
    println("######### /validate #########")
    println(descriptor)

    val region        = descriptor.field("region")
    val containerName = descriptor.sub("container").field("name")
    val containerSize = descriptor.sub("container").field("size")

    Validation.start
      .checkOption(
        region,
        Validation.IsValid
          .when(field => field.value.contains("europe"))
          .otherwise(MalformedValue)
      )
      .checkOption(
        containerName,
        Validation.IsValid
          .when(field => field.value.contains("name"))
          .otherwise(MalformedValue)
      )
      .checkOption(
        containerSize,
        Validation.IsValid
          .whenInRange(Seq("Small", "Medium", "Large"))
      )
  }
}
