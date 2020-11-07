package uk.gov.hmrc.traderservices.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait CreateCaseStubs {
  me: WireMockSupport =>

  def givenPegaCreateCaseRequestSucceeds(): Unit =
    stubForPostWithResponse(
      """{
        |  "ApplicationType" : "Route1",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {
        |    "EntryType":"Import",
        |    "RequestType":"New",
        |    "EntryProcessingUnit":"002",
        |    "Route":"Route 1",
        |    "EntryNumber":"A23456A",
        |    "VesselName":"Vessel Name",
        |    "EntryDate":"20200902",
        |    "VesselEstimatedDate":"20201029",
        |    "VesselEstimatedTime":"234500",
        |    "FreightOption":"Maritime",
        |    "EORI":"GB123456789012345",
        |    "TelephoneNumber":"07123456789",
        |    "EmailAddress":"sampelname@gmail.com"
        |    }
        |}""".stripMargin,
      """{
        |    "Status": "Success",
        |    "StatusText": "Case created successfully",
        |    "CaseID": "PCE201103470D2CC8K0NH3",
        |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
        |}""".stripMargin
    )

  def stubForPostWithResponse(payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo("/cpr/caserequest/route1/create/v1"))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("x-forwarded-host", matching("[A-Za-z0-9.-]{1,255}"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", matching("Bearer \\w{1,1024}"))
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

}
