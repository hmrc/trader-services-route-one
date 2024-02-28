/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.traderservices.controllers

import java.time.LocalDateTime
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.traderservices.models._
import uk.gov.hmrc.traderservices.stubs._
import uk.gov.hmrc.traderservices.support.ServerBaseISpec
import uk.gov.hmrc.traderservices.support.JsonMatchers
import play.api.libs.json.JsObject

import java.{util => ju}
import uk.gov.hmrc.traderservices.services.TraderServicesAuditEvent
import uk.gov.hmrc.traderservices.connectors.ApiError
import play.api.libs.ws.InMemoryBody
import org.apache.pekko.util.ByteString

import java.nio.charset.StandardCharsets
import play.api.libs.ws.BodyWritable
import play.api.Application
import org.scalatest.concurrent.Eventually
import org.scalatest.time.Span
import org.scalatest.time.Millis
import uk.gov.hmrc.http.HeaderNames

class CreateUpdateCaseControllerAsyncISpec
    extends ServerBaseISpec with AuthStubs with CreateCaseStubs with UpdateCaseStubs with FileTransferStubs
    with JsonMatchers with Eventually {
  this: Suite with ServerProvider =>

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(5000, Millis)), interval = scaled(Span(500, Millis)))

  override implicit lazy val app: Application = defaultAppBuilder
    .configure("features.transferFilesAsync" -> true)
    .build()

  val baseUrl = s"http://localhost:$port"

  val dateTime: LocalDateTime = LocalDateTime.now()

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "CreateUpdateCaseController" when {
    "POST /create-case" should {
      "return 201 with CaseID as a result if successful PEGA API call for import case" in {
        val correlationId = ju.UUID.randomUUID().toString
        givenAuthorised()
        givenPegaCreateImportCaseRequestSucceeds(200)

        val createCaseRequest =
          TestData.testCreateImportCaseRequest(wireMockBaseUrlAsString)

        val files = FileTransferData.fromUploadedFilesAndReason(
          createCaseRequest.uploadedFiles,
          createCaseRequest.questionsAnswers.reason
        )

        givenMultiFileTransferSucceeds(
          "PCE201103470D2CC8K0NH3",
          "Route1",
          correlationId,
          files
        )

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
          .post(Json.toJson(createCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "result",
              haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")) and
                haveProperty[Seq[FileTransferResult]]("fileTransferResults", have(size(0)))
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        eventually {
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
      }

      "return 201 with CaseID as a result if successful PEGA API call for export case" in {
        val correlationId = ju.UUID.randomUUID().toString
        givenAuthorised()
        givenPegaCreateExportCaseRequestSucceeds()

        val createCaseRequest =
          TestData.testCreateExportCaseRequest(wireMockBaseUrlAsString)

        val files = FileTransferData.fromUploadedFilesAndReason(
          createCaseRequest.uploadedFiles,
          createCaseRequest.questionsAnswers.reason
        )

        givenMultiFileTransferSucceeds(
          "PCE201103470D2CC8K0NH3",
          "Route1",
          correlationId,
          files
        )

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
          .post(Json.toJson(createCaseRequest))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "result",
              haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")) and
                haveProperty[Seq[FileTransferResult]]("fileTransferResults", have(size(0)))
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        eventually {
          verifyMultiFileTransferHasHappened(1)
          verifyAuditRequestSent(
            1,
            TraderServicesAuditEvent.CreateCase,
            Json.obj(
              "success"             -> true,
              "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
            ) ++ TestData.createExportRequestDetails(wireMockBaseUrlAsString, transferSuccess = true, files = files)
          )
        }
      }

      "return 400 if empty payload" in {
        val correlationId = ju.UUID.randomUUID().toString
        givenAuthorised()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
        verifyMultiFileTransferDidNotHappen()
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
                    "Could not parse payload due to Unexpected end-of-input in field name\n at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 2, column: 12]."
                  )
                )
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestDidNotHappen()
        verifyMultiFileTransferDidNotHappen()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          TestData.errorDetails(
            wireMockBaseUrlAsString,
            ApiError(
              "ERROR_UNKNOWN",
              Some(
                "Could not parse payload due to Unexpected end-of-input in field name\n at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 2, column: 12]."
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
        verifyMultiFileTransferDidNotHappen()
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
          .withHttpHeaders(HeaderNames.authorisation -> "Bearer dummy-it-token")
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
        verifyMultiFileTransferDidNotHappen()
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
        verifyMultiFileTransferDidNotHappen()
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
            "success"   -> false,
            "duplicate" -> false,
            "errorCode" -> "500",
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
        givenPegaUpdateCaseRequestSucceeds("The user has attached the following file(s): test?1.jpeg.")
        val uploadedFiles =
          TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString)
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
        eventually {
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
      }

      "return 201 with CaseID when both response text and files uploaded" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()
        givenPegaUpdateCaseRequestSucceeds()
        val uploadedFiles =
          TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString)
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
        eventually {
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
          .post(Json.toJson(payload))
          .futureValue

        val errorMessage =
          s"""Invalid payload: Validation failed due to "Invalid caseReferenceNumber, should be between 1 and 32 (inclusive) character long. in ${payload.toString}."""

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
        verifyFileTransferDidNotHappen()
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
        val correlationId = ju.UUID.randomUUID().toString
        givenAuthorised()

        val result = wsClient
          .url(s"$baseUrl/update-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
          .post(Json.obj())
          .futureValue

        val errorMessage =
          "Invalid payload: Parsing failed due to at path /uploadedFiles with error.path.missing, and at path /typeOfAmendment with error.path.missing, and at path /caseReferenceNumber with error.path.missing."

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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
          .post(Json.prettyPrint(Json.toJson(payload)).take(21))(jsonBodyWritable)
          .futureValue

        val errorMessage =
          "Could not parse payload due to Unexpected end-of-input in field name\n at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 2, column: 20]."

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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
          .withHttpHeaders(HeaderNames.authorisation -> "Bearer dummy-it-token")
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
          .withHttpHeaders("X-Correlation-ID" -> correlationId, HeaderNames.authorisation -> "Bearer dummy-it-token")
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
