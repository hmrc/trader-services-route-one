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

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

object SHA256 {

  final val digest = MessageDigest.getInstance("SHA-256")

  final def compute(string: String): String =
    bytesToHex(digest.digest(string.getBytes(StandardCharsets.UTF_8)))

  private def bytesToHex(hash: Array[Byte]): String = {
    val hexString = new StringBuilder(2 * hash.length)
    for (i <- 0 until hash.length) {
      val hex = Integer.toHexString(0xff & hash(i))
      if (hex.length() == 1)
        hexString.append('0')
      hexString.append(hex)
    }
    hexString.toString()
  }
}
