package it.agilelab.spinframework.app.features.compiler.circe

import io.circe.{ ACursor, Json }
import it.agilelab.spinframework.app.features.compiler.Field

class CirceField(cursor: ACursor, override val path: Seq[String]) extends Field {

  private lazy val unspecified = cursor.focus.get == Json.Null // the field exists but no value is provided
  private lazy val isString    = cursor.as[String].isRight
  private lazy val asString    = cursor.as[String].getOrElse("")
  private lazy val isNumber    = cursor.as[Int].isRight
  private lazy val asNumber    = cursor.as[Int].getOrElse(0)
  private lazy val isNumberSeq = cursor.as[Seq[Int]].isRight
  private lazy val asNumberSeq = cursor.as[Seq[Int]].getOrElse(Seq.empty).map(String.valueOf)
  private lazy val isStringSeq = cursor.as[Seq[String]].isRight
  private lazy val asStringSeq = cursor.as[Seq[String]].getOrElse(Seq.empty)

  override def value: String = {
    if (undefined) return ""
    if (unspecified) return ""
    if (isString) return asString
    if (isNumber) return asString(asNumber)              // 123 -> "123"
    if (isNumberSeq) return asArrayNotation(asNumberSeq) // [1, 2] -> "[1, 2]"
    if (isStringSeq) return asArrayNotation(asStringSeq) // [a, 2] -> "[a, 2]"
    throw new IllegalStateException(s"Field $name has no string/int value. Maybe it is a parent field?")
  }

  private def asArrayNotation[T](seq: Seq[T]): String = {
    val string = seq.mkString(", ")
    s"[$string]"
  }

  override def values: Seq[String] = {
    if (undefined) return Seq.empty
    if (unspecified) return Seq.empty
    if (isNumber) return Seq(asString(asNumber)) // 123 -> Seq(123)
    if (isString) return Seq(asString)           // abc -> Seq(abc)
    if (isNumberSeq) return asNumberSeq          // [1, 2] -> Seq(1, 2)
    if (isStringSeq) return asStringSeq          // [a, 2] -> Seq("a", "2")
    throw new IllegalStateException(s"Field $name has no string/int array of values. Maybe it is a parent field?")
  }

  private def asString(number: Int): String = String.valueOf(number)

  override def toString: String = s"Field($name)"

  override def defined: Boolean = cursor.succeeded
}
