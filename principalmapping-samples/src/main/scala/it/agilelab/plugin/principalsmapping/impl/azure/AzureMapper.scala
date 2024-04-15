package it.agilelab.plugin.principalsmapping.impl.azure

import it.agilelab.plugin.principalsmapping.api.Mapper
import org.slf4j.{Logger, LoggerFactory}

class AzureMapper(client: AzureClient) extends Mapper {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def map(subjects: Set[String]): Map[String, Either[Throwable, String]] =
    subjects.map {
        case ref @ s"user:$user" => ref -> getAndMapUser(user)
        case ref @ s"group:$group" => ref -> getAndMapGroup(group)
        case ref =>
          val errorMessage = s"The subject $ref is neither a Witboost user nor a group"
          logger.error(errorMessage)
          ref -> Left(new Throwable(errorMessage))
      }.toMap

  private def getAndMapUser(user: String): Either[Throwable, String] = {
    val underscoreIndex = user.lastIndexOf("_")
    val mail          = if (underscoreIndex.equals(-1)) user else user.substring(0, underscoreIndex) + "@" + user.substring(underscoreIndex + 1)
    client.getUserId(mail)
  }

  private def getAndMapGroup(group: String): Either[Throwable, String] = {
    client.getGroupId(group)
  }

}
