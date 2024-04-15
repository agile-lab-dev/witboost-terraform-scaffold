package it.agilelab.plugin.principalsmapping.impl.azure

import com.microsoft.graph.serviceclient.GraphServiceClient
import org.slf4j.{Logger, LoggerFactory}
import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._

class AzureGraphClient(graphServiceClient: GraphServiceClient) extends AzureClient {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def getUserId(mail: String): Either[Throwable, String] = {
    Try(graphServiceClient.users().get(_.queryParameters.filter = s"mail eq '$mail'").getValue.asScala.headOption) match {
      case Failure(exception) =>
        logger.error("Exception in getUserId", exception)
        Left(exception)
      case Success(Some(user)) => Right(user.getId)
      case Success(None) =>
        val errorMessage = s"User $mail not found on the configured Azure tenant"
        logger.error(errorMessage)
        Left(new Throwable(errorMessage))
    }
  }

  override def getGroupId(group: String): Either[Throwable, String] = {
    Try(graphServiceClient.groups().get(_.queryParameters.filter = s"displayName eq '$group'").getValue.asScala.headOption) match {
      case Failure(exception) =>
        logger.error("Exception in getGroupId", exception)
        Left(exception)
      case Success(Some(group)) => Right(group.getId)
      case Success(None) =>
        val errorMessage = s"Group $group not found on the configured Azure tenant"
        logger.error(errorMessage)
        Left(new Throwable(errorMessage))
    }
  }
}
