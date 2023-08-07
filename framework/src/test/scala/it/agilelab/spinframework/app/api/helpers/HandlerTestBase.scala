package it.agilelab.spinframework.app.api.helpers

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.http4s.{ EntityDecoder, Response, Status }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HandlerTestBase extends AnyFlatSpec with Matchers {

  implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  def check[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])(implicit
    ev: EntityDecoder[IO, A]
  ): Boolean = {
    val actualResp  = actual.unsafeRunSync()
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck   = expectedBody.fold[Boolean](
      // Verify Response's body is empty.
      actualResp.body.compile.toVector.unsafeRunSync().isEmpty
    )(expected => actualResp.as[A].unsafeRunSync() == expected)
    statusCheck && bodyCheck
  }

  def check[A](actual: IO[Response[IO]], expectedStatus: Status)(implicit ev: EntityDecoder[IO, A]): Boolean = {
    val actualResp  = actual.unsafeRunSync()
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck   = actualResp.as[A].unsafeRunSync().isInstanceOf[A]
    statusCheck && bodyCheck
  }

}
