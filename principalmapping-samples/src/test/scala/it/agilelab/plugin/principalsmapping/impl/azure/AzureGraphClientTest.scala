package it.agilelab.plugin.principalsmapping.impl.azure

import com.azure.identity.ClientSecretCredentialBuilder
import com.microsoft.graph.models.{Group, User}
import com.microsoft.graph.serviceclient.GraphServiceClient
import com.microsoft.kiota.ApiException
import org.mockito.DefaultAnswers.ReturnsDeepStubs
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import java.util.UUID
import scala.jdk.CollectionConverters._

class AzureGraphClientTest extends AnyFlatSpec with should.Matchers with EitherValues with IdiomaticMockito with ArgumentMatchersSugar {

  private val graphServiceClient = mock[GraphServiceClient](ReturnsDeepStubs)
  private val azureClient = new AzureGraphClient(graphServiceClient)
  private val azureUser = mock[User]
  private val azureGroup = mock[Group]

  "The client" should "map a mail to an azure objectId" in {
    val mail = "name.surname@email.com"
    val userId = UUID.randomUUID().toString
    when(graphServiceClient.users().get(*).getValue) thenReturn List(azureUser).asJava
    when(azureUser.getId) thenReturn userId

    val res = azureClient.getUserId(mail)

    res.value shouldEqual userId
  }

  it should "return a left it the user is not found" in {
    val mail = "not.existing@email.com"
    when (graphServiceClient.users().get(*).getValue) thenReturn List.empty.asJava
    val expectedError = s"User $mail not found on the configured Azure tenant"

    val res = azureClient.getUserId(mail)

    res.left.value.getMessage shouldEqual expectedError
  }

  it should "return a left it there was an exception while searching for users" in {
    val mail = "name.surname@email.com"
    val error = s"Unexpected error"
    val expectedException = new ApiException(error)
    when (graphServiceClient.users().get(*).getValue) thenThrow expectedException

    val res = azureClient.getUserId(mail)

    res.left.value shouldEqual expectedException
  }

  it should "map a group to an azure objectId" in {
    val group = "dev"
    val groupId = UUID.randomUUID().toString
    when(graphServiceClient.groups().get(*).getValue) thenReturn List(azureGroup).asJava
    when(azureGroup.getId) thenReturn groupId

    val res = azureClient.getGroupId(group)

    res.value shouldEqual groupId
  }

  it should "return a left it the group is not found" in {
    val group = "notexisting"
    when (graphServiceClient.groups().get(*).getValue) thenReturn List.empty.asJava
    val expectedError = s"Group $group not found on the configured Azure tenant"

    val res = azureClient.getGroupId(group)

    res.left.value.getMessage shouldEqual expectedError
  }

  it should "return a left it there was an exception while searching for groups" in {
    val group = "dev"
    val error = s"Unexpected error"
    val expectedException = new ApiException(error)
    when (graphServiceClient.groups().get(*).getValue) thenThrow expectedException

    val res = azureClient.getGroupId(group)

    res.left.value shouldEqual expectedException
  }

}
