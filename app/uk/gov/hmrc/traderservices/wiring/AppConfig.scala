/*
 * Copyright 2022 HM Revenue & Customs
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

  lazy val fileTransferUrl: String = ""

  lazy val multiFileTransferUrl: String = ""

  val eisBaseUrl: String

  val eisCreateCaseApiPath: String

  val eisUpdateCaseApiPath: String

  val eisAuthorizationToken: String

  val eisEnvironment: String

  val transferFilesAsync: Boolean
}

class AppConfigImpl @Inject() (config: ServicesConfig) extends AppConfig {

  override val appName: String = config.getString("appName")

  override val authBaseUrl: String = config.baseUrl("auth")

  override val authorisedServiceName: String = config.getString("authorisedServiceName")

  override val authorisedIdentifierKey: String = config.getString("authorisedIdentifierKey")

  override lazy val fileTransferUrl: String =
    config.getConfString(
      "file-transfer.url",
      throw new IllegalStateException(
        "Missing [microservice.services.file-transfer.url] configuration property"
      )
    )

  override lazy val multiFileTransferUrl: String =
    config.getConfString(
      "multi-file-transfer.url",
      throw new IllegalStateException(
        "Missing [microservice.services.multi-file-transfer.url] configuration property"
      )
    )

  override val eisBaseUrl: String = config.baseUrl("eis.cpr.caserequest.route1")

  override val eisCreateCaseApiPath: String =
    config.getConfString(
      "eis.cpr.caserequest.route1.create.path",
      throw new IllegalStateException(
        "Missing [microservice.services.eis.cpr.caserequest.route1.create.path] configuration property"
      )
    )

  override val eisUpdateCaseApiPath: String =
    config.getConfString(
      "eis.cpr.caserequest.route1.update.path",
      throw new IllegalStateException(
        "Missing [microservice.services.eis.cpr.caserequest.route1.update.path] configuration property"
      )
    )

  override val eisAuthorizationToken: String =
    config.getConfString(
      "eis.cpr.caserequest.route1.token",
      throw new IllegalStateException(
        "Missing [microservice.services.eis.cpr.caserequest.route1.token] configuration property"
      )
    )

  override val eisEnvironment: String =
    config.getConfString(
      "eis.cpr.caserequest.route1.environment",
      throw new IllegalStateException(
        "Missing [microservice.services.eis.cpr.caserequest.route1.environment] configuration property"
      )
    )

  override val transferFilesAsync: Boolean =
    config.getBoolean("features.transferFilesAsync")

}
