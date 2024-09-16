package it.agilelab.spinframework.app.features.support.test

import com.google.gson._
import io.circe.{ parser, Json }
import it.agilelab.spinframework.app.api.generated.definitions.ProvisioningStatus.{ Status => PS }
import it.agilelab.spinframework.app.api.generated.definitions.ReverseProvisioningStatus.{ Status => RPS }
import it.agilelab.spinframework.app.api.generated.definitions.{ DescriptorKind, Log, ValidationError }
import requests.Response

import java.lang.reflect.{ ParameterizedType, Type }

class LocalHttpClient(val port: Int, val pref: Option[String] = None) {
  // WARNING: interface MUST BE "localhost", otherwise tests on Jenkins will break!
  private val interface   = "localhost"
  private val baseUrl     = s"http://$interface:$port"
  private val prefix      = pref.getOrElse(s"/datamesh.specificprovisioner")
  private val gsonBuilder = new GsonBuilder()
  gsonBuilder.registerTypeAdapter(classOf[PS], new StatusDeserializer)
  gsonBuilder.registerTypeAdapter(classOf[RPS], new ReverseProvisioningStatusDeserializer)
  gsonBuilder.registerTypeAdapter(classOf[DescriptorKind], new DescriptorKindSerializer)
  gsonBuilder.registerTypeAdapter(classOf[Option[Any]], new OptionSerializer)
  gsonBuilder.registerTypeAdapter(classOf[Vector[String]], new VectorSerializer)
  gsonBuilder.registerTypeAdapter(classOf[Vector[String]], new VectorDeserializer)
  gsonBuilder.registerTypeAdapter(classOf[io.circe.Json], new CirceJsonDeserializer)
  gsonBuilder.registerTypeAdapter(classOf[io.circe.Json], new CirceJsonSerializer)
  gsonBuilder.registerTypeAdapter(classOf[Option[Any]], new OptionDeserializer)

  private val gson = gsonBuilder.create

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

class ReverseProvisioningStatusDeserializer extends JsonDeserializer[RPS] {
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): RPS =
    RPS.from(json.getAsString).get
}

class StatusDeserializer extends JsonDeserializer[PS] {
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PS =
    PS.from(json.getAsString).get
}

class DescriptorKindSerializer extends JsonSerializer[DescriptorKind] {
  override def serialize(src: DescriptorKind, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
    new JsonPrimitive(src.value)
}

class OptionSerializer extends JsonSerializer[Option[Any]] {
  override def serialize(src: Option[Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
    src match {
      case Some(value) => context.serialize(value)
      case None        => JsonNull.INSTANCE
    }
}

class OptionDeserializer extends JsonDeserializer[Option[_]] {
  def innerType(outerType: Type)                                                                             = outerType match {
    case pt: ParameterizedType => pt.getActualTypeArguments()(0)
    case _                     => throw new UnsupportedOperationException(outerType.toString)
  }
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Option[_] =
    json match {
      case null                 => None
      case _ if json.isJsonNull => None
      case _                    => Some(context.deserialize(json, innerType(typeOfT)))
    }
}

class VectorSerializer extends JsonSerializer[Vector[String]] {
  override def serialize(src: Vector[String], typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
    context.serialize(src.toArray)
}

class VectorDeserializer extends JsonDeserializer[Vector[String]] {
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Vector[String] =
    new Gson().fromJson(json.getAsJsonArray, classOf[Array[String]]).toVector
}

class CirceJsonDeserializer extends JsonDeserializer[io.circe.Json] {
  override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): io.circe.Json =
    parser.parse(json.getAsString).getOrElse(Json.Null)
}

class CirceJsonSerializer extends JsonSerializer[io.circe.Json] {
  override def serialize(src: Json, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
    context.serialize(src.toString())
}
