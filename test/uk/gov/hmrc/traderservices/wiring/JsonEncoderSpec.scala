/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.traderservices.wiring.JsonEncoder
class JsonEncoderSpec extends UnitSpec {

  val encoder = new JsonEncoder

  "JsonEncoder" should {
    "read data prefix" in {
      encoder.jsonDataPrefix shouldBe Seq("route1", "backend")
    }

    "encode json message" in {
      val node = encoder.mapper.createObjectNode()
      encoder.decodeMessage(node, """json{"foo":"bar", "zoo": true}""")
      val message = node.toPrettyString()
      message shouldBe """{
                         |  "message" : "{\"foo\":\"bar\", \"zoo\": true}",
                         |  "route1" : {
                         |    "backend" : {
                         |      "foo" : "bar",
                         |      "zoo" : true
                         |    }
                         |  }
                         |}""".stripMargin
    }
  }
}
