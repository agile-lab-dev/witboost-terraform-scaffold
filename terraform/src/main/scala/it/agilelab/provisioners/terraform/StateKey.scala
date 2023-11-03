package it.agilelab.provisioners.terraform

object StateKey {
  def empty(): StateKey = StateKey("", "")
}
case class StateKey(name: String, value: String)
