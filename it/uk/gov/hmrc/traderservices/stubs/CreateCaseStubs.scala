package uk.gov.hmrc.traderservices.stubs

import java.time.format.DateTimeFormatter

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.models.CreateImportCaseRequest
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait CreateCaseStubs {
  me: WireMockSupport =>

  def givenImportCreateRequest[A](
    importCaseRequest: CreateImportCaseRequest,
    eori: String
  ): Unit = {
    val answers = importCaseRequest.importQuestionsAnswers
    val declarationDetails = importCaseRequest.declarationDetails
    val date = declarationDetails.entryDate
    stubForPostWithResponse(
      s"""
         |{
         |"ApplicationType" : "Route1",
         |"OriginatingSystem" : "Digital",
         |"AcknowledgementReference" : "1234",
         |"Content": {
         |  "IsALVS":"${answers.hasALVS.getOrElse(false)}",
         |  "EntryDate":"${date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}",
         |  "EntryNumber":"${declarationDetails.entryNumber.value}",
         |  "EntryProcessingUnit":"${declarationDetails.epu.value}",
         |  "EntryType":"Import",
         |  "FreightOption":"${answers.freightType.get}",
         |  "RequestType":"${answers.requestType}",
         |  "Route":"${answers.routeType}",
         |  "EORI":"$eori",
         |  "TelephoneNumber":"${answers.contactInfo.contactNumber.get}",
         |  "EmailAddress":"${answers.contactInfo.contactEmail}"
         |  }
         |}
           """.stripMargin,
      s"""
         |{
         |"CaseID": "Risk-363",
         |"ProcessingDate": "2020-08-24T09:16:10.047Z",
         |"Status": "Success",
         |"StatusText": "Case created successfully"
         |}
          """.stripMargin
    )
  }

  def stubForPostWithResponse(payload: String, responseBody: String): Unit =
    stubFor(
      post(urlEqualTo("/v1/create-case"))
        .atPriority(1)
        .withRequestBody(equalToJson(payload, true, true))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody)
        )
    )

}
