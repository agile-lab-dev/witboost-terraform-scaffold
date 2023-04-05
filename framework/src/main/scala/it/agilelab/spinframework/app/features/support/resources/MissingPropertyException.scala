package it.agilelab.spinframework.app.features.support.resources

case class MissingPropertyException(name: String = "") extends RuntimeException(s"Missing property: $name")
