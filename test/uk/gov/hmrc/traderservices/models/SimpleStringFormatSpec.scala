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

class SimpleStringFormatSpec extends UnitSpec {

  case class Example(foo: String, bar: Int)

  val format = SimpleStringFormat[Example](
    s => { val a = s.split(":"); Example(a(0), a(1).toInt) },
    e => s"${e.foo}:${e.bar}"
  )

  val beJsError = matchPattern { case JsError(_) => }

  "SimpleStringFormat" should {
    "serialize an entity to string" in {
      format.writes(Example("", 0)) shouldBe JsString(":0")
      format.writes(Example("a", 1)) shouldBe JsString("a:1")
      format.writes(Example("abc", 123)) shouldBe JsString("abc:123")
    }

    "deserialize an entity from string" in {
      format.reads(JsString(":0")) shouldBe JsSuccess(Example("", 0))
      format.reads(JsString("a:1")) shouldBe JsSuccess(Example("a", 1))
      format.reads(JsString("abc:123")) shouldBe JsSuccess(Example("abc", 123))

      format.reads(JsNull) should beJsError
      format.reads(Json.obj()) should beJsError
      format.reads(Json.arr()) should beJsError
      format.reads(JsNumber(1)) should beJsError
      format.reads(JsBoolean(true)) should beJsError
      format.reads(JsBoolean(false)) should beJsError
    }
  }

}
