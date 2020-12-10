package uk.gov.hmrc.traderservices.controllers

import java.time.LocalDateTime
import java.time.LocalDate

import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs._
import uk.gov.hmrc.traderservices.support.ServerBaseISpec
import java.time.LocalTime
import uk.gov.hmrc.traderservices.support.JsonMatchers
import play.api.libs.json.JsObject
import java.{util => ju}
import java.time.ZonedDateTime
import uk.gov.hmrc.traderservices.services.TraderServicesAuditEvent
import java.time.ZoneId

class TraderServicesRouteOneISpec
    extends ServerBaseISpec with AuthStubs with CreateCaseStubs with UpdateCaseStubs with JsonMatchers {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "TraderServicesRouteOneController" when {
    "POST /create-case" should {
      "return 201 with CaseID as a result if successful PEGA API call" in {
        givenAuthorised()
        givenPegaCreateCaseRequestSucceeds()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[String]("result", be("PCE201103470D2CC8K0NH3"))
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Map(
            "success"             -> "true",
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createRequestDetailsMap
        )
      }

      "return 400 with error code 400 and message if PEGA API call fails with 403" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(403, "400")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest))
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("400")) and
              notHaveProperty("errorMessage")
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Map("success" -> "false", "duplicate" -> "false", "errorCode" -> "400")
            ++ TestData.createRequestDetailsMap
        )
      }

      "return 400 with error code 500 and message if PEGA API call fails with 500" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(500, "500", "Foo Bar")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest))
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("500")) and
              haveProperty[String]("errorMessage", be("Foo Bar"))
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Map("success" -> "false", "duplicate" -> "false", "errorCode" -> "500", "errorMessage" -> "Foo Bar")
            ++ TestData.createRequestDetailsMap
        )
      }

      "return 400 with error code 409 and message if PEGA reports duplicated case" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(500, "500", "999: PCE201103470D2CC8K0NH3")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest))
          .futureValue

        result.status shouldBe 409
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("409")) and
              haveProperty[String]("errorMessage", be("PCE201103470D2CC8K0NH3"))
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Map(
            "success"      -> "false",
            "duplicate"    -> "true",
            "errorCode"    -> "409",
            "errorMessage" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createRequestDetailsMap
        )
      }

      "return 400 with error code 403 if api call returns 403 with empty body" in {
        givenAuthorised()
        givenPegaCreateCaseRequestRespondsWith403WithoutContent()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest))
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("403")) and
              haveProperty[String]("errorMessage", be("Error: empty response"))
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Map(
            "success"      -> "false",
            "duplicate"    -> "false",
            "errorCode"    -> "403",
            "errorMessage" -> "Error: empty response"
          ) ++ TestData.createRequestDetailsMap
        )
      }

      "return 500 if api call returns unexpected content" in {
        givenAuthorised()
        givenPegaCreateCaseRequestRespondsWithHtml()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest))
          .futureValue

        result.status shouldBe 500

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Map(
            "success"      -> "false",
            "duplicate"    -> "false",
            "errorCode"    -> "500",
            "errorMessage" -> "Unexpected response type of status 400, expected application/json but got text/html with body:\n<html>\\r\\n<head><title>400 Bad Request</title></head>\\r\\n<body bgcolor=\\\"white\\\">\\r\\n<center><h1>400 Bad Request</h1></center>\\r\\n<hr><center>nginx</center>\\r\\n</body>\\r\\n</html>\\r\\n\\"
          )
        )
      }
    }

    "POST /update-case" should {
      "return 201 with CaseID when only response text" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestSucceeds()

        val correlationId = ju.UUID.randomUUID().toString()

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.WriteResponse,
          responseText = Some("An example description."),
          Seq()
        )

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(payload))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[String]("result", be("PCE201103470D2CC8K0NH3"))
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened(
          "Additional Information",
          "PCE201103470D2CC8K0NH3",
          "An example description."
        )
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Map(
            "success"             -> "true",
            "typeOfAmendment"     -> "WriteResponse",
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3",
            "responseText"        -> "An example description."
          )
        )
      }

      "return 201 with CaseID when only files uploaded" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestSucceeds("The user has attached the following file(s): my.pdf.")

        val correlationId = ju.UUID.randomUUID().toString()

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.UploadDocuments,
          responseText = None,
          TestData.testUpdateCaseRequestUploadedFiles
        )

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(payload))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[String]("result", be("PCE201103470D2CC8K0NH3"))
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened(
          "Additional Information",
          "PCE201103470D2CC8K0NH3",
          "The user has attached the following file(s): my.pdf."
        )
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Map(
            "success"             -> "true",
            "typeOfAmendment"     -> "UploadDocuments",
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.updateRequestFileUploadDetailsMap
        )
      }

      "return 201 with CaseID when both response text and files uploaded" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestSucceeds()

        val correlationId = ju.UUID.randomUUID().toString()

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = Some("An example description."),
          TestData.testUpdateCaseRequestUploadedFiles
        )

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(payload))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[String]("result", be("PCE201103470D2CC8K0NH3"))
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened(
          "Additional Information",
          "PCE201103470D2CC8K0NH3",
          "An example description."
        )
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Map(
            "success"             -> "true",
            "typeOfAmendment"     -> "WriteResponseAndUploadDocuments",
            "responseText"        -> "An example description.",
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.updateRequestFileUploadDetailsMap
        )
      }

      "return 400 with error code 400 and message if PEGA API call fails with 403" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestFails(403, "400")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest))
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("400")) and
              notHaveProperty("errorMessage")
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Map("success" -> "false", "errorCode" -> "400") ++ TestData.updateRequestDetailsMap
        )
      }

      "return 400 with error code 500 and message if PEGA API call fails with 500" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestFails(500, "500", "Foo Bar")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest))
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("500")) and
              haveProperty[String]("errorMessage", be("Foo Bar"))
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Map(
            "success"      -> "false",
            "errorCode"    -> "500",
            "errorMessage" -> "Foo Bar"
          ) ++ TestData.updateRequestDetailsMap
        )
      }

      "return 400 with error code 500 and message if PEGA reports duplicated case" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestFails(500, "500", "999: PCE201103470D2CC8K0NH3")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest))
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("500")) and
              haveProperty[String]("errorMessage", be("999: PCE201103470D2CC8K0NH3"))
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Map(
            "success"      -> "false",
            "errorCode"    -> "500",
            "errorMessage" -> "999: PCE201103470D2CC8K0NH3"
          ) ++ TestData.updateRequestDetailsMap
        )
      }

      "return 400 with error code 403 if api call returns 403 with empty body" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestRespondsWith403WithoutContent()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest))
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[JsObject](
            "error",
            haveProperty[String]("errorCode", be("403")) and
              haveProperty[String]("errorMessage", be("Error: empty response"))
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Map(
            "success"      -> "false",
            "errorCode"    -> "403",
            "errorMessage" -> "Error: empty response"
          ) ++ TestData.updateRequestDetailsMap
        )
      }

      "return 500 if api call returns unexpected content" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestRespondsWithHtml()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$url/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest))
          .futureValue

        result.status shouldBe 500

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened()
        verifyAuditRequestSent(1, TraderServicesAuditEvent.UpdateCase, Map("success" -> "false", "errorCode" -> "500"))
      }
    }
  }
}

object TestData {

  val testCreateCaseRequest =
    TraderServicesCreateCaseRequest(
      DeclarationDetails(EPU(2), EntryNumber("A23456A"), LocalDate.parse("2020-09-02")),
      ImportQuestions(
        requestType = ImportRequestType.New,
        routeType = ImportRouteType.Route1,
        priorityGoods = None,
        hasALVS = false,
        freightType = ImportFreightType.Maritime,
        vesselDetails = Some(
          VesselDetails(
            vesselName = Some("Vessel Name"),
            dateOfArrival = Some(LocalDate.of(2020, 10, 29)),
            timeOfArrival = Some(LocalTime.of(23, 45, 0))
          )
        ),
        contactInfo = ContactInfo(
          contactName = Some("Full Name"),
          contactNumber = Some("07123456789"),
          contactEmail = "sampelname@gmail.com"
        )
      ),
      Seq(
        UploadedFile(
          downloadUrl = "https://s3.amazonaws.com/bucket/test.png",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("UTC")),
          checksum = "7612627146267837821637162783612ve7qwdsagjdhjabHSGUY1T31QWGAhjsahdgy1qtwy",
          fileName = "test.png",
          fileMimeType = "image/png"
        )
      ),
      "GB123456789012345"
    )

  val createRequestDetailsMap = Map(
    "eori"                                                -> "GB123456789012345",
    "questionsAnswers.import.vesselDetails.dateOfArrival" -> "2020-10-29",
    "questionsAnswers.import.hasALVS"                     -> "false",
    "questionsAnswers.import.vesselDetails.timeOfArrival" -> "23:45:00",
    "questionsAnswers.import.requestType"                 -> "New",
    "questionsAnswers.import.freightType"                 -> "Maritime",
    "declarationDetails.epu"                              -> "2",
    "declarationDetails.entryDate"                        -> "2020-09-02",
    "questionsAnswers.import.routeType"                   -> "Route1",
    "questionsAnswers.import.vesselDetails.vesselName"    -> "Vessel Name",
    "declarationDetails.entryNumber"                      -> "A23456A",
    "questionsAnswers.import.contactInfo.contactNumber"   -> "07123456789",
    "questionsAnswers.import.contactInfo.contactEmail"    -> "sampelname@gmail.com",
    "questionsAnswers.import.contactInfo.contactName"     -> "Full Name",
    "uploadedFiles.0.fileName"                            -> "test.png",
    "uploadedFiles.0.checksum"                            -> "7612627146267837821637162783612ve7qwdsagjdhjabHSGUY1T31QWGAhjsahdgy1qtwy",
    "uploadedFiles.0.fileMimeType"                        -> "image/png",
    "uploadedFiles.0.uploadTimestamp"                     -> "2020-10-10T10:10:10Z[UTC]",
    "uploadedFiles.0.downloadUrl"                         -> "https://s3.amazonaws.com/bucket/test.png",
    "numberOfFilesUploaded"                               -> "1"
  )

  val testUpdateCaseRequestUploadedFiles = Seq(
    UploadedFile(
      "https://s3.amazonaws/bucket/12817782718728728",
      ZonedDateTime.now(),
      "A1A2C3445F65",
      "my.pdf",
      "application/pdf"
    )
  )

  val updateRequestFileUploadDetailsMap = Map(
    "uploadedFiles.0.fileName"     -> "my.pdf",
    "uploadedFiles.0.checksum"     -> "A1A2C3445F65",
    "uploadedFiles.0.fileMimeType" -> "application/pdf",
    "uploadedFiles.0.downloadUrl"  -> "https://s3.amazonaws/bucket/12817782718728728",
    "numberOfFilesUploaded"        -> "1"
  )

  val testUpdateCaseRequest = TraderServicesUpdateCaseRequest(
    caseReferenceNumber = "PCE201103470D2CC8K0NH3",
    typeOfAmendment = TypeOfAmendment.WriteResponseAndUploadDocuments,
    responseText = Some("An example description."),
    testUpdateCaseRequestUploadedFiles
  )

  val updateRequestDetailsMap = Map(
    "typeOfAmendment"     -> "WriteResponseAndUploadDocuments",
    "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3",
    "responseText"        -> "An example description."
  ) ++ updateRequestFileUploadDetailsMap

}
