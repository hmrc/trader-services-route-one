package uk.gov.hmrc.traderservices.support

import play.api.inject.guice.GuiceApplicationBuilder

trait TestApplication {
  _: BaseISpec =>

  def defaultAppBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                              -> wireMockPort,
        "microservice.services.eis.cpr.caserequest.route1.host"        -> wireMockHost,
        "microservice.services.eis.cpr.caserequest.route1.port"        -> wireMockPort,
        "microservice.services.eis.cpr.caserequest.route1.token"       -> "dummy-it-token",
        "microservice.services.eis.cpr.caserequest.route1.environment" -> "it",
        "metrics.enabled"                                              -> true,
        "auditing.enabled"                                             -> true,
        "auditing.consumer.baseUri.host"                               -> wireMockHost,
        "auditing.consumer.baseUri.port"                               -> wireMockPort,
        "microservice.services.file-transfer.url"                      -> s"$wireMockBaseUrlAsString/transfer-file",
        "microservice.services.multi-file-transfer.url" -> s"$wireMockBaseUrlAsString/transfer-multiple-files",
        "features.transferFilesAsync"                   -> false
      )
}
