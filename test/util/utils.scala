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

package util

import com.typesafe.config.ConfigFactory
import org.scalatest.{Tag, TagAnnotation}
import play.api.test.FakeRequest

import java.lang.annotation._

object Utils {
  lazy val headerOrigin: String =
    ConfigFactory.load().getString("header.x-origin")

  implicit class FakeRequestWithOrigin[T](fake: FakeRequest[T]) {
    def withHeadersOrigin: FakeRequest[T] =
      fake.withHeaders(fake.headers.replace(headerOrigin -> "xyz"))
  }

  object tags {
    @TagAnnotation
    @Retention(RetentionPolicy.RUNTIME)
    @Target(Array(ElementType.METHOD, ElementType.TYPE))
    @Inherited trait MicroBenchmark {}

    object MicroBenchmark extends Tag("util.Utils.MicroBenchmark")
  }
}
