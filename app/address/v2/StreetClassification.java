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

public enum StreetClassification {

    Footpath(4), // or general pedestrian way
    Cycleway(6),
    All_Vehicles(8),
    Restricted_Byway(9),
    Bridleway(10);

    //-------------------------------------------------------------------------

    public final int code;

    StreetClassification(int code) {
        this.code = code;
    }

    public static StreetClassification lookup(int code) {
        for (StreetClassification v: values()) {
            if (v.code == code) return v;
        }
        return null;
    }
}
