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

public enum BLPUState {

    Under_Construction(1),
    In_Use(2),
    Unoccupied(3), // also covers vacant, derelict
    Demolished(4),
    Planning_Permission_Granted(6);

    //-------------------------------------------------------------------------

    public final int code;

    BLPUState(int code) {
        this.code = code;
    }

    public static BLPUState lookup(int code) {
        for (BLPUState v : values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
