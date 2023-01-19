/*
 * Copyright 2023 HM Revenue & Customs
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
        "microservice.services.multi-file-transfer.url"                -> s"$wireMockBaseUrlAsString/transfer-multiple-files",
        "features.transferFilesAsync"                                  -> false
      )
}
