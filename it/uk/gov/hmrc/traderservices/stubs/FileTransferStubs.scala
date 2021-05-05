package uk.gov.hmrc.traderservices.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.traderservices.support.WireMockSupport

trait FileTransferStubs {
  me: WireMockSupport =>

  def givenTraderServicesFileTransferSucceeds(
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

  def givenTraderServicesFileTransferFailure(status: Int): Unit =
    stubFor(
      post(urlPathEqualTo("/transfer-file"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def verifyTraderServicesFileTransferHasHappened(times: Int = 1) =
    verify(times, postRequestedFor(urlPathEqualTo("/transfer-file")))

  def verifyTraderServicesFileTransferDidNotHappen() =
    verify(0, postRequestedFor(urlPathEqualTo("/transfer-file")))

}
