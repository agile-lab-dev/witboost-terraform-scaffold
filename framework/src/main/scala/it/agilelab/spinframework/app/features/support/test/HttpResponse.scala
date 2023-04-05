package it.agilelab.spinframework.app.features.support.test

case class HttpResponse[T](status: Int, body: T)
