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

package model

import com.typesafe.config.Config
import play.api.ConfigLoader
import play.api.libs.json._

object response {

  import scala.jdk.CollectionConverters._
  case class ErrorMessage(msg: List[String], args: List[String])
  object ErrorMessage {
    val invalidJson: ErrorMessage =
      ErrorMessage(msg = List("error.payload.invalid"), args = List())

    object Implicits {
      implicit val errorMessageReads: Reads[ErrorMessage] =
        Json.reads[ErrorMessage]
      implicit val errorMessageWrites: Writes[ErrorMessage] =
        Json.writes[ErrorMessage]
    }
  }
  case class ErrorResponse(obj: List[ErrorMessage])
  object ErrorResponse {
    val invalidJson = ErrorResponse(obj = List(ErrorMessage.invalidJson))

    object Implicits {
      import ErrorMessage.Implicits._
      implicit val errorResponseReads: Reads[ErrorResponse] =
        Json.reads[ErrorResponse]
      implicit val errorResponseWrites: Writes[ErrorResponse] =
        Json.writes[ErrorResponse]
    }
  }
}
