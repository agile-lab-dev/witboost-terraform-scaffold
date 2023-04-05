package it.agilelab.spinframework.app.features.compiler

object SimpleField {
  def undefined(name: String): SimpleField                   = SimpleField(Seq(name), "", defined = false)
  def defined(name: String, value: String): SimpleField      = SimpleField(Seq(name), value, defined = true)
  def undefined(path: Seq[String]): SimpleField              = SimpleField(path, "", defined = false)
  def defined(path: Seq[String], value: String): SimpleField = SimpleField(path, value, defined = true)
}

case class SimpleField(path: Seq[String], value: String, defined: Boolean) extends Field {
  override def values: Seq[String] = Seq(value)

}
