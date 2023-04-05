package it.agilelab.spinframework.app.features.support.test

import com.google.gson.Gson
import requests.Response

class LocalHttpClient(val port: Int) {
  // WARNING: interface MUST BE "localhost", otherwise tests on Jenkins will break!
  private val interface = "localhost"
  private val baseUrl   = s"http://$interface:$port"
  private val version   = "1.0.0"
  private val prefix    = s"/datamesh.specificprovisioner/$version"
  private val gson      = new Gson()

  def post[T](endpoint: String, request: AnyRef, bodyClass: Class[T]): HttpResponse[T] = {
    val completeUrl = url(endpoint)
    val jsonRequest = gson.toJson(request)
    println(s"POST $completeUrl $jsonRequest")

    val response: Response =
      requests.post(url = completeUrl, headers = applicationJson, data = jsonRequest, readTimeout = 1000000)

    println(s"${response.statusCode} ${response.text()}")
    buildHttpResponse(response, bodyClass)
  }

  def get[T](endpoint: String, bodyClass: Class[T]): HttpResponse[T] = {
    val completeUrl = url(endpoint)
    println(s"GET $completeUrl")

    val response: Response = requests.get(completeUrl)

    println(s"${response.statusCode} ${response.text()}")
    buildHttpResponse(response, bodyClass)
  }

  private def buildHttpResponse[T](response: Response, bodyClass: Class[T]): HttpResponse[T] =
    HttpResponse(response.statusCode, gson.fromJson(response.text(), bodyClass))

  private def url(endpoint: String) =
    s"$baseUrl$prefix$endpoint"

  private def applicationJson: Map[String, String] = Map("Content-Type" -> "application/json")

}
