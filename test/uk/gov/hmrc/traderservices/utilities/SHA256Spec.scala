/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.traderservices.utilities

import uk.gov.hmrc.traderservices.support.UnitSpec

class SHA256Spec extends UnitSpec {
  "SHA256S" should {
    "compute SHA-256 hash" in {
      SHA256.compute("a") shouldBe "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb"
      SHA256.compute("Hello World!") shouldBe "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"
    }
  }
}
