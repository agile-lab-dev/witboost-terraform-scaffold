package it.agilelab.spinframework.app.features.compiler

import DescriptorErrorType._
import Validation.{ Defined, IsValid, NonEmpty }
import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ValidationTest extends AnyFlatSpec with should.Matchers with FrameworkTestSupport {

  "The validation" should "be ok for a defined field" in {
    val field = SimpleField.defined("field", "value")

    val validationResult = Validation.start
      .check(field, Defined)

    shouldBeOk(validationResult)
  }

  "The validation" should "detect an undefined field error" in {
    val field = SimpleField.undefined("field")

    val validationResult = Validation.start
      .check(field, Defined)

    shouldBeError(validationResult, UndefinedField, "field")
  }

  "The validation" should "return an error message with the field's complete path" in {
    val field = SimpleField.undefined(Seq("root", "sub1", "sub2"))

    val validationResult = Validation.start
      .check(field, Defined)

    shouldBeError(validationResult, UndefinedField, "root.sub1.sub2")
  }

  "The validation" should "be ok for a non empty field" in {
    val field = SimpleField.defined("field", "value")

    val validationResult = Validation.start
      .check(field, NonEmpty)

    shouldBeOk(validationResult)
  }

  "The validator" should "detect an empty field error" in {
    val field = SimpleField.defined("field", "")

    val validationResult = Validation.start.check(field, NonEmpty)

    shouldBeError(validationResult, EmptyField, "field")
  }

  "The validation" should "be ok when condition is satisfied" in {
    val field = SimpleField.defined("field", "one")

    val validationResult = Validation.start
      .check(field, IsValid.when(field => field.value == "one").otherwise(GenericError))

    shouldBeOk(validationResult)
  }

  "The validation" should "detect a non-existent value error for a field" in {
    val field = SimpleField.defined("field", "one")

    val validationResult = Validation.start
      .check(field, IsValid.when(field => field.value == "two").otherwise(NonExistentValue))

    shouldBeError(validationResult, NonExistentValue, "field", "one")
  }

  "The validation" should "detect a malformed value error for a field" in {
    val field = SimpleField.defined("field", "one")

    val validationResult = Validation.start
      .check(field, IsValid.when(field => field.value == "two").otherwise(MalformedValue))

    shouldBeError(validationResult, MalformedValue, "field", "one")
  }

  "The validation" should "detect a not-available value error for a field" in {
    val field = SimpleField.defined("field", "one")

    val validationResult = Validation.start
      .check(field, IsValid.when(field => field.value == "two").otherwise(NotAvailableValue))

    shouldBeError(validationResult, NotAvailableValue, "field", "one")
  }

  "The validation" should "be ok for a field with a in-range value" in {
    val field = SimpleField.defined("field", "two")

    val validationResult = Validation.start
      .check(field, IsValid.whenInRange(Seq("one", "two", "three")))

    shouldBeOk(validationResult)
  }

  "The validation" should "detect a not-in-range error for a field" in {
    val field = SimpleField.defined("field", "four")

    val validationResult = Validation.start.check(field, IsValid.whenInRange(Seq("one", "two", "three")))

    shouldBeError(validationResult, NotInRangeValue, "field", "four")
  }

  "The validation" should "be ok for an optional undefined field" in {
    val field = SimpleField.undefined("field")

    val validationResult = Validation.start
      .checkOption(field, IsValid.when(field => false).otherwise(GenericError))

    shouldBeOk(validationResult)
  }

  "The validation" should "be ok for an optional empty field" in {
    val field = SimpleField.defined("field", "  ")

    val validationResult = Validation.start
      .checkOption(field, IsValid.when(field => false).otherwise(GenericError))

    shouldBeOk(validationResult)
  }

  "The validation" should "be ok for multiple conditions in and" in {
    val field = SimpleField.defined("field", "abcde")

    val validationResult = Validation.start
      .checkOption(
        field,
        IsValid.when(field => field.value.startsWith("a")).otherwise(MalformedValue) &&
          IsValid.when(field => field.value.endsWith("e")).otherwise(MalformedValue)
      )

    shouldBeOk(validationResult)
  }

  "The validation" should "detect an error for multiple conditions in and" in {
    val field = SimpleField.defined("field", "abcde")

    val validationResult = Validation.start
      .checkOption(
        field,
        IsValid.when(field => field.value.charAt(0) == 'a').otherwise(GenericError) &&
          IsValid.when(field => field.value.charAt(1) == 'b').otherwise(GenericError) &&
          IsValid.when(field => field.value.charAt(2) == 'c').otherwise(GenericError) &&
          IsValid.when(field => field.value.charAt(3) == 'd').otherwise(GenericError) &&
          IsValid.when(field => field.value.charAt(4) == 'w').otherwise(MalformedValue)
      )

    shouldBeError(validationResult, MalformedValue, "field", "abcde")
  }

  private def shouldBeOk(validationResult: ValidationResult) = {
    validationResult.isSuccess shouldBe true
    validationResult.errors shouldBe empty
  }

  private def shouldBeError(result: ValidationResult, errorType: DescriptorErrorType, words: String*): Unit = {
    result.isSuccess shouldBe false
    result.errors.size shouldBe 1
    val error = result.errors.head

    error.errorType shouldBe errorType
    for (word <- words)
      error.message should include(word)
  }

}
