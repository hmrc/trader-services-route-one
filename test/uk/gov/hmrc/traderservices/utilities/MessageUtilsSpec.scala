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

package uk.gov.hmrc.traderservices.utilities

import uk.gov.hmrc.traderservices.support.UnitSpec
import uk.gov.hmrc.traderservices.stubs.MessageUtils

class MessageUtilsSpec extends UnitSpec {
  "MessageUtils" should {
    "encode string as Base64 and calculate SHA-256 checksum" in {
      MessageUtils.encodeBase64AndCalculateSHA256("") shouldBe (
        (
          "",
          "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )
      )
      MessageUtils.encodeBase64AndCalculateSHA256("a") shouldBe (
        (
          "YQ==",
          "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb"
        )
      )
      MessageUtils.encodeBase64AndCalculateSHA256("Hello!") shouldBe (
        (
          "SGVsbG8h",
          "334d016f755cd6dc58c53a86e183882f8ec14f52fb05345887c8a5edd42c87b7"
        )
      )
      MessageUtils.encodeBase64AndCalculateSHA256(
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin tempus tempor est in dapibus. Maecenas dignissim elit in tempus vehicula. Nullam nunc eros, laoreet eu augue a, elementum mattis leo. Vestibulum accumsan semper felis, ac commodo velit consequat nec. Nulla facilisi. Sed ac dui eu velit porta pharetra sollicitudin id nunc. Aenean ipsum ipsum, aliquam eget nisi id, eleifend dapibus quam. Vivamus in imperdiet diam. Vestibulum nec interdum nisl. Praesent sed massa nec est pellentesque accumsan eu in mi. Vivamus pellentesque purus eleifend eros ornare, non cursus dolor bibendum. Donec feugiat diam sit amet rhoncus condimentum. Morbi sagittis vitae nulla."
      ) shouldBe (
        (
          "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4gUHJvaW4gdGVtcHVzIHRlbXBvciBlc3QgaW4gZGFwaWJ1cy4gTWFlY2VuYXMgZGlnbmlzc2ltIGVsaXQgaW4gdGVtcHVzIHZlaGljdWxhLiBOdWxsYW0gbnVuYyBlcm9zLCBsYW9yZWV0IGV1IGF1Z3VlIGEsIGVsZW1lbnR1bSBtYXR0aXMgbGVvLiBWZXN0aWJ1bHVtIGFjY3Vtc2FuIHNlbXBlciBmZWxpcywgYWMgY29tbW9kbyB2ZWxpdCBjb25zZXF1YXQgbmVjLiBOdWxsYSBmYWNpbGlzaS4gU2VkIGFjIGR1aSBldSB2ZWxpdCBwb3J0YSBwaGFyZXRyYSBzb2xsaWNpdHVkaW4gaWQgbnVuYy4gQWVuZWFuIGlwc3VtIGlwc3VtLCBhbGlxdWFtIGVnZXQgbmlzaSBpZCwgZWxlaWZlbmQgZGFwaWJ1cyBxdWFtLiBWaXZhbXVzIGluIGltcGVyZGlldCBkaWFtLiBWZXN0aWJ1bHVtIG5lYyBpbnRlcmR1bSBuaXNsLiBQcmFlc2VudCBzZWQgbWFzc2EgbmVjIGVzdCBwZWxsZW50ZXNxdWUgYWNjdW1zYW4gZXUgaW4gbWkuIFZpdmFtdXMgcGVsbGVudGVzcXVlIHB1cnVzIGVsZWlmZW5kIGVyb3Mgb3JuYXJlLCBub24gY3Vyc3VzIGRvbG9yIGJpYmVuZHVtLiBEb25lYyBmZXVnaWF0IGRpYW0gc2l0IGFtZXQgcmhvbmN1cyBjb25kaW1lbnR1bS4gTW9yYmkgc2FnaXR0aXMgdml0YWUgbnVsbGEu",
          "ab8615431fec545eba5e66c0c6fa460b531dff9b5a18bde3b28dca7c4b5dffaa"
        )
      )
    }
  }
}
