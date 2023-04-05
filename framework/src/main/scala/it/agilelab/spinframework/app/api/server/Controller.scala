package it.agilelab.spinframework.app.api.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Directives, PathMatcher, StandardRoute }
import it.agilelab.spinframework.app.api.dtos.{ SystemErrorDto, SystemErrorDtoJsonFormat }

private[app] object Controller extends Directives {
  private val base    = "datamesh.specificprovisioner"
  private val version = "1.0.0"

  val basePathMatcher: PathMatcher[Unit] = base / version
  val basePath: String                   = s"$base/$version"
}

trait Controller extends Directives with SystemErrorDtoJsonFormat {

  // TODO: why this base path?
  protected val basePath: PathMatcher[Unit] = Controller.basePathMatcher

  def catchSystemErrors(requestHandler: => StandardRoute): StandardRoute =
    try requestHandler.apply
    catch {
      case exc: Throwable => complete(StatusCodes.InternalServerError, SystemErrorDto(throwableToString(exc)))
    }

  private def throwableToString(exc: Throwable): String = {
    import java.io.{ PrintWriter, StringWriter }
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    exc.printStackTrace(pw)
    sw.toString
  }
}
