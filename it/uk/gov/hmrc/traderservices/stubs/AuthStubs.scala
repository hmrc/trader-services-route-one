package uk.gov.hmrc.traderservices.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.support.WireMockSupport
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

trait AuthStubs {
  me: WireMockSupport =>

  case class Enrolment(serviceName: String, identifierName: String, identifierValue: String)

  def givenAuthorisedAsValidTrader[A](eori: String) =
    givenAuthorisedWithEnrolment(Enrolment("HMRC-CUS-ORG", "EORINumber", eori))

  def givenAuthorisedWithEnrolment[A](
    enrolment: Enrolment
  ): Unit =
    stubForAuthAuthorise(
      s"""
        |{
        |  "authorise": [
        |    { "identifiers":[], "state":"Activated", "enrolment": "${enrolment.serviceName}" },
        |    { "authProviders": ["GovernmentGateway"] }
        |  ],
        |  "retrieve":["authorisedEnrolments"]
        |}
           """.stripMargin,
      s"""
        |{
        |"authorisedEnrolments": [
        |  { "key":"${enrolment.serviceName}", "identifiers": [
        |    {"key":"${enrolment.identifierName}", "value": "${enrolment.identifierValue}"}
        |  ]}
        |]}
          """.stripMargin
    )

  def givenUnauthorisedWith(mdtpDetail: String): Unit =
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", s"""MDTP detail="$mdtpDetail"""")
        )
    )

  def stubForAuthAuthorise(payload: String, responseBody: String): Unit = {
    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(1)
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

    stubFor(
      post(urlEqualTo("/auth/authorise"))
        .atPriority(2)
        .willReturn(
          aResponse()
            .withStatus(401)
            .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
        )
    )
  }

  def verifyAuthoriseAttempt(): Unit =
    verify(1, postRequestedFor(urlEqualTo("/auth/authorise")))

}
