package it.agilelab.spinframework.app.api.helpers

import akka.http.scaladsl.testkit.ScalatestRouteTest
import it.agilelab.spinframework.app.api.dtos.{ SystemErrorDtoJsonFormat, ValidationErrorDto }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait ControllerTestBase extends AnyFlatSpec with Matchers with ScalatestRouteTest with SystemErrorDtoJsonFormat {
  def endpoint(finalEndpoint: String): String = {
    val base    = "/datamesh.specificprovisioner"
    val version = "/1.0.0"
    s"$base$version$finalEndpoint"
  }

  protected def fromStrings(errors: Seq[String]): ValidationErrorDto = ValidationErrorDto(errors.toArray)
}
