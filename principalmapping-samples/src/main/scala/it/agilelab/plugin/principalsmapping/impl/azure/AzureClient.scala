package it.agilelab.plugin.principalsmapping.impl.azure

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.serviceclient.GraphServiceClient
import com.typesafe.config.Config
import scala.util.Try

/**
  * Azure mapping functions
  */
trait AzureClient {
  /**
    * Retrieve the corresponding Azure objectId for the given mail address
    * @param mail user mail address
    * @return either an error or the corresponding objectId
    */
  def getUserId(mail: String): Either[Throwable, String]

  /**
    * Retrieve the corresponding Azure objectId for the given group name
    * @param group group name
    * @return either an error or the corresponding objectId
    */
  def getGroupId(group: String): Either[Throwable, String]
}

object AzureClient  {
  /**
    * Build a concrete instance of an AzureClient
    * @param config config for the service principal
    * @return a Try containing an AzureGraphClient or an error
    */
  def getClient(config: Config): Try[AzureClient] = Try {
    val clientId = config.getString("clientId")
    val tenantId = config.getString("tenantId")
    val clientSecret = config.getString("clientSecret")

    // The client credentials flow requires that you request the
    // /.default scope, and pre-configure your permissions on the
    // app registration in Azure. An administrator must grant consent
    // to those permissions beforehand.
    val scopes = Array[String]("https://graph.microsoft.com/.default")

    val credential = new ClientSecretCredentialBuilder().clientId(clientId).tenantId(tenantId).clientSecret(clientSecret).build
    new AzureGraphClient(new GraphServiceClient(credential, scopes:_*))
  }
}
