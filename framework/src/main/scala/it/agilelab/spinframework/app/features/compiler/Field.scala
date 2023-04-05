package it.agilelab.spinframework.app.features.compiler

/** *
  * This trait represents a field within the descriptor.
  * A field is a name-value pair.
  *
  * When you get a field from the descriptor, it is 'defined' it
  * the descriptor actually has a field with the provided name.
  *
  * On a defined field, the 'value' method returns the value if the field is a string.
  * The 'values' methods return a sequence of values if the field is a string array.
  */
trait Field {

  /** @return the extended path of this field in a descriptor
    */
  def path: Seq[String]

  /** @return hierarchical name of the field
    */
  final def name: String = path.mkString(".")

  /** @return the value of the field if defined and of type string,
    *          the corresponding string if it is a number,
    *         an empty string if it is of another type
    */
  def value: String

  /** @return the values' sequence if the field is a defined array of string, an empty Seq otherwise
    */
  def values: Seq[String]

  /** @return true if the field exists in the descriptor
    */
  def defined: Boolean

  /** @return true if the field does not exist
    */
  final def undefined: Boolean = !defined

  /** @return true if the trimmed value of the field is an empty string
    */
  final def empty: Boolean = defined && (value.trim.isEmpty || values.isEmpty)

  /** @return true if the trimmed value of the field is a non empty string
    */
  final def nonEmpty: Boolean = !empty

  /** @param defaultValue  a default value to return if the field is undefined or empty
    * @return the value of the field if defined and non empty, default otherwise
    */
  final def valueOrElse(defaultValue: String): String = if (undefined || empty) defaultValue else value
}
