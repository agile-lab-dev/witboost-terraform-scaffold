package it.agilelab.plugin.principalsmapping.impl.azure

import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.util.UUID

class AzureMapperTest extends AnyFlatSpec with should.Matchers with IdiomaticMockito with ArgumentMatchersSugar with EitherValues {

  private val client = mock[AzureClient]
  private val mapper = new AzureMapper(client)
  private val inputUser = Set("user:name.surname_email.com")
  private val inputGroup = Set("group:dev")
  private val wrongIdentity = Set("wrong:id")

  "The mapper" should "map a Witboost user identity to an azure objectId" in {
    val userId = UUID.randomUUID().toString
    val mail = "name.surname@email.com"
    when(client.getUserId(mail)) thenReturn Right(userId)

    val res = mapper.map(inputUser)

    res.size shouldBe 1
    res.head._1 shouldEqual inputUser.head
    res.head._2.value shouldEqual userId
  }

  it should "map a Witboost group identity to an azure objectId" in {
    val groupId = UUID.randomUUID().toString
    val group = "dev"
    when(client.getGroupId(group)) thenReturn Right(groupId)

    val res = mapper.map(inputGroup)

    res.size shouldBe 1
    res.head._1 shouldEqual inputGroup.head
    res.head._2.value shouldEqual groupId
  }

  it should "return a left for a wrong identity" in {
    val res = mapper.map(wrongIdentity)

    res.size shouldBe 1
    res.head._1 shouldEqual wrongIdentity.head
    res.head._2.left.value.getMessage shouldEqual "The subject wrong:id is neither a Witboost user nor a group"
  }

}
