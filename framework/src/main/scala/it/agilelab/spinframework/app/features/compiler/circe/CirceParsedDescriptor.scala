package it.agilelab.spinframework.app.features.compiler.circe

import io.circe.{ ACursor, Json }
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, Field }

object CirceParsedDescriptor {
  def apply(json: Json) = new CirceParsedDescriptor(json.hcursor, Seq.empty)
}

class CirceParsedDescriptor(cursor: ACursor, path: Seq[String]) extends ComponentDescriptor {
  override def sub(fieldName: String): ComponentDescriptor = new CirceParsedDescriptor(
    cursor.downField(fieldName),
    path :+ fieldName
  )

  override def field(fieldName: String): Field =
    new CirceField(cursor.downField(fieldName), path :+ fieldName)

  override def toString: String = cursor.focus.getOrElse(Json.Null).toString()

  override def succeeded: Boolean =
    this.cursor.succeeded

}
