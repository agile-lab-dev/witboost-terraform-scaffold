package it.agilelab.spinframework.app.features.compiler

/** This trait provides an easy access to the input descriptor.
  *
  * The descriptor has a tree structure, since it is the result
  * the yaml file parsing.
  *
  * Each field is identified by its name.
  */
trait ComponentDescriptor {

  /** If the field is a sub-tree, the [[sub]] method allow you to move down on it.
    *
    * @param fieldName an identifier of a field
    * @return the sub-tree matching the field name
    */
  def sub(fieldName: String): ComponentDescriptor

  /** If the field is a leaf, the [[field]] method provides you its content.
    *
    * @param fieldName an identifier of a field
    * @return the specified field
    */
  def field(fieldName: String): Field

  /** Checks whether the underlying cursor represents the result of a successful operation.
    *
    * @return a boolean
    */
  def succeeded: Boolean
}
