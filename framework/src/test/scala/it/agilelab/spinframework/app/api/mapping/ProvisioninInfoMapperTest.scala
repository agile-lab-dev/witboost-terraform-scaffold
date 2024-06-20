package it.agilelab.spinframework.app.api.mapping

import io.circe.{ Decoder, JsonObject }
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps
import it.agilelab.spinframework.app.api.mapping.ProvisioningInfoMapper.InnerJson
import it.agilelab.spinframework.app.features.compiler.TerraformOutput
import it.agilelab.spinframework.app.features.provision.{ ProvisionResult, ProvisioningStatus }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.circe._
import io.circe.parser._
import io.circe.generic.semiauto._

import scala.collection.immutable

class ProvisioninInfoMapperTest extends AnyFlatSpec with Matchers {

  implicit val innerJsonDecoder: Decoder[InnerJson] = deriveDecoder[InnerJson]

  "ProvisioningInfoMapper" should "check that no publicInfo is added" in {

    val pr: ProvisionResult = new ProvisionResult(
      ProvisioningStatus.Completed,
      null,
      Seq(),
      Seq(TerraformOutput("foo", "bar".asJson))
    )
    val res                 = ProvisioningInfoMapper.from(pr)

    res.get.publicInfo.as[Map[String, InnerJson]].getOrElse(Map[String, InnerJson]()).size shouldBe 0

  }

  "ProvisioningInfoMapper" should "check that no publicInfo is added for wrong schema" in {

    val pr: ProvisionResult = new ProvisionResult(
      ProvisioningStatus.Completed,
      null,
      Seq(),
      Seq(TerraformOutput("public_info", "bar".asJson))
    )
    val res                 = ProvisioningInfoMapper.from(pr)

    res.get.publicInfo.as[Map[String, InnerJson]].getOrElse(Map[String, InnerJson]()).size shouldBe 0

  }

  "ProvisioningInfoMapper" should "check that a publicInfo is added returning 2 out of 3 elements" in {

    val pr: ProvisionResult = new ProvisionResult(
      ProvisioningStatus.Completed,
      null,
      Seq(),
      Seq(
        TerraformOutput(
          "public_info",
          parse("""{
                  |  "aString" : {
                  |    "href" : "http://foo.bar",
                  |    "label" : "Storage Account Name",
                  |    "type" : "astring",
                  |    "value" : "/subscription/halable/foo"
                  |  },
                  |  "bString" : {
                  |    "label" : "Storage Account Name",
                  |    "type" : "bstring",
                  |    "value" : "/subscription/halable/bar"
                  |  },
                  |  "cString" : {
                  |    "label" : "Storage Account Name",
                  |    "type" : "bstring"
                  |  }
                  |}""".stripMargin).getOrElse(null)
        )
      )
    )

    val res =
      ProvisioningInfoMapper.from(pr).get.publicInfo.as[Map[String, InnerJson]].getOrElse(Map[String, InnerJson]())
    res.size shouldBe 2
    res.values.mkString(",") shouldNot include("cString")

  }

  "ProvisioningInfoMapper" should "check that no publicInfo is returned because of missing field" in {

    val pr: ProvisionResult = new ProvisionResult(
      ProvisioningStatus.Completed,
      null,
      Seq(),
      Seq(
        TerraformOutput(
          "public_info",
          parse("""{
                  |  "bString" : {
                  |    "label" : "Storage Account Name",
                  |    "value" : "/subscription/halable/bar"
                  |  }
                  |}""".stripMargin).getOrElse(null)
        )
      )
    )

    val res = ProvisioningInfoMapper.from(pr)
    res.get.publicInfo.as[Map[String, InnerJson]].getOrElse(Map[String, InnerJson]()).size shouldBe 0

  }

}
