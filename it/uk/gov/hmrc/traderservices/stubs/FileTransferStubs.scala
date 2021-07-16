package uk.gov.hmrc.traderservices.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait FileTransferStubs {
  me: WireMockSupport =>

  def givenFileTransferSucceeds(
    caseReferenceNumber: String,
    fileName: String,
    conversationId: String
  ): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-file"))
        .withRequestBody(
          equalToJson(
            s"""{
               |   "caseReferenceNumber":"$caseReferenceNumber",
               |   "fileName":"$fileName",
               |   "conversationId":"$conversationId"
               |}""".stripMargin,
            true,
            true
          )
        )
        .willReturn(
          aResponse()
            .withStatus(202)
        )
    )

  def givenFileTransferFailure(status: Int): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-file"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def verifyFileTransferHasHappened(times: Int = 1) =
    verify(times, postRequestedFor(urlPathEqualTo("/transfer-file")))

  def verifyTraderServicesFileTransferDidNotHappen() =
    verify(0, postRequestedFor(urlPathEqualTo("/transfer-file")))

  def givenMultiFileTransferSucceeds(
    caseReferenceNumber: String,
    applicationName: String,
    conversationId: String
  ): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-multiple-files"))
        .withRequestBody(
          equalToJson(
            s"""{
               |   "caseReferenceNumber":"$caseReferenceNumber",
               |   "applicationName":"$applicationName",
               |   "conversationId":"$conversationId",
               |   "files": [
               |      {
               |          "upscanReference":"XYZ0123456789",
               |          "downloadUrl":"/dummy.jpeg",
               |          "fileName":"dummy.jpeg",
               |          "fileMimeType":"image/jpeg",
               |          "checksum":"${"0" * 64}"
               |      },
               |      {
               |          "upscanReference":"XYZ0123456780",
               |          "downloadUrl":"/foo.jpeg",
               |          "fileName":"foo.jpeg",
               |          "fileMimeType":"image/jpeg",
               |          "checksum":"${"1" * 64}"
               |      }
               |    ]
               |}""".stripMargin,
            true,
            true
          )
        )
        .willReturn(
          aResponse()
            .withStatus(201)
            .withBody(s"""{
                         |   "caseReferenceNumber":"$caseReferenceNumber",
                         |   "applicationName":"$applicationName",
                         |   "conversationId":"$conversationId",
                         |    "results": [
                         |        {
                         |            "upscanReference": "XYZ0123456789",
                         |            "success": true,
                         |            "httpStatus": 202,
                         |            "transferredAt": "2021-07-14T12:35:19"
                         |        },
                         |        {
                         |            "upscanReference": "XYZ0123456780",
                         |            "success": false,
                         |            "httpStatus": 400,
                         |            "transferredAt": "2021-07-14T12:37:02"
                         |        }
                         |    ]
                         |}""".stripMargin)
        )
    )

  def givenMultiFileTransferFails(
    caseReferenceNumber: String,
    applicationName: String,
    conversationId: String,
    status: Int
  ): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-multiple-files"))
        .withRequestBody(
          equalToJson(
            s"""{
               |   "caseReferenceNumber":"$caseReferenceNumber",
               |   "applicationName":"$applicationName",
               |   "conversationId":"$conversationId",
               |   "files": [
               |      {
               |          "upscanReference":"XYZ0123456789",
               |          "downloadUrl":"/dummy.jpeg",
               |          "fileName":"dummy.jpeg",
               |          "fileMimeType":"image/jpeg",
               |          "checksum":"${"0" * 64}"
               |      },
               |      {
               |          "upscanReference":"XYZ0123456780",
               |          "downloadUrl":"/foo.jpeg",
               |          "fileName":"foo.jpeg",
               |          "fileMimeType":"image/jpeg",
               |          "checksum":"${"1" * 64}"
               |      }
               |    ]
               |}""".stripMargin,
            true,
            true
          )
        )
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def verifyMultiFileTransferHasHappened(times: Int = 1) =
    verify(times, postRequestedFor(urlPathEqualTo("/transfer-multiple-files")))

  def verifyMultiTraderServicesFileTransferDidNotHappen() =
    verify(0, postRequestedFor(urlPathEqualTo("/transfer-multiple-files")))

}
