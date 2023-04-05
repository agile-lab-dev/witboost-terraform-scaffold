package it.agilelab.spinframework.app.features.compiler

import ValidationResultFactory.validationResultWithErrors
import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class CompileServiceTest extends AnyFlatSpec with should.Matchers with FrameworkTestSupport {
  val parser: Parser = ParserFactory.parser()

  "The compile result" should "return the descriptor when 'isSuccess' precondition is true" in {
    val yaml = """
       field1: "1"
       field2: "2"
    """

    val aDescriptor                  = descriptorFrom(yaml)
    val compileResult: CompileResult = CompileResult.success(aDescriptor)

    compileResult.isSuccess shouldBe true
    compileResult.descriptor shouldBe aDescriptor
  }

  "The compile result" should "throw an error when 'isSuccess' precondition is false" in {
    val compileResult: CompileResult = CompileResult.failure(Seq.empty)

    compileResult.isSuccess shouldBe false
    an[RuntimeException] should be thrownBy compileResult.descriptor
  }

  "The compile service" should "return a success when the validator returns a success" in {
    val validator: DescriptorValidator = _ => ValidationResult.create
    val compile: CompileService        = new CompileService(parser, validator)
    val yamlDescriptor                 = YamlDescriptor("""
       field1: "1"
       field2: "2"
       field3: "3"
    """)

    val compileResult: CompileResult = compile.doCompile(yamlDescriptor)

    compileResult.isSuccess shouldBe true
  }

  "The compile service" should "return a no-success when the validator returns some error" in {
    val validator: DescriptorValidator = _ => validationResultWithErrors("field1")
    val compile: CompileService        = new CompileService(parser, validator)
    val yamlDescriptor                 = YamlDescriptor("""
       field1: "1"
       field2: "2"
       field3: "3"
    """)

    val compileResult: CompileResult = compile.doCompile(yamlDescriptor)

    compileResult.isSuccess shouldBe false
  }

  "The compile service" should "return a no-success when the descriptor is an invalid yaml" in {
    val compile: CompileService = new CompileService(parser, null)
    val invalidYamlDescriptor   = YamlDescriptor("""
       invalid-field: 1:2:3
    """)

    val compileResult: CompileResult = compile.doCompile(invalidYamlDescriptor)

    compileResult.isSuccess shouldBe false
  }

  "The compile service" should "return a list of validation errors" in {
    val validationResult                   = validationResultWithErrors("field1", "field2")
    val validatorStub: DescriptorValidator = _ => validationResult
    val compile: CompileService            = new CompileService(parser, validatorStub)
    val yamlDescriptor                     = YamlDescriptor("""
       field1: "1"
       field2: "2"
    """)

    val compileResult: CompileResult = compile.doCompile(yamlDescriptor)

    compileResult.isSuccess shouldBe false
    compileResult.errors.size shouldBe validationResult.errors.size
  }
}
