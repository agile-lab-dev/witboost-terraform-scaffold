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
    tfOutputs.getOrElse(null).head.value.asString.get shouldEqual "bar"

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

    val result    = terraform.doApply()
    val tfOutputs = result.terraformOutputs

    result.isSuccess shouldBe true
    tfOutputs.isRight shouldBe true
    tfOutputs.getOrElse(null).size shouldBe 0

  }

  "Terraform" should "check that the error correctly parsed" in {

    val outputString =
      """
        |{
        |  "@level":"error",
        |  "@message":"Error: No value for required variable",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2023-11-09T17:31:47.934774+01:00",
        |  "diagnostic":{"severity":"error","summary":"No value for required variable","detail":"The root module input variable \"storage_account_name\" is not set, and has no default value. Use a -var or -var-file command line argument to provide a value for this variable.","range":{"filename":"variables.tf","start":{"line":11,"column":1,"byte":206},"end":{"line":11,"column":32,"byte":237}},"snippet":{"context":null,"code":"variable \"storage_account_name\" {","start_line":11,"highlight_start_offset":0,"highlight_end_offset":31,"values":[]}},
        |  "type":"diagnostic"
        |}
        |""".stripMargin.replace("\n", "")

    val mockProcessor = new MockProcessor(1, outputString)

    val terraform = Terraform()
      .outputInJson()
      .processor(mockProcessor)
      .onDirectory("folder")

    val result = terraform.doDestroy(noVariable())

    result.isSuccess shouldBe false
    result.errorMessages.size shouldBe 1
    result.errorMessages.head should startWith("Message: [Error: No value for required variable]")

  }

  "Terraform" should "return an error message when can't access attributes" in {

    val logs =
      """
        |{
        |  "@level":"error",
        |  "@message":"Error: Unsupported attribute",
        |  "@module":"terraform.ui",
        |  "@timestamp":"2024-05-29T09:30:11.285252+02:00",
        |  "diagnostic":{"severity":"error","summary":"Unsupported attribute","detail":"Can't access attributes on a primitive-typed value (string).","range":{"filename":"...","start":{"line":88,"column":67,"byte":4280},"end":{"line":88,"column":70,"byte":4283}},"snippet":{"context":"...","code":"...","start_line":88,"highlight_start_offset":66,"highlight_end_offset":69,"values":[{"traversal":"var.data_product__name","statement":"is a string"}]}},
        |  "type":"diagnostic"
        |}
        |""".stripMargin.replace("\n", "")

    val terraform = Terraform()
      .outputInJson()
      .processor(new MockProcessor(1, logs))
      .onDirectory("folder")

    val result = terraform.doApply()

    result.isSuccess shouldBe false
    result.errorMessages.size shouldBe 1
    result.errorMessages.head should startWith(
      "Message: [Error: Unsupported attribute]"
    )

  }

  "Terraform" should "return an error message when can't access attributes 2" in {

    val logs =
      """
        |{
        |  "@level": "error",
        |  "@message": "Error: waiting for Synapse Workspace ...",
        |  "@module": "terraform.ui",
        |  "@timestamp": "2024-05-29T12:08:34.186712Z",
        |  "diagnostic": {
        |    "severity": "error",
        |    "summary": "waiting for Synapse Workspace ...",
        |    "detail": ""
        |  },
        |  "type": "diagnostic"
        |}
        |""".stripMargin.replace("\n", "")

    val terraform = Terraform()
      .outputInJson()
      .processor(new MockProcessor(1, logs))
      .onDirectory("folder")

    val result = terraform.doApply()

    result.isSuccess shouldBe false
    result.errorMessages.size shouldBe 1
    result.errorMessages.head should startWith(
      "Message: [Error: waiting for Synapse Workspace ...]"
    )

  }

}
