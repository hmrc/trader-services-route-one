package uk.gov.hmrc.traderservices.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait UpdateCaseStubs {
  me: WireMockSupport =>

  val UPDATE_CASE_URL = "/cpr/caserequest/route1/update/v1"

  def givenPegaUpdateCaseRequestSucceeds(description: String = "An example description."): Unit =
    stubForPostWithResponse(
      200,
      s"""{
         |  "ApplicationType" : "Route1",
         |  "OriginatingSystem" : "Digital",
         |  "Content": {
         |    "RequestType": "Additional Information",
         |    "CaseID": "PCE201103470D2CC8K0NH3",
         |    "Description": "$description"
         |    }
         |}""".stripMargin,
      """{
        |    "Status": "Success",
        |    "StatusText": "Case Updated successfully",
        |    "CaseID": "PCE201103470D2CC8K0NH3",
        |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
        |}""".stripMargin
    )

  def verifyPegaUpdateCaseRequestHasHappened() =
    verify(1, postRequestedFor(urlEqualTo(UPDATE_CASE_URL)))

  def verifyPegaUpdateCaseRequestHasHappened(requestType: String, caseId: String, description: String): Unit =
    verify(
      1,
      postRequestedFor(urlEqualTo(UPDATE_CASE_URL))
        .withRequestBody(
          equalToJson(
            s"""{
               |  "ApplicationType" : "Route1",
               |  "OriginatingSystem" : "Digital",
               |  "Content": {
               |    "RequestType": "$requestType",
               |    "CaseID": "$caseId",
               |    "Description": "$description"
               |}}""".stripMargin,
            true,
            true
          )
        )
    )

  def verifyPegaUpdateCaseRequestDidNotHappen() =
    verify(0, postRequestedFor(urlEqualTo(UPDATE_CASE_URL)))

  def givenPegaUpdateCaseRequestFails(status: Int, errorCode: String, errorMessage: String = ""): Unit =
    stubForPostWithResponse(
      status,
      """{
        |  "ApplicationType" : "Route1",
        |  "OriginatingSystem" : "Digital",
        |  "Content": {}
        |}""".stripMargin,
      s"""{"errorDetail":{
         |   "timestamp": "2020-11-03T15:29:28.601Z", 
         |   "correlationId": "123123123", 
         |   "errorCode": "$errorCode"
         |   ${if (errorMessage.nonEmpty) s""","errorMessage": "$errorMessage"""" else ""}
         |}}""".stripMargin
    )

  private def stubForPostWithResponse(status: Int, payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("CustomProcessesHost", equalTo("Digital"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", equalTo("Bearer dummy-it-token"))
        .withHeader("environment", equalTo("it"))
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

  def givenPegaUpdateCaseRequestRespondsWithHtml(): Unit =
    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("CustomProcessesHost", equalTo("Digital"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", equalTo("Bearer dummy-it-token"))
        .withHeader("environment", equalTo("it"))
        .willReturn(
          aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "text/html")
            .withBody(
              """<html>\r\n<head><title>400 Bad Request</title></head>\r\n<body bgcolor=\"white\">\r\n<center><h1>400 Bad Request</h1></center>\r\n<hr><center>nginx</center>\r\n</body>\r\n</html>\r\n\"""
            )
        )
    )

  def givenPegaUpdateCaseRequestRespondsWith403WithoutContent(): Unit =
    stubFor(
      post(urlEqualTo(UPDATE_CASE_URL))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("CustomProcessesHost", equalTo("Digital"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", equalTo("Bearer dummy-it-token"))
        .withHeader("environment", equalTo("it"))
        .willReturn(
          aResponse()
            .withStatus(403)
        )
    )

}
