package it.agilelab.spinframework.app.features.compiler

import io.circe.syntax.EncoderOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ReverseChangesTest extends AnyFlatSpec with should.Matchers {

  "ReverseChanges" should "be correctly encoded" in {

    val changes = ReverseChanges(
      Seq(
        ImportBlock(id = "ex1", to = "val1"),
        ImportBlock(id = "ex2", to = "val2")
      ),
      skipSafetyChecks = false
    )

    changes
      .asJson(ReverseChanges.customEncoder)
      .findAllByKey("spec.mesh.specific.reverse.imports")
      .head
      .asArray
      .get
      .size shouldBe 2

  }
}
