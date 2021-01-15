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

package health

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, DefaultControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PingController @Inject()(controllerComponents: DefaultControllerComponents, transactor: Option[Transactor[IO]])
  extends BackendController(controllerComponents) {

  def ping(): Action[AnyContent] = Action.async { _ =>
    val testPostCode = "W14 9HR"
    transactor match {
      case Some(t) =>
        sql"""SELECT COUNT(*) FROM address_lookup WHERE postcode = $testPostCode"""
          .query[Int].unique.transact(t).unsafeToFuture() map {
          count =>
            if (count > 0) Ok else ServiceUnavailable
        }
      case _ => Future.successful(Ok)
    }
  }
}