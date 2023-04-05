package it.agilelab.spinframework.app.features.compiler

import it.agilelab.spinframework.app.features.support.test.FrameworkTestSupport
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ComponentDescriptorTest extends AnyFlatSpec with should.Matchers with FrameworkTestSupport {

  "The descriptor" should "return path and name of parent and nested fields" in {
    val yaml = """
        field1: 
          field2:
            field3: value
    """

    val undefined = descriptorFrom(yaml).field("undefined")
    undefined.path shouldBe Seq("undefined")
    undefined.name shouldBe "undefined"

    val field1 = descriptorFrom(yaml).field("field1")
    field1.path shouldBe Seq("field1")
    field1.name shouldBe "field1"

    val field2 = descriptorFrom(yaml).field("field2")
    field2.path shouldBe Seq("field2")
    field2.name shouldBe "field2"

    val field1_field2 = descriptorFrom(yaml).sub("field1").field("field2")
    field1_field2.path shouldBe Seq("field1", "field2")
    field1_field2.name shouldBe "field1.field2"

    val field1_field2_field3 = descriptorFrom(yaml).sub("field1").sub("field2").field("field3")
    field1_field2_field3.path shouldBe Seq("field1", "field2", "field3")
    field1_field2_field3.name shouldBe "field1.field2.field3"
  }

  "The field" should "say if it is defined or not" in {
    val yaml = """
      unspecified: 
      empty: ""
      spaces: "   "
      one_elem: a
      multi_line_array: 
        - "a"
        - "b"
      parent_field:
        nested: something
    """

    val descriptor = descriptorFrom(yaml)

    descriptor.field("undefined").defined shouldBe false
    descriptor.field("unspecified").defined shouldBe true
    descriptor.field("empty").defined shouldBe true
    descriptor.field("spaces").defined shouldBe true
    descriptor.field("one_elem").defined shouldBe true
    descriptor.field("multi_line_array").defined shouldBe true
    descriptor.field("parent_field").defined shouldBe true
  }

  "The field" should "return a value" in {
    val yaml = """
      unspecified: 
      empty: ""
      spaces: "   "
      one_elem: a
      one_number: 1
      one_elem_with_comma: a, b
      more_numbers_with_comma: 1, 2
      mix_elems_numbers: a, 2
      empty_array: []
      one_elem_array: [a]
      one_number_array: [1]
      two_elems_array: [a, b]
      two_numbers_array: [1, 2]
      two_elems_array_with_apex: ["a", "b"]
      multi_line_array: 
        - "a"
        - "b"
      parent_field:
        nested: something
    """

    val descriptor = descriptorFrom(yaml)

    descriptor.field("undefined").value shouldBe ""
    descriptor.field("unspecified").value shouldBe ""
    descriptor.field("empty").value shouldBe ""
    descriptor.field("spaces").value shouldBe "   "
    descriptor.field("one_elem").value shouldBe "a"
    descriptor.field("one_number").value shouldBe "1"
    descriptor.field("one_elem_with_comma").value shouldBe "a, b"
    descriptor.field("more_numbers_with_comma").value shouldBe "1, 2"
    descriptor.field("mix_elems_numbers").value shouldBe "a, 2"
    descriptor.field("empty_array").value shouldBe "[]"
    descriptor.field("one_elem_array").value shouldBe "[a]"
    descriptor.field("one_number_array").value shouldBe "[1]"
    descriptor.field("two_elems_array").value shouldBe "[a, b]"
    descriptor.field("two_numbers_array").value shouldBe "[1, 2]"
    descriptor.field("two_elems_array_with_apex").value shouldBe "[a, b]"
    descriptor.field("multi_line_array").value shouldBe "[a, b]"
    an[RuntimeException] should be thrownBy descriptor.field("parent_field").value
  }

  "The field" should "return a list of values" in {
    val yaml = """
      unspecified: 
      empty: ""
      spaces: "   "
      one_elem: a
      one_number: 1
      one_elem_with_comma: a, b
      more_numbers_with_comma: 1, 2
      mix_elems_numbers: a, 2
      empty_array: []
      one_elem_array: [a]
      one_number_array: [1]
      two_elems_array: [a, b]
      two_numbers_array: [1, 2]
      two_elems_array_with_apex: ["a", "b"]
      multi_line_array: 
        - "a"
        - "b"
      parent_field:
        nested: something
    """

    val descriptor = descriptorFrom(yaml)

    descriptor.field("undefined").values shouldBe Seq.empty
    descriptor.field("unspecified").values shouldBe Seq.empty
    descriptor.field("empty").values shouldBe Seq("")
    descriptor.field("spaces").values shouldBe Seq("   ")
    descriptor.field("one_elem").values shouldBe Seq("a")
    descriptor.field("one_number").values shouldBe Seq("1")
    descriptor.field("one_elem_with_comma").values shouldBe Seq("a, b")
    descriptor.field("more_numbers_with_comma").values shouldBe Seq("1, 2")
    descriptor.field("mix_elems_numbers").values shouldBe Seq("a, 2")
    descriptor.field("empty_array").values shouldBe Seq.empty
    descriptor.field("one_elem_array").values shouldBe Seq("a")
    descriptor.field("one_number_array").values shouldBe Seq("1")
    descriptor.field("two_elems_array").values shouldBe Seq("a", "b")
    descriptor.field("two_numbers_array").values shouldBe Seq("1", "2")
    descriptor.field("two_elems_array_with_apex").values shouldBe Seq("a", "b")
    descriptor.field("multi_line_array").values shouldBe Seq("a", "b")
    an[RuntimeException] should be thrownBy descriptor.field("parent_field").values
  }

  "The field" should "say if it is empty or not" in {
    val yaml = """
      unspecified: 
      empty: ""
      spaces: "   "
      one_elem: a
      one_number: 1
      one_elem_with_comma: a, b
      more_numbers_with_comma: 1, 2
      mix_elems_numbers: a, 2
      empty_array: []
      one_elem_array: [a]
      one_number_array: [1]
      two_elems_array: [a, b]
      two_numbers_array: [1, 2]
      two_elems_array_with_apex: ["a", "b"]
      multi_line_array: 
        - "a"
        - "b"
      parent_field:
        nested: something
    """

    val descriptor = descriptorFrom(yaml)

    descriptor.field("undefined").empty shouldBe false
    descriptor.field("unspecified").empty shouldBe true
    descriptor.field("empty").empty shouldBe true
    descriptor.field("spaces").empty shouldBe true
    descriptor.field("one_elem").empty shouldBe false
    descriptor.field("one_number").empty shouldBe false
    descriptor.field("one_elem_with_comma").empty shouldBe false
    descriptor.field("more_numbers_with_comma").empty shouldBe false
    descriptor.field("mix_elems_numbers").empty shouldBe false
    descriptor.field("empty_array").empty shouldBe true
    descriptor.field("one_elem_array").empty shouldBe false
    descriptor.field("one_number_array").empty shouldBe false
    descriptor.field("two_elems_array").empty shouldBe false
    descriptor.field("two_numbers_array").empty shouldBe false
    descriptor.field("two_elems_array_with_apex").empty shouldBe false
    descriptor.field("multi_line_array").empty shouldBe false
    an[RuntimeException] should be thrownBy descriptor.field("parent_field").empty
  }

  "The field" should "return a default value when empty" in {
    val yaml = """
      unspecified: 
      empty: ""
      spaces: "   "
      one_elem: a
      one_number: 1
      parent_field:
        nested: something
    """

    val descriptor = descriptorFrom(yaml)
    descriptor.field("undefined").valueOrElse("default") shouldBe "default"
    descriptor.field("unspecified").valueOrElse("default") shouldBe "default"
    descriptor.field("empty").valueOrElse("default") shouldBe "default"
    descriptor.field("spaces").valueOrElse("default") shouldBe "default"
    descriptor.field("one_elem").valueOrElse("default") shouldBe "a"
    an[RuntimeException] should be thrownBy descriptor.field("parent_field").valueOrElse("default")
  }

}
