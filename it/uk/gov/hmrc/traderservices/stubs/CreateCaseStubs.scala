package uk.gov.hmrc.traderservices.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait CreateCaseStubs {
  me: WireMockSupport =>

  val CREATE_CASE_URL = "/cpr/caserequest/route1/create/v1"

  val validResponseBody =
    """{
      |    "Status": "Success",
      |    "StatusText": "Case created successfully",
      |    "CaseID": "PCE201103470D2CC8K0NH3",
      |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
      |}""".stripMargin

  val validCreateImportCasePayload =
    """{
      |  "ApplicationType" : "Route1",
      |  "OriginatingSystem" : "Digital",
      |  "Content": {
      |    "EntryType":"Import",
      |    "RequestType":"New",
      |    "EntryProcessingUnit":"002",
      |    "Route":"Route 1",
      |    "EntryNumber":"223456A",
      |    "VesselName":"Vessel Name",
      |    "EntryDate":"20200902",
      |    "VesselEstimatedDate":"20201029",
      |    "VesselEstimatedTime":"234500",
      |    "FreightOption":"Maritime",
      |    "TelephoneNumber":"07123456789",
      |    "EmailAddress":"sampelname@gmail.com"
      |    }
      |}""".stripMargin

  val validCreateExportCasePayload =
    """{
      |  "ApplicationType" : "Route1",
      |  "OriginatingSystem" : "Digital",
      |  "Content": {
      |    "EntryType":"Export",
      |    "RequestType":"New",
      |    "EntryProcessingUnit":"002",
      |    "Route":"Route 1",
      |    "EntryNumber":"A23456A",
      |    "VesselName":"Vessel Name",
      |    "EntryDate":"20200902",
      |    "VesselEstimatedDate":"20201029",
      |    "VesselEstimatedTime":"234500",
      |    "FreightOption":"Maritime",
      |    "TelephoneNumber":"07123456789",
      |    "EmailAddress":"sampelname@gmail.com",
      |    "Priority" : "Live animals"
      |    }
      |}""".stripMargin

  def givenPegaCreateImportCaseRequestSucceeds(status: Int): Unit =
    stubForPostWithResponse(status, validCreateImportCasePayload, validResponseBody)

  def givenPegaCreateExportCaseRequestSucceeds(): Unit =
    stubForPostWithResponse(200, validCreateExportCasePayload, validResponseBody)

  def verifyPegaCreateCaseRequestHasHappened(times: Int = 1) =
    verify(times, postRequestedFor(urlEqualTo(CREATE_CASE_URL)))

  def verifyPegaCreateCaseRequestDidNotHappen() =
    verify(0, postRequestedFor(urlEqualTo(CREATE_CASE_URL)))

  def givenPegaCreateCaseRequestFails(status: Int, errorCode: String, errorMessage: String = ""): Unit =
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
      post(urlEqualTo(CREATE_CASE_URL))
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

  def givenPegaCreateCaseRequestRespondsWithHtml(): Unit =
    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
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

  def givenPegaCreateCaseRequestRespondsWith403WithoutContent(): Unit =
    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
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

  def givenPegaCreateImportCaseRespondsWithoutContentType(status: Int): Unit =
    stubFor(
      post(urlEqualTo(CREATE_CASE_URL))
        .withHeader("x-correlation-id", matching("[A-Za-z0-9-]{36}"))
        .withHeader("CustomProcessesHost", equalTo("Digital"))
        .withHeader("date", matching("[A-Za-z0-9,: ]{29}"))
        .withHeader("accept", equalTo("application/json"))
        .withHeader("content-Type", equalTo("application/json"))
        .withHeader("authorization", equalTo("Bearer dummy-it-token"))
        .withHeader("environment", equalTo("it"))
        .withRequestBody(equalToJson(validCreateImportCasePayload, true, true))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(validResponseBody)
        )
    )

  def givenPegaCreateImportRespondsWithInvalidSuccessMessage(): Unit =
    stubForPostWithResponse(
      200,
      validCreateImportCasePayload,
      """{
        |    "Success": true,
        |    "StatusText": "Case created successfully",
        |    "CaseId": "PCE201103470D2CC8K0NH3",
        |    "ProcessingDate": "2020-11-03T15:29:28.601Z"
        |}""".stripMargin
    )

  def givenPegaCreateImportRespondsWithInvalidErrorMessage(): Unit =
    stubForPostWithResponse(
      403,
      validCreateImportCasePayload,
      s"""{
         |   "timestamp": "2020-11-03T15:29:28.601Z", 
         |   "correlationId": "123123123", 
         |   "errorCode": "404",
         |   "errorMessage": "foo"
         |}""".stripMargin
    )
}
