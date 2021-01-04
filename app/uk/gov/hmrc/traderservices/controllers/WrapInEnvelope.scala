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

package uk.gov.hmrc.traderservices.controllers

import akka.stream.FlowShape
import akka.util.ByteString
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.stage.GraphStageLogic
import akka.stream.Attributes
import akka.stream.stage.OutHandler
import akka.stream.stage.InHandler
import java.nio.charset.StandardCharsets
import akka.stream.stage.GraphStage

class WrapInEnvelope(prefix: String, suffix: String) extends GraphStage[FlowShape[ByteString, ByteString]] {

  val in = Inlet[ByteString]("WrapInEnvelope.in")
  val out = Outlet[ByteString]("WrapInEnvelope.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(
    attr: Attributes
  ): GraphStageLogic = {

    val stageLogic =
      new GraphStageLogic(shape) {

        final override def preStart: Unit =
          setKeepGoing(true)

        setHandler(
          in,
          new InHandler {
            final override def onPush(): Unit = {
              val message = grab(in).toArray
              push(out, ByteString(prefix.getBytes(StandardCharsets.UTF_8)) ++ ByteString(message))
              setHandler(
                in,
                new InHandler {
                  final override def onPush(): Unit = {
                    val message = grab(in).toArray
                    push(out, ByteString(message))
                  }

                  final override def onUpstreamFinish(): Unit = {
                    push(out, ByteString(suffix.getBytes(StandardCharsets.UTF_8)))
                    super.onUpstreamFinish()
                  }
                }
              )
            }
          }
        )

        setHandler(
          out,
          new OutHandler {
            final override def onPull(): Unit =
              pull(in)
          }
        )
      }

    stageLogic
  }

}
