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

package uk.gov.hmrc.traderservices.models

import uk.gov.hmrc.traderservices.support.UnitSpec
import play.api.libs.json._

class EnumerationFormatsSpec extends UnitSpec {

  sealed trait Example
  object Example extends EnumerationFormats[Example] {
    case object FOO extends Example
    case object BAR extends Example
    case object BAZ extends Example
    case object ZOO extends Example //not listed in values!
    override val values: Set[Example] = Set(FOO, BAR, BAZ)
  }

  "EnumerationFormats" should {
    "serialize enum values" in {
      Example.format.writes(Example.BAR) shouldBe JsString("BAR")
      Example.format.writes(Example.FOO) shouldBe JsString("FOO")
      Example.format.writes(Example.BAZ) shouldBe JsString("BAZ")
      an[IllegalStateException] shouldBe
        thrownBy(Example.format.writes(Example.ZOO))
    }

    "deserialize enum values" in {
      Example.format.reads(JsString("BAR")) shouldBe JsSuccess(Example.BAR)
      Example.format.reads(JsString("FOO")) shouldBe JsSuccess(Example.FOO)
      Example.format.reads(JsString("BAZ")) shouldBe JsSuccess(Example.BAZ)
      Example.format.reads(JsString("ZOO")) shouldBe a[JsError] // enum not listed in values
      Example.format.reads(JsString("XYZ")) shouldBe a[JsError] // not an enum

    }
  }

}
