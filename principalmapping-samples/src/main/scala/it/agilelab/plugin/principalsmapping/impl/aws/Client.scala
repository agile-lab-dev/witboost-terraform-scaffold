package it.agilelab.plugin.principalsmapping.impl.aws

import com.typesafe.config.{Config, ConfigFactory}
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.iam.IamClient

import scala.util.Try

trait Client {

  /**
   * This method tries to build an [[IamClient]]
   * @param config the configuration that might be used to build the client
   * @return an [[IamClient]] in case of Success, or a Failure if a non-fatal exception is thrown
   */
  def getClient(config: Config): Try[IamClient]
}

object Client extends Client {

  /**
   * Tries to create an IamClient. using the `WebIdentityTokenFileCredentialsProvider`.
   * As k8s is default choice for running specific provisioners, we use `WebIdentityTokenFileCredentialsProvider` as default CredentialsProvider, which means the authentication is demanded to the serviceAccount bound the pod.
   * @param config the configuration that might be used to build the client
   * @return an [[IamClient]] in case of Success, or a Failure if a non-fatal exception is thrown
   */
  override def getClient(config : Config = ConfigFactory.empty()) : Try[IamClient] = {

    val region = Region.AWS_GLOBAL

    /*
    Another, discouraged, approach would be to use the `SystemPropertyCredentialsProvider` and provide technical user access keys.
    ```
    System.setProperty("aws.accessKeyId", "access-key-id")
    System.setProperty("aws.secretAccessKey", "secret-access-key")
    val provider = SystemPropertyCredentialsProvider.create()
    ```
     */
    val provider = WebIdentityTokenFileCredentialsProvider.create()

    Try {
      IamClient.builder.region(region).credentialsProvider(provider).build
    }
  }
}
