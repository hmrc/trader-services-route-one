/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.traderservices.wiring

import com.google.inject.ImplementedBy
import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {

  val appName: String

  val authBaseUrl: String

  val authorisedServiceName: String

  val authorisedIdentifierKey: String

  val createCaseApiBaseUrl: String

  val createCaseApiPath: String

  val createCaseApiAuthorizationToken: String

  val createCaseApiEnvironment: String
}

class AppConfigImpl @Inject() (config: ServicesConfig) extends AppConfig {

  override val appName: String = config.getString("appName")

  override val authBaseUrl: String = config.baseUrl("auth")

  override val authorisedServiceName: String = config.getString("authorisedServiceName")

  override val authorisedIdentifierKey: String = config.getString("authorisedIdentifierKey")

  override val createCaseApiBaseUrl: String = config.baseUrl("eis.createcaseapi")

  override val createCaseApiPath: String =
    config.getConfString(
      "eis.createcaseapi.path",
      throw new IllegalStateException("Missing [microservice.services.eis.createcaseapi.path] configuration property")
    )

  override val createCaseApiAuthorizationToken: String =
    config.getConfString(
      "eis.createcaseapi.token",
      throw new IllegalStateException("Missing [microservice.services.eis.createcaseapi.token] configuration property")
    )

  override val createCaseApiEnvironment: String =
    config.getConfString(
      "eis.createcaseapi.environment",
      throw new IllegalStateException(
        "Missing [microservice.services.eis.createcaseapi.environment] configuration property"
      )
    )

}
