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
import uk.gov.hmrc.traderservices.connectors.ApiError
import play.api.libs.ws.InMemoryBody
import akka.util.ByteString
import java.nio.charset.StandardCharsets
import play.api.libs.ws.BodyWritable
import java.net.URLEncoder
import play.api.libs.json.JsArray

class CreateUpdateCaseControllerISpec
    extends ServerBaseISpec with AuthStubs with CreateCaseStubs with UpdateCaseStubs with FileTransferStubs
    with JsonMatchers {
  this: Suite with ServerProvider =>

  val baseUrl = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "CreateUpdateCaseController" when {
    "POST /create-case" should {
      "return 201 with CaseID as a result if successful PEGA API call for import case" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()
        givenPegaCreateImportCaseRequestSucceeds(200)

        val createCaseRequest =
          TestData.testCreateImportCaseRequest(wireMockBaseUrlAsString)

        val files = FileTransferData.fromUploadedFilesAndExplanation(
          createCaseRequest.uploadedFiles,
          createCaseRequest.questionsAnswers.explanation
        )

        givenMultiFileTransferSucceeds(
          "PCE201103470D2CC8K0NH3",
          "Route1",
          correlationId,
          files
        )

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(createCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "result",
              haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")) and
                haveProperty[Seq[FileTransferResult]]("fileTransferResults", have(size(3)))
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyMultiFileTransferHasHappened(1)
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj(
            "success"             -> true,
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createImportRequestDetails(wireMockBaseUrlAsString, transferSuccess = true, files = files)
        )
      }

      "return 201 with CaseID as a result if successful PEGA API call for export case" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()
        givenPegaCreateExportCaseRequestSucceeds()

        val createCaseRequest =
          TestData.testCreateExportCaseRequest(wireMockBaseUrlAsString)

        val files = FileTransferData.fromUploadedFilesAndExplanation(
          createCaseRequest.uploadedFiles,
          createCaseRequest.questionsAnswers.explanation
        )

        givenMultiFileTransferSucceeds(
          "PCE201103470D2CC8K0NH3",
          "Route1",
          correlationId,
          files
        )

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(createCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "result",
              haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")) and
                haveProperty[Seq[FileTransferResult]]("fileTransferResults", have(size(3)))
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyMultiFileTransferHasHappened(1)
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj(
            "success"             -> true,
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createExportRequestDetails(wireMockBaseUrlAsString, transferSuccess = true, files)
        )
      }

      "return 400 if empty payload" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.obj())
          .futureValue

        val errorMessage =
          "Invalid payload: Parsing failed due to at path /uploadedFiles with error.path.missing, and at path /entryDetails with error.path.missing, and at path /questionsAnswers with error.path.missing."

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "error",
              haveProperty[String]("errorCode", be("ERROR_JSON")) and
                haveProperty[String](
                  "errorMessage",
                  be(errorMessage)
                )
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestDidNotHappen()
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          TestData.errorDetails(
            wireMockBaseUrlAsString,
            ApiError(
              "ERROR_JSON",
              Some(errorMessage)
            )
          ) ++ Json.obj("duplicate" -> false)
        )
      }

      "return 400 if malformed payload" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()

        val jsonBodyWritable =
          BodyWritable
            .apply[String](s => InMemoryBody(ByteString.fromString(s, StandardCharsets.UTF_8)), "application/json")

        val jsonPayload =
          Json.prettyPrint(Json.toJson(TestData.testCreateExportCaseRequest(wireMockBaseUrlAsString)))

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(jsonPayload.take(13))(jsonBodyWritable)
          .futureValue

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "error",
              haveProperty[String]("errorCode", be("ERROR_UNKNOWN")) and
                haveProperty[String](
                  "errorMessage",
                  be(
                    "Could not parse payload due to Unexpected end-of-input in field name\n at [Source: (String)\"{\n  \"entryDet\"; line: 2, column: 12]."
                  )
                )
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestDidNotHappen()
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          TestData.errorDetails(
            wireMockBaseUrlAsString,
            ApiError(
              "ERROR_UNKNOWN",
              Some(
                "Could not parse payload due to Unexpected end-of-input in field name\n at [Source: (String)\"{\n  \"entryDet\"; line: 2, column: 12]."
              )
            )
          ) ++ Json.obj("duplicate" -> false)
        )
      }

      "return 400 with error code 400 and message if PEGA API call fails with 403" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(403, "400")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateImportCaseRequest(wireMockBaseUrlAsString)))
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
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj("success" -> false, "duplicate" -> false, "errorCode" -> "400")
            ++ TestData.createImportRequestDetails(wireMockBaseUrlAsString, transferSuccess = false, files = Seq.empty)
        )
      }

      "return 400 with error code 500 and message if PEGA API call fails with 500" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(500, "500", "Foo Bar")

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .post(Json.toJson(TestData.testCreateImportCaseRequest(wireMockBaseUrlAsString)))
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
        verifyPegaCreateCaseRequestHasHappened(times = 3)
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj("success" -> false, "duplicate" -> false, "errorCode" -> "500", "errorMessage" -> "Foo Bar")
            ++ TestData.createImportRequestDetails(wireMockBaseUrlAsString, transferSuccess = false, files = Seq.empty)
        )
      }

      "return 400 with error code 409 and message if PEGA reports duplicated import case" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(500, "500", "999: PCE201103470D2CC8K0NH3")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateImportCaseRequest(wireMockBaseUrlAsString)))
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
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj(
            "success"      -> false,
            "duplicate"    -> true,
            "errorCode"    -> "409",
            "errorMessage" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createImportRequestDetails(wireMockBaseUrlAsString, transferSuccess = false, files = Seq.empty)
        )
      }

      "return 400 with error code 409 and message if PEGA reports duplicated export case" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(500, "500", "999: PCE201103470D2CC8K0NH3")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateExportCaseRequest(wireMockBaseUrlAsString)))
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
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj(
            "success"      -> false,
            "duplicate"    -> true,
            "errorCode"    -> "409",
            "errorMessage" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createExportRequestDetails(wireMockBaseUrlAsString, transferSuccess = false, files = Seq.empty)
        )
      }

      "return 400 with error code 403 if api call returns 403 with empty body" in {
        givenAuthorised()
        givenPegaCreateCaseRequestRespondsWith403WithoutContent()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateImportCaseRequest(wireMockBaseUrlAsString)))
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
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj(
            "success"      -> false,
            "duplicate"    -> false,
            "errorCode"    -> "403",
            "errorMessage" -> "Error: empty response"
          ) ++ TestData.createImportRequestDetails(wireMockBaseUrlAsString, transferSuccess = false, files = Seq.empty)
        )
      }

      "return 500 if api call returns unexpected content" in {
        givenAuthorised()
        givenPegaCreateCaseRequestRespondsWithHtml()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateImportCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 500

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj(
            "success"      -> false,
            "duplicate"    -> false,
            "errorCode"    -> "500",
            "errorMessage" -> "Unexpected response type of status 400, expected application/json but got text/html with body:\n<html>\\r\\n<head><title>400 Bad Request</title></head>\\r\\n<body bgcolor=\\\"white\\\">\\r\\n<center><h1>400 Bad Request</h1></center>\\r\\n<hr><center>nginx</center>\\r\\n</body>\\r\\n</html>\\r\\n\\"
          )
        )
      }
    }

    "POST /update-case" should {
      "return 201 with CaseID when only response text" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()
        givenPegaUpdateCaseRequestSucceeds()

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.WriteResponse,
          responseText = Some("An example description."),
          uploadedFiles = Seq(),
          eori = Some("GB123456789012345")
        )

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(payload))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject]("result", haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")))
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened(
          "Additional Information",
          "PCE201103470D2CC8K0NH3",
          "An example description."
        )
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj(
            "success"             -> true,
            "typeOfAmendment"     -> "WriteResponse",
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3",
            "responseText"        -> "An example description."
          )
        )
      }

      "return 201 with CaseID when only files uploaded" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()
        val uploadedFiles = TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString)
        givenPegaUpdateCaseRequestSucceeds("The user has attached the following file(s): test?1.jpeg.")
        givenMultiFileTransferSucceeds(
          "PCE201103470D2CC8K0NH3",
          "Route1",
          correlationId,
          uploadedFiles.map(FileTransferData.fromUploadedFile)
        )

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.UploadDocuments,
          responseText = None,
          uploadedFiles = uploadedFiles,
          eori = Some("GB123456789012345")
        )

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(payload))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject]("result", haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")))
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened(
          "Additional Information",
          "PCE201103470D2CC8K0NH3",
          "The user has attached the following file(s): test?1.jpeg."
        )
        verifyMultiFileTransferHasHappened(1)
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj(
            "success"             -> true,
            "typeOfAmendment"     -> "UploadDocuments",
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.updateRequestFileUploadDetailsMap(wireMockBaseUrlAsString, transferSuccess = true)
        )
      }

      "return 201 with CaseID when both response text and files uploaded" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()
        givenPegaUpdateCaseRequestSucceeds()
        val uploadedFiles = TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString)
        givenMultiFileTransferSucceeds(
          "PCE201103470D2CC8K0NH3",
          "Route1",
          correlationId,
          uploadedFiles.map(FileTransferData.fromUploadedFile)
        )

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = Some("An example description."),
          uploadedFiles = uploadedFiles,
          eori = Some("GB123456789012345")
        )

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(payload))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject]("result", haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")))
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened(
          "Additional Information",
          "PCE201103470D2CC8K0NH3",
          "An example description."
        )
        verifyMultiFileTransferHasHappened(1)
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj(
            "success"             -> true,
            "typeOfAmendment"     -> "WriteResponseAndUploadDocuments",
            "responseText"        -> "An example description.",
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.updateRequestFileUploadDetailsMap(wireMockBaseUrlAsString, transferSuccess = true)
        )
      }

      "return 400 if invalid payload" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "fooooooooooooooooooooooooooooooooooooooooo",
          typeOfAmendment = TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = Some("An example description."),
          uploadedFiles = TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString),
          eori = Some("GB123456789012345")
        )

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(payload))
          .futureValue

        val errorMessage =
          s"""Invalid payload: Validation failed due to "Invalid caseReferenceNumber, should be between 1 and 32 (inclusive) character long. in ${payload
            .toString()
            .replaceAllLiterally("List", "Vector")}."""

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "error",
              haveProperty[String]("errorCode", be("ERROR_VALIDATION")) and
                haveProperty[String](
                  "errorMessage",
                  be(errorMessage)
                )
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestDidNotHappen()
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          TestData.errorDetails(
            wireMockBaseUrlAsString,
            ApiError(
              "ERROR_VALIDATION",
              Some(errorMessage)
            )
          )
        )
      }

      "return 400 if empty payload" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.obj())
          .futureValue

        val errorMessage =
          "Invalid payload: Parsing failed due to at path /uploadedFiles with error.path.missing, and at path /caseReferenceNumber with error.path.missing, and at path /typeOfAmendment with error.path.missing."

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "error",
              haveProperty[String]("errorCode", be("ERROR_JSON")) and
                haveProperty[String](
                  "errorMessage",
                  be(errorMessage)
                )
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestDidNotHappen()
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          TestData.errorDetails(
            wireMockBaseUrlAsString,
            ApiError(
              "ERROR_JSON",
              Some(errorMessage)
            )
          )
        )
      }

      "return 400 if malformed payload" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()

        val jsonBodyWritable =
          BodyWritable
            .apply[String](s => InMemoryBody(ByteString.fromString(s, StandardCharsets.UTF_8)), "application/json")

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = Some("An example description."),
          uploadedFiles = TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString),
          eori = Some("GB123456789012345")
        )

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.prettyPrint(Json.toJson(payload)).take(21))(jsonBodyWritable)
          .futureValue

        val errorMessage =
          "Could not parse payload due to Unexpected end-of-input in field name\n at [Source: (String)\"{\n  \"caseReferenceNum\"; line: 2, column: 20]."

        result.status shouldBe 400
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "error",
              haveProperty[String]("errorCode", be("ERROR_UNKNOWN")) and
                haveProperty[String](
                  "errorMessage",
                  be(errorMessage)
                )
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestDidNotHappen()
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          TestData.errorDetails(
            wireMockBaseUrlAsString,
            ApiError(
              "ERROR_UNKNOWN",
              Some(errorMessage)
            )
          )
        )
      }

      "return 400 with error code 400 and message if PEGA API call fails with 403" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestFails(403, "400")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest(wireMockBaseUrlAsString)))
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
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj("success" -> false, "errorCode" -> "400") ++ TestData.updateRequestDetailsMap(
            wireMockBaseUrlAsString,
            transferSuccess = false
          )
        )
      }

      "return 400 with error code 500 and message if PEGA API call fails with 500" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestFails(500, "500", "Foo Bar")

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .post(Json.toJson(TestData.testUpdateCaseRequest(wireMockBaseUrlAsString)))
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
        verifyPegaUpdateCaseRequestHasHappened(times = 3)
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj(
            "success"      -> false,
            "errorCode"    -> "500",
            "errorMessage" -> "Foo Bar"
          ) ++ TestData.updateRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

      "return 400 with error code 500 and message if PEGA reports duplicated case" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestFails(500, "500", "999: PCE201103470D2CC8K0NH3")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest(wireMockBaseUrlAsString)))
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
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj(
            "success"      -> false,
            "errorCode"    -> "500",
            "errorMessage" -> "999: PCE201103470D2CC8K0NH3"
          ) ++ TestData.updateRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

      "return 400 with error code 403 if api call returns 403 with empty body" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestRespondsWith403WithoutContent()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest(wireMockBaseUrlAsString)))
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
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj(
            "success"      -> false,
            "errorCode"    -> "403",
            "errorMessage" -> "Error: empty response"
          ) ++ TestData.updateRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

      "return 500 if api call returns unexpected content" in {
        givenAuthorised()
        givenPegaUpdateCaseRequestRespondsWithHtml()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testUpdateCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 500

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened()
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.UpdateCase,
          Json.obj("success" -> false, "errorCode" -> "500")
        )
      }
    }
  }
}

object TestData {

  def testCreateImportCaseRequest(baseUrl: String) =
    TraderServicesCreateCaseRequest(
      EntryDetails(EPU(2), EntryNumber("223456A"), LocalDate.parse("2020-09-02")),
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
        ),
        explanation = Some(
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tempus tempor est in dapibus. Maecenas dignissim elit in tempus vehicula. Nullam nunc eros, laoreet eu augue a, elementum mattis leo."
        )
      ),
      Seq(
        UploadedFile(
          "ref-123",
          downloadUrl = baseUrl + s"/bucket/${URLEncoder.encode("test⫐1.jpeg", "UTF-8")}",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("UTC")),
          checksum = "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          fileName = "test⫐1.jpeg",
          fileMimeType = "image/jpeg"
        ),
        UploadedFile(
          "ref-789",
          downloadUrl = baseUrl + "/bucket/app.routes",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 20, 20, 0, ZoneId.of("UTC")),
          checksum = "f1198e91e6fe05ccf6788b2f871f0fa90e9fab98252e81ca20238cf26119e616",
          fileName = "app.routes",
          fileMimeType = "application/routes"
        )
      ),
      eori = Some("GB123456789012345")
    )

  def testCreateExportCaseRequest(baseUrl: String) =
    TraderServicesCreateCaseRequest(
      EntryDetails(EPU(2), EntryNumber("A23456A"), LocalDate.parse("2020-09-02")),
      ExportQuestions(
        requestType = ExportRequestType.New,
        routeType = ExportRouteType.Route1,
        priorityGoods = Some(ExportPriorityGoods.LiveAnimals),
        freightType = ExportFreightType.Maritime,
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
        ),
        explanation = Some(
          "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tempus tempor est in dapibus. Maecenas dignissim elit in tempus vehicula. Nullam nunc eros, laoreet eu augue a, elementum mattis leo."
        )
      ),
      Seq(
        UploadedFile(
          "ref-123",
          downloadUrl = baseUrl + s"/bucket/${URLEncoder.encode("test⫐1.jpeg", "UTF-8")}",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("UTC")),
          checksum = "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          fileName = "test⫐1.jpeg",
          fileMimeType = "image/jpeg"
        ),
        UploadedFile(
          "ref-789",
          downloadUrl = baseUrl + "/bucket/app.routes",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 20, 20, 0, ZoneId.of("UTC")),
          checksum = "f1198e91e6fe05ccf6788b2f871f0fa90e9fab98252e81ca20238cf26119e616",
          fileName = "app.routes",
          fileMimeType = "application/routes"
        )
      ),
      eori = Some("GB123456789012345")
    )

  def createImportRequestDetails(baseUrl: String, transferSuccess: Boolean, files: Seq[FileTransferData]): JsObject =
    Json.obj(
      "eori"            -> "GB123456789012345",
      "declarationType" -> "import",
      "entryDetails" -> Json
        .obj("epu" -> "2", "entryDate" -> "2020-09-02", "entryNumber" -> "223456A"),
      "dateOfArrival" -> "2020-10-29",
      "hasALVS"       -> false,
      "timeOfArrival" -> "23:45:00",
      "requestType"   -> "New",
      "freightType"   -> "Maritime",
      "routeType"     -> "Route1",
      "vesselName"    -> "Vessel Name",
      "contactNumber" -> "07123456789",
      "contactEmail"  -> "sampelname@gmail.com",
      "contactName"   -> "Full Name",
      "uploadedFiles" -> JsArray(
        files.map(f =>
          Json.obj(
            "upscanReference" -> f.upscanReference,
            "fileName"        -> f.fileName,
            "checksum"        -> f.checksum,
            "fileMimeType"    -> f.fileMimeType,
            "downloadUrl"     -> f.downloadUrl,
            "transferSuccess" -> transferSuccess
          )
        )
      ),
      "numberOfFilesUploaded" -> 2
    )

  def createExportRequestDetails(baseUrl: String, transferSuccess: Boolean, files: Seq[FileTransferData]): JsObject =
    Json.obj(
      "eori"            -> "GB123456789012345",
      "declarationType" -> "export",
      "entryDetails" -> Json
        .obj("epu" -> "2", "entryDate" -> "2020-09-02", "entryNumber" -> "A23456A"),
      "dateOfArrival" -> "2020-10-29",
      "timeOfArrival" -> "23:45:00",
      "requestType"   -> "New",
      "freightType"   -> "Maritime",
      "routeType"     -> "Route1",
      "priorityGoods" -> "LiveAnimals",
      "vesselName"    -> "Vessel Name",
      "contactNumber" -> "07123456789",
      "contactEmail"  -> "sampelname@gmail.com",
      "contactName"   -> "Full Name",
      "uploadedFiles" -> JsArray(
        files.map(f =>
          Json.obj(
            "upscanReference" -> f.upscanReference,
            "fileName"        -> f.fileName,
            "checksum"        -> f.checksum,
            "fileMimeType"    -> f.fileMimeType,
            "downloadUrl"     -> f.downloadUrl,
            "transferSuccess" -> transferSuccess
          )
        )
      ),
      "numberOfFilesUploaded" -> 2
    )

  def errorDetails(baseUrl: String, error: ApiError): JsObject =
    Json.obj(
      "success"   -> false,
      "errorCode" -> s"${error.errorCode}"
    ) ++
      (error.errorMessage.map(m => Json.obj("errorMessage" -> s"$m")).getOrElse(Json.obj()))

  def testUpdateCaseRequestUploadedFiles(baseUrl: String) =
    Seq(
      UploadedFile(
        "ref-123",
        baseUrl + s"/bucket/${URLEncoder.encode("test⫐1.jpeg", "UTF-8")}",
        ZonedDateTime.now(),
        "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
        "test⫐1.jpeg",
        "image/jpeg"
      )
    )

  def updateRequestFileUploadDetailsMap(baseUrl: String, transferSuccess: Boolean) =
    Json.obj(
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName"        -> "test⫐1.jpeg",
          "checksum"        -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType"    -> "image/jpeg",
          "downloadUrl"     -> (baseUrl + s"/bucket/${URLEncoder.encode("test⫐1.jpeg", "UTF-8")}"),
          "transferSuccess" -> transferSuccess
        )
      ),
      "numberOfFilesUploaded" -> 1
    )

  def testUpdateCaseRequest(baseUrl: String) =
    TraderServicesUpdateCaseRequest(
      caseReferenceNumber = "PCE201103470D2CC8K0NH3",
      typeOfAmendment = TypeOfAmendment.WriteResponseAndUploadDocuments,
      responseText = Some("An example description."),
      uploadedFiles = testUpdateCaseRequestUploadedFiles(baseUrl),
      eori = Some("GB123456789012345")
    )

  def updateRequestDetailsMap(baseUrl: String, transferSuccess: Boolean) =
    Json.obj(
      "typeOfAmendment"     -> "WriteResponseAndUploadDocuments",
      "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3",
      "responseText"        -> "An example description."
    ) ++ updateRequestFileUploadDetailsMap(baseUrl, transferSuccess)

}
