package uk.gov.hmrc.traderservices.stubs

import java.time.format.DateTimeFormatter

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.controllers.TraderServicesCreateCaseRequest
import uk.gov.hmrc.traderservices.models.{CreateImportCaseRequest, ExportQuestions, ImportQuestions}
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait CreateCaseStubs {
  me: WireMockSupport =>

  def givenImportCreateRequestWithVesselDetails[A](
    importCaseRequest: TraderServicesCreateCaseRequest,
    eori: String
  ): Unit = {
    val declarationDetails = importCaseRequest.declarationDetails
    val date = declarationDetails.entryDate
    importCaseRequest.questionsAnswers match {
      case ImportQuestions(
            requestType,
            routeType,
            _,
            priorityGoods,
            hasALVS,
            freightType,
            vesselDetails,
            contactInfo
          ) =>
        stubForPostWithResponse(
          s"""
             |{
             |"ApplicationType" : "Route1",
             |"OriginatingSystem" : "Digital",
             |"AcknowledgementReference" : "1234",
             |"Content": {
             |  "IsALVS":"${hasALVS.get}",
             |  "EntryDate":"${date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}",
             |  "EntryNumber":"${declarationDetails.entryNumber.value}",
             |  "EntryProcessingUnit":"${declarationDetails.epu.value}",
             |  "EntryType":"Import",
             |  "FreightOption":"${freightType.get}",
             |  "RequestType":"${requestType.get}",
             |  "Route":"${routeType.get}",
             |  "EORI":"$eori",
             |  "TelephoneNumber":"${contactInfo.get.contactNumber.get}",
             |  "EmailAddress":"${contactInfo.get.contactEmail}",
             |  "Priority":"${priorityGoods.get}",
             |  "VesselName":"${vesselDetails.get.vesselName.get}",
             |  "VesselEstimatedDate":"${vesselDetails.get.dateOfArrival.get}",
             |  "VesselEstimatedTime":"${vesselDetails.get.timeOfArrival.get}"
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
      case ExportQuestions(
            requestType,
            routeType,
            hasPriorityGoods,
            priorityGoods,
            freightType,
            vesselDetails,
            contactInfo
          ) =>
        stubForPostWithResponse(
          s"""
             |{
             |"ApplicationType" : "Route1",
             |"OriginatingSystem" : "Digital",
             |"AcknowledgementReference" : "1234",
             |"Content": {
             |  "IsALVS":"false",
             |  "EntryDate":"${date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}",
             |  "EntryNumber":"${declarationDetails.entryNumber.value}",
             |  "EntryProcessingUnit":"${declarationDetails.epu.value}",
             |  "EntryType":"Import",
             |  "FreightOption":"${freightType.get}",
             |  "RequestType":"${requestType.get}",
             |  "Route":"${routeType.get}",
             |  "EORI":"$eori",
             |  "TelephoneNumber":"${contactInfo.get.contactNumber.get}",
             |  "EmailAddress":"${contactInfo.get.contactEmail}",
             |  "Priority":"${priorityGoods.get}",
             |  "VesselName":"${vesselDetails.get.vesselName.get}",
             |  "VesselEstimatedDate":"${vesselDetails.get.dateOfArrival.get}",
             |  "VesselEstimatedTime":"${vesselDetails.get.timeOfArrival.get}"
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
