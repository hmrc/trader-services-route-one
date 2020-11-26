package uk.gov.hmrc.traderservices.support

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait TestApplication {
  _: BaseISpec =>

  override implicit lazy val app: Application = appBuilder.build()

  protected override def appBuilder: GuiceApplicationBuilder =
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
        "auditing.consumer.baseUri.port"                               -> wireMockPort
      )

}
