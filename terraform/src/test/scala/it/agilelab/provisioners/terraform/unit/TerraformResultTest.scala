package it.agilelab.provisioners.terraform.unit

import it.agilelab.provisioners.terraform.Terraform
import it.agilelab.provisioners.terraform.TerraformVariables.noVariable
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TerraformResultTest extends AnyFlatSpec with should.Matchers {

  "Terraform" should "fail apply and contain the extracted error message partially" in {

    val firstRow =
      """
        |{
        |  "@level": "info",
        |  "@message": "Terraform 1.5.3",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-08-02T17:29:17.089030+05:30",
        |  "terraform": "1.5.3",
        |  "type": "version",
        |  "ui": "1.1"
        |}
        |""".stripMargin.replace("\n", "")

    val outputString =
      """
        |{
        |  "@level": "error",
        |  "@message": "Error: Invalid reference",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-08-02T17:29:17.155150+05:30",
        |  "diagnostic": {
        |    "severity": "error",
        |    "summary": "Invalid reference",
        |    "detail": "A reference to a resource type must be followed by at least one attribute access, specifying the resource name.",
        |    "range": {
        |      "filename": "main.tf",
        |      "start": {
        |        "line": 3,
        |        "column": 22,
        |        "byte": 89
        |      },
        |      "end": {
        |        "line": 3,
        |        "column": 25,
        |        "byte": 92
        |      }
        |    },
        |    "snippet": {
        |      "context": "resource \"random_string\" \"random\"",
        |      "code": "  special          = tru",
        |      "start_line": 3,
        |      "highlight_start_offset": 21,
        |      "highlight_end_offset": 24,
        |      "values": []
        |    }
        |  },
        |  "type": "diagnostic"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(1, firstRow.concat("\n").concat(outputString))

    val terraform = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doApply()

    result.isSuccess shouldBe false
    result.errorMessages.size shouldBe 1

    result.errorMessages.exists(
      _.contains(
        "A reference to a resource type must be followed by at least one attribute access, specifying the resource name."
      )
    )

  }

  "Terraform" should "fail plan and and contain the extracted error message partially" in {

    val firstRow =
      """
        |{
        |  "@level": "info",
        |  "@message": "Terraform 1.5.3",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-08-02T17:29:17.089030+05:30",
        |  "terraform": "1.5.3",
        |  "type": "version",
        |  "ui": "1.1"
        |}
        |""".stripMargin.replace("\n", "")

    val outputString =
      """
        |{
        |  "@level": "error",
        |  "@message": "Error: Invalid reference",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-08-02T17:29:17.155150+05:30",
        |  "diagnostic": {
        |    "severity": "error",
        |    "summary": "Invalid reference",
        |    "detail": "A reference to a resource type must be followed by at least one attribute access, specifying the resource name.",
        |    "range": {
        |      "filename": "main.tf",
        |      "start": {
        |        "line": 3,
        |        "column": 22,
        |        "byte": 89
        |      },
        |      "end": {
        |        "line": 3,
        |        "column": 25,
        |        "byte": 92
        |      }
        |    },
        |    "snippet": {
        |      "context": "resource \"random_string\" \"random\"",
        |      "code": "  special          = tru",
        |      "start_line": 3,
        |      "highlight_start_offset": 21,
        |      "highlight_end_offset": 24,
        |      "values": []
        |    }
        |  },
        |  "type": "diagnostic"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(1, firstRow.concat("\n").concat(outputString))

    val terraform = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doPlan()

    result.isSuccess shouldBe false
    result.errorMessages.size shouldBe 1

    result.errorMessages.exists(
      _.contains(
        "A reference to a resource type must be followed by at least one attribute access, specifying the resource name."
      )
    )

  }

  "Terraform" should "fail destroy and and contain the extracted error message partially" in {

    val firstRow =
      """
        |{
        |  "@level": "info",
        |  "@message": "Terraform 1.5.3",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-08-02T17:29:17.089030+05:30",
        |  "terraform": "1.5.3",
        |  "type": "version",
        |  "ui": "1.1"
        |}
        |""".stripMargin.replace("\n", "")

    val outputString =
      """
        |{
        |  "@level": "error",
        |  "@message": "Error: Invalid reference",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2023-08-02T17:29:17.155150+05:30",
        |  "diagnostic": {
        |    "severity": "error",
        |    "summary": "Invalid reference",
        |    "detail": "A reference to a resource type must be followed by at least one attribute access, specifying the resource name.",
        |    "range": {
        |      "filename": "main.tf",
        |      "start": {
        |        "line": 3,
        |        "column": 22,
        |        "byte": 89
        |      },
        |      "end": {
        |        "line": 3,
        |        "column": 25,
        |        "byte": 92
        |      }
        |    },
        |    "snippet": {
        |      "context": "resource \"random_string\" \"random\"",
        |      "code": "  special          = tru",
        |      "start_line": 3,
        |      "highlight_start_offset": 21,
        |      "highlight_end_offset": 24,
        |      "values": []
        |    }
        |  },
        |  "type": "diagnostic"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(1, firstRow.concat("\n").concat(outputString))

    val terraform = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doDestroy(noVariable())

    result.isSuccess shouldBe false
    result.errorMessages.size shouldBe 1

    result.errorMessages.exists(
      _.contains(
        "A reference to a resource type must be followed by at least one attribute access, specifying the resource name."
      )
    )

  }

  "Terraform" should "extract outputs" in {

    val outputString =
      """
        |{
        |  "@level":"info",
        |  "@message":"Outputs: 2",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2023-09-04T14:19:04.774029+02:00",
        |  "outputs":{
        |	   "foo":{
        |      "sensitive":false,
        |      "type":"string",
        |      "value":"bar"
        |    },
        |	   "fiz":{
        |        "sensitive":false,
        |        "type":"string",
        |        "value":"biz"
        |    }
        |	 },
        |  "type":"outputs"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(0, outputString)
    val terraform     = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doApply()

    println(result.buildOutputString)
    val tfOutputs = result.terraformOutputs

    result.isSuccess shouldBe true
    tfOutputs.isRight shouldBe true

    tfOutputs.getOrElse(null).size shouldEqual 2
    tfOutputs.getOrElse(null).head.name shouldEqual "foo"
    tfOutputs.getOrElse(null).head.value shouldEqual "bar"

  }

  "Terraform" should "extract 0 outputs" in {

    val outputString =
      """
        |{
        |  "@level":"info",
        |  "@message":"Outputs: 0",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2023-09-04T14:49:45.862557+02:00",
        |  "outputs":{},
        |  "type":"outputs"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(0, outputString)
    val terraform     = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doApply()

    println(result.buildOutputString)
    val tfOutputs = result.terraformOutputs

    result.isSuccess shouldBe true
    tfOutputs.isRight shouldBe true

    tfOutputs.getOrElse(null).size shouldEqual 0
  }

  "Terraform" should "succeed handling outputs parsing error" in {

    val outputString =
      """
        |{
        |  "@level":"info",
        |  "@message":"Outputs: 0",
        |  "@module":"terraform.ui",
        |  "@timestamppppppp":"2023-09-04T14:49:45.862557+02:00",
        |  "outputs":{},
        |  "type":"outputs"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(0, outputString)
    val terraform     = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doApply()

    val tfOutputs = result.terraformOutputs

    result.isSuccess shouldBe true
    tfOutputs.isLeft shouldBe true

  }

  "Terraform" should "succeed handling no outputs" in {

    val mockProcessor = new MockProcessor(0, "")
    val terraform     = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doApply()
    val tfOutputs = result.terraformOutputs

    result.isSuccess shouldBe true
    tfOutputs.isRight shouldBe true
    tfOutputs.getOrElse(null).size shouldBe 0

  }

}