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

class CreateUpdateCaseControllerISpec
    extends ServerBaseISpec with AuthStubs with CreateCaseStubs with UpdateCaseStubs with FileTransferStubs
    with JsonMatchers {
  this: Suite with ServerProvider =>

  val baseUrl = s"http://localhost:$port"

  val dateTime = LocalDateTime.now()

  val wsClient = app.injector.instanceOf[WSClient]

  "CreateUpdateCaseController" when {
    "POST /create-case" should {
      "return 201 with CaseID as a result if successful PEGA API call" in {
        val correlationId = ju.UUID.randomUUID().toString()
        givenAuthorised()
        givenPegaCreateCaseRequestSucceeds()
        givenFileTransferSucceeds("PCE201103470D2CC8K0NH3", "test1.jpeg", correlationId)
        givenFileTransferSucceeds("PCE201103470D2CC8K0NH3", "app.routes", correlationId)

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 201
        result.json.as[JsObject] should (
          haveProperty[String]("correlationId", be(correlationId)) and
            haveProperty[JsObject](
              "result",
              haveProperty[String]("caseId", be("PCE201103470D2CC8K0NH3")) and
                haveProperty[Seq[FileTransferResult]]("fileTransferResults", have(size(2)))
            )
        )

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
        verifyAuditRequestSent(
          1,
          TraderServicesAuditEvent.CreateCase,
          Json.obj(
            "success"             -> true,
            "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = true)
        )
      }

      "return 400 with error code 400 and message if PEGA API call fails with 403" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(403, "400")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
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
          Json.obj("success" -> false, "duplicate" -> false, "errorCode" -> "400")
            ++ TestData.createRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

      "return 400 with error code 500 and message if PEGA API call fails with 500" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(500, "500", "Foo Bar")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
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
          Json.obj("success" -> false, "duplicate" -> false, "errorCode" -> "500", "errorMessage" -> "Foo Bar")
            ++ TestData.createRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

      "return 400 with error code 409 and message if PEGA reports duplicated case" in {
        givenAuthorised()
        givenPegaCreateCaseRequestFails(500, "500", "999: PCE201103470D2CC8K0NH3")

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
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
          Json.obj(
            "success"      -> false,
            "duplicate"    -> true,
            "errorCode"    -> "409",
            "errorMessage" -> "PCE201103470D2CC8K0NH3"
          ) ++ TestData.createRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

      "return 400 with error code 403 if api call returns 403 with empty body" in {
        givenAuthorised()
        givenPegaCreateCaseRequestRespondsWith403WithoutContent()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
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
          Json.obj(
            "success"      -> false,
            "duplicate"    -> false,
            "errorCode"    -> "403",
            "errorMessage" -> "Error: empty response"
          ) ++ TestData.createRequestDetailsMap(wireMockBaseUrlAsString, transferSuccess = false)
        )
      }

      "return 500 if api call returns unexpected content" in {
        givenAuthorised()
        givenPegaCreateCaseRequestRespondsWithHtml()

        val correlationId = ju.UUID.randomUUID().toString()

        val result = wsClient
          .url(s"$baseUrl/create-case")
          .withHttpHeaders("X-Correlation-ID" -> correlationId)
          .post(Json.toJson(TestData.testCreateCaseRequest(wireMockBaseUrlAsString)))
          .futureValue

        result.status shouldBe 500

        verifyAuthorisationHasHappened()
        verifyPegaCreateCaseRequestHasHappened()
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
          eori = "GB123456789012345"
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
        givenPegaUpdateCaseRequestSucceeds("The user has attached the following file(s): test1.jpeg.")
        givenFileTransferSucceeds("PCE201103470D2CC8K0NH3", "test1.jpeg", correlationId)

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.UploadDocuments,
          responseText = None,
          uploadedFiles = TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString),
          eori = "GB123456789012345"
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
          "The user has attached the following file(s): test1.jpeg."
        )
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
        givenFileTransferSucceeds("PCE201103470D2CC8K0NH3", "test1.jpeg", correlationId)

        val payload = TraderServicesUpdateCaseRequest(
          caseReferenceNumber = "PCE201103470D2CC8K0NH3",
          typeOfAmendment = TypeOfAmendment.WriteResponseAndUploadDocuments,
          responseText = Some("An example description."),
          uploadedFiles = TestData.testUpdateCaseRequestUploadedFiles(wireMockBaseUrlAsString),
          eori = "GB123456789012345"
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
              haveProperty[String]("errorMessage", be("Foo Bar"))
          )
        )

        verifyAuthorisationHasHappened()
        verifyPegaUpdateCaseRequestHasHappened()
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

  def testCreateCaseRequest(baseUrl: String) =
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
          "ref-123",
          downloadUrl = baseUrl + "/bucket/test1.jpeg",
          uploadTimestamp = ZonedDateTime.of(2020, 10, 10, 10, 10, 10, 0, ZoneId.of("UTC")),
          checksum = "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          fileName = "test1.jpeg",
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
      eori = "GB123456789012345"
    )

  def createRequestDetailsMap(baseUrl: String, transferSuccess: Boolean): JsObject =
    Json.obj(
      "eori"            -> "GB123456789012345",
      "declarationType" -> "import",
      "declarationDetails" -> Json
        .obj("epu" -> "2", "entryDate" -> "2020-09-02", "entryNumber" -> "A23456A"),
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
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName"        -> "test1.jpeg",
          "checksum"        -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType"    -> "image/jpeg",
          "uploadTimestamp" -> "2020-10-10T10:10:10Z[UTC]",
          "downloadUrl"     -> (baseUrl + "/bucket/test1.jpeg"),
          "transferSuccess" -> transferSuccess
        ),
        Json.obj(
          "upscanReference" -> "ref-789",
          "fileName"        -> "app.routes",
          "checksum"        -> "f1198e91e6fe05ccf6788b2f871f0fa90e9fab98252e81ca20238cf26119e616",
          "fileMimeType"    -> "application/routes",
          "uploadTimestamp" -> "2020-10-10T10:20:20Z[UTC]",
          "downloadUrl"     -> (baseUrl + "/bucket/app.routes"),
          "transferSuccess" -> transferSuccess
        )
      ),
      "numberOfFilesUploaded" -> 2
    )

  def testUpdateCaseRequestUploadedFiles(baseUrl: String) =
    Seq(
      UploadedFile(
        "ref-123",
        baseUrl + "/bucket/test1.jpeg",
        ZonedDateTime.now(),
        "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
        "test1.jpeg",
        "image/jpeg"
      )
    )

  def updateRequestFileUploadDetailsMap(baseUrl: String, transferSuccess: Boolean) =
    Json.obj(
      "uploadedFiles" -> Json.arr(
        Json.obj(
          "upscanReference" -> "ref-123",
          "fileName"        -> "test1.jpeg",
          "checksum"        -> "f55a741917d512ab4c547ea97bdfdd8df72bed5fe51b6a248e0a5a0ae58061c8",
          "fileMimeType"    -> "image/jpeg",
          "downloadUrl"     -> (baseUrl + "/bucket/test1.jpeg"),
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
      eori = "GB123456789012345"
    )

  def updateRequestDetailsMap(baseUrl: String, transferSuccess: Boolean) =
    Json.obj(
      "typeOfAmendment"     -> "WriteResponseAndUploadDocuments",
      "caseReferenceNumber" -> "PCE201103470D2CC8K0NH3",
      "responseText"        -> "An example description."
    ) ++ updateRequestFileUploadDetailsMap(baseUrl, transferSuccess)

}
