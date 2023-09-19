package it.agilelab.plugin.principalsmapping.impl.aws

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.MockitoSugar.{mock, spy, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import software.amazon.awssdk.services.iam.IamClient

class AwsIamMapperTest extends AnyFlatSpec with should.Matchers  {

  val iamMapping = spy(new IamMapper(null))
  val client = mock[IamClient]
  val inputUser = Set("user:jon.doe@foo.bar")
  val inputGroup = Set("group:devs")


  "AwsIamMapper" should "return the arn of the user" in {

    when (iamMapping.getUserByUsername(anyString(), any())) thenReturn( Right("arn::account:user:devs"))

    val res = iamMapping.map(inputUser)
    res.size shouldBe 1
    res.head._2.getOrElse(null) shouldEqual "arn::account:user:devs"

  }

  "AwsIamMapper" should "return the arn of the group" in {

    when (iamMapping.getGroupByName(anyString(), any())) thenReturn( Right("arn::account:group:devs"))

    val res = iamMapping.map(inputGroup)

    res.size shouldBe 1
    res.head._2.getOrElse(null) shouldEqual "arn::account:group:devs"

  }

  "AwsIamMapper" should "return one empty item for wrong type" in {

    val inputWrongUser = Set("duck:jon.doe@foo.bar")

    val res = iamMapping.map(inputWrongUser)

    res.size shouldBe 1
    res.head._2.isLeft shouldBe true

  }


}
