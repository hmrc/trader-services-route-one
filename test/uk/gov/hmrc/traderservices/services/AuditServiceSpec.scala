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

package uk.gov.hmrc.traderservices.services

import uk.gov.hmrc.traderservices.support.UnitSpec
import play.api.libs.json._

class AuditServiceSpec extends UnitSpec {

  "AuditService" should {
    "extract details from json" in {
      AuditService.detailsFromJson(JsNull) shouldBe Seq.empty
      AuditService.detailsFromJson(JsTrue) shouldBe Seq("" -> true)
      AuditService.detailsFromJson(JsFalse) shouldBe Seq("" -> false)
      AuditService.detailsFromJson(JsString("foo bar")) shouldBe Seq("" -> "foo bar")
      AuditService.detailsFromJson(JsNumber(10.12d)) shouldBe Seq("" -> 10.12d)

      AuditService.detailsFromJson(Json.obj("foo" -> JsNull)) shouldBe Seq.empty
      AuditService.detailsFromJson(Json.obj("foo" -> true)) shouldBe Seq("foo" -> true)
      AuditService.detailsFromJson(Json.obj("bar" -> false)) shouldBe Seq("bar" -> false)
      AuditService.detailsFromJson(Json.obj("foo" -> "bar")) shouldBe Seq("foo" -> "bar")
      AuditService.detailsFromJson(Json.obj("foo" -> 0.001d)) shouldBe Seq("foo" -> 0.001d)

      AuditService.detailsFromJson(Json.arr("a")) shouldBe Seq("0" -> "a")
      AuditService.detailsFromJson(Json.arr("a", 0, true)) shouldBe Seq("0" -> "a", "1" -> 0, "2" -> true)
      AuditService
        .detailsFromJson(Json.arr("foo", Json.obj("foo" -> "bar"))) shouldBe Seq("0" -> "foo", "1.foo" -> "bar")
      AuditService
        .detailsFromJson(Json.arr(Json.obj("foo" -> "bar"), Json.obj("foo" -> "baz"))) shouldBe Seq(
        "0.foo" -> "bar",
        "1.foo" -> "baz"
      )

      AuditService.detailsFromJson(Json.obj("foo" -> Json.obj("bar" -> 1))) shouldBe Seq("foo.bar" -> 1)

      AuditService.detailsFromJson(Json.obj("foo" -> Json.obj("bar" -> 1, "baz" -> "foz"))) shouldBe Seq(
        "foo.bar" -> 1,
        "foo.baz" -> "foz"
      )

      AuditService.detailsFromJson(
        Json.obj(
          "foo" -> Json.obj("bar" -> 1, "baz" -> "foz"),
          "faz" -> Json.obj("bar" -> Json.obj("bar" -> true), "foz" -> "baz")
        )
      ) shouldBe Seq(
        "foo.bar"     -> 1,
        "foo.baz"     -> "foz",
        "faz.bar.bar" -> true,
        "faz.foz"     -> "baz"
      )

      AuditService.detailsFromJson(
        Json.obj(
          "foo" -> Json.arr(Json.obj("fos" -> 1), Json.obj("fos" -> "baz")),
          "faz" -> Json.arr(Json.obj("fas" -> "bar"), Json.obj("fas" -> false))
        )
      ) shouldBe Seq(
        "foo.0.fos" -> 1,
        "foo.1.fos" -> "baz",
        "faz.0.fas" -> "bar",
        "faz.1.fas" -> false
      )
    }
  }

}
