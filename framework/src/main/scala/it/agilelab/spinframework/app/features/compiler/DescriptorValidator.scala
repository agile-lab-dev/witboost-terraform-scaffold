package it.agilelab.spinframework.app.features.compiler

/** This trait represents the validation to be performed on the descriptor
  * in order to guarantee it has the right syntax and semantically valid values.
  *
  * As client of the framework, you must extend this trait and provide
  * your own implementation, by accessing the fields within the input descriptor
  * and check their validity.
  *
  * You can use the [[Validation]] helper class to build a proper result.
  */
trait DescriptorValidator {

  /** Checks if the descriptor is well formed and if not it returns the composition's error(s).
    *
    * @param descriptor descriptor of the component containing all required fields
    * @return the validation result
    */
  def validate(descriptor: ComponentDescriptor): ValidationResult
}
