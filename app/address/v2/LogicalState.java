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

package address.v2;

public enum LogicalState {

    Approved(1),
    Alternative(3),
    Provisional(6),
    Historical(8);

    //-------------------------------------------------------------------------

    public final int code;

    LogicalState(int code) {
        this.code = code;
    }

    public static LogicalState lookup(int code) {
        for (LogicalState v: values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
