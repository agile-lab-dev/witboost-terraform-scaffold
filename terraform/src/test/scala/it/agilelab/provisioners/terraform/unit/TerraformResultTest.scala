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

}
