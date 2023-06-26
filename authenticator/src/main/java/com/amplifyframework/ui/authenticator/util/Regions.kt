/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.ui.authenticator.util

import java.util.Locale

internal data class Region(
    val code: String,
    val dialCode: String
) {
    val flagEmoji by lazy {
        val firstLetter = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        buildString {
            appendCodePoint(firstLetter)
            appendCodePoint(secondLetter)
        }
    }

    val name by lazy<String> {
        Locale("", code).displayName
    }
}

internal val regionList = listOf(
    Region(code = "AF", dialCode = "+93"),
    Region(code = "AX", dialCode = "+358"),
    Region(code = "AL", dialCode = "+355"),
    Region(code = "DZ", dialCode = "+213"),
    Region(code = "AS", dialCode = "+1684"),
    Region(code = "AD", dialCode = "+376"),
    Region(code = "AO", dialCode = "+244"),
    Region(code = "AI", dialCode = "+1264"),
    Region(code = "AQ", dialCode = "+672"),
    Region(code = "AG", dialCode = "+1268"),
    Region(code = "AR", dialCode = "+54"),
    Region(code = "AM", dialCode = "+374"),
    Region(code = "AW", dialCode = "+297"),
    Region(code = "AU", dialCode = "+61"),
    Region(code = "AT", dialCode = "+43"),
    Region(code = "AZ", dialCode = "+994"),
    Region(code = "BS", dialCode = "+1242"),
    Region(code = "BH", dialCode = "+973"),
    Region(code = "BD", dialCode = "+880"),
    Region(code = "BB", dialCode = "+1246"),
    Region(code = "BY", dialCode = "+375"),
    Region(code = "BE", dialCode = "+32"),
    Region(code = "BZ", dialCode = "+501"),
    Region(code = "BJ", dialCode = "+229"),
    Region(code = "BM", dialCode = "+1441"),
    Region(code = "BT", dialCode = "+975"),
    Region(code = "BO", dialCode = "+591"),
    Region(code = "BA", dialCode = "+387"),
    Region(code = "BW", dialCode = "+267"),
    Region(code = "BR", dialCode = "+55"),
    Region(code = "IO", dialCode = "+246"),
    Region(code = "BN", dialCode = "+673"),
    Region(code = "BG", dialCode = "+359"),
    Region(code = "BF", dialCode = "+226"),
    Region(code = "BI", dialCode = "+257"),
    Region(code = "KH", dialCode = "+855"),
    Region(code = "CM", dialCode = "+237"),
    Region(code = "CA", dialCode = "+1"),
    Region(code = "CV", dialCode = "+238"),
    Region(code = "KY", dialCode = "+ 345"),
    Region(code = "CF", dialCode = "+236"),
    Region(code = "TD", dialCode = "+235"),
    Region(code = "CL", dialCode = "+56"),
    Region(code = "CN", dialCode = "+86"),
    Region(code = "CX", dialCode = "+61"),
    Region(code = "CC", dialCode = "+61"),
    Region(code = "CO", dialCode = "+57"),
    Region(code = "KM", dialCode = "+269"),
    Region(code = "CG", dialCode = "+242"),
    Region(code = "CD", dialCode = "+243"),
    Region(code = "CK", dialCode = "+682"),
    Region(code = "CR", dialCode = "+506"),
    Region(code = "CI", dialCode = "+225"),
    Region(code = "HR", dialCode = "+385"),
    Region(code = "CU", dialCode = "+53"),
    Region(code = "CY", dialCode = "+357"),
    Region(code = "CZ", dialCode = "+420"),
    Region(code = "DK", dialCode = "+45"),
    Region(code = "DJ", dialCode = "+253"),
    Region(code = "DM", dialCode = "+1767"),
    Region(code = "DO", dialCode = "+1849"),
    Region(code = "EC", dialCode = "+593"),
    Region(code = "EG", dialCode = "+20"),
    Region(code = "SV", dialCode = "+503"),
    Region(code = "GQ", dialCode = "+240"),
    Region(code = "ER", dialCode = "+291"),
    Region(code = "EE", dialCode = "+372"),
    Region(code = "ET", dialCode = "+251"),
    Region(code = "FK", dialCode = "+500"),
    Region(code = "FO", dialCode = "+298"),
    Region(code = "FJ", dialCode = "+679"),
    Region(code = "FI", dialCode = "+358"),
    Region(code = "FR", dialCode = "+33"),
    Region(code = "GF", dialCode = "+594"),
    Region(code = "PF", dialCode = "+689"),
    Region(code = "GA", dialCode = "+241"),
    Region(code = "GM", dialCode = "+220"),
    Region(code = "GE", dialCode = "+995"),
    Region(code = "DE", dialCode = "+49"),
    Region(code = "GH", dialCode = "+233"),
    Region(code = "GI", dialCode = "+350"),
    Region(code = "GR", dialCode = "+30"),
    Region(code = "GL", dialCode = "+299"),
    Region(code = "GD", dialCode = "+1473"),
    Region(code = "GP", dialCode = "+590"),
    Region(code = "GU", dialCode = "+1671"),
    Region(code = "GT", dialCode = "+502"),
    Region(code = "GG", dialCode = "+44"),
    Region(code = "GN", dialCode = "+224"),
    Region(code = "GW", dialCode = "+245"),
    Region(code = "GY", dialCode = "+595"),
    Region(code = "HT", dialCode = "+509"),
    Region(code = "VA", dialCode = "+379"),
    Region(code = "HN", dialCode = "+504"),
    Region(code = "HK", dialCode = "+852"),
    Region(code = "HU", dialCode = "+36"),
    Region(code = "IS", dialCode = "+354"),
    Region(code = "IN", dialCode = "+91"),
    Region(code = "ID", dialCode = "+62"),
    Region(code = "IR", dialCode = "+98"),
    Region(code = "IQ", dialCode = "+964"),
    Region(code = "IE", dialCode = "+353"),
    Region(code = "IM", dialCode = "+44"),
    Region(code = "IL", dialCode = "+972"),
    Region(code = "IT", dialCode = "+39"),
    Region(code = "JM", dialCode = "+1876"),
    Region(code = "JP", dialCode = "+81"),
    Region(code = "JE", dialCode = "+44"),
    Region(code = "JO", dialCode = "+962"),
    Region(code = "KZ", dialCode = "+77"),
    Region(code = "KE", dialCode = "+254"),
    Region(code = "KI", dialCode = "+686"),
    Region(code = "KP", dialCode = "+850"),
    Region(code = "KR", dialCode = "+82"),
    Region(code = "KW", dialCode = "+965"),
    Region(code = "KG", dialCode = "+996"),
    Region(code = "LA", dialCode = "+856"),
    Region(code = "LV", dialCode = "+371"),
    Region(code = "LB", dialCode = "+961"),
    Region(code = "LS", dialCode = "+266"),
    Region(code = "LR", dialCode = "+231"),
    Region(code = "LY", dialCode = "+218"),
    Region(code = "LI", dialCode = "+423"),
    Region(code = "LT", dialCode = "+370"),
    Region(code = "LU", dialCode = "+352"),
    Region(code = "MO", dialCode = "+853"),
    Region(code = "MK", dialCode = "+389"),
    Region(code = "MG", dialCode = "+261"),
    Region(code = "MW", dialCode = "+265"),
    Region(code = "MY", dialCode = "+60"),
    Region(code = "MV", dialCode = "+960"),
    Region(code = "ML", dialCode = "+223"),
    Region(code = "MT", dialCode = "+356"),
    Region(code = "MH", dialCode = "+692"),
    Region(code = "MQ", dialCode = "+596"),
    Region(code = "MR", dialCode = "+222"),
    Region(code = "MU", dialCode = "+230"),
    Region(code = "YT", dialCode = "+262"),
    Region(code = "MX", dialCode = "+52"),
    Region(code = "FM", dialCode = "+691"),
    Region(code = "MD", dialCode = "+373"),
    Region(code = "MC", dialCode = "+377"),
    Region(code = "MN", dialCode = "+976"),
    Region(code = "ME", dialCode = "+382"),
    Region(code = "MS", dialCode = "+1664"),
    Region(code = "MA", dialCode = "+212"),
    Region(code = "MZ", dialCode = "+258"),
    Region(code = "MM", dialCode = "+95"),
    Region(code = "NA", dialCode = "+264"),
    Region(code = "NR", dialCode = "+674"),
    Region(code = "NP", dialCode = "+977"),
    Region(code = "NL", dialCode = "+31"),
    Region(code = "AN", dialCode = "+599"),
    Region(code = "NC", dialCode = "+687"),
    Region(code = "NZ", dialCode = "+64"),
    Region(code = "NI", dialCode = "+505"),
    Region(code = "NE", dialCode = "+227"),
    Region(code = "NG", dialCode = "+234"),
    Region(code = "NU", dialCode = "+683"),
    Region(code = "NF", dialCode = "+672"),
    Region(code = "MP", dialCode = "+1670"),
    Region(code = "NO", dialCode = "+47"),
    Region(code = "OM", dialCode = "+968"),
    Region(code = "PK", dialCode = "+92"),
    Region(code = "PW", dialCode = "+680"),
    Region(code = "PS", dialCode = "+970"),
    Region(code = "PA", dialCode = "+507"),
    Region(code = "PG", dialCode = "+675"),
    Region(code = "PY", dialCode = "+595"),
    Region(code = "PE", dialCode = "+51"),
    Region(code = "PH", dialCode = "+63"),
    Region(code = "PN", dialCode = "+872"),
    Region(code = "PL", dialCode = "+48"),
    Region(code = "PT", dialCode = "+351"),
    Region(code = "PR", dialCode = "+1939"),
    Region(code = "QA", dialCode = "+974"),
    Region(code = "RO", dialCode = "+40"),
    Region(code = "RU", dialCode = "+7"),
    Region(code = "RW", dialCode = "+250"),
    Region(code = "RE", dialCode = "+262"),
    Region(code = "BL", dialCode = "+590"),
    Region(code = "SH", dialCode = "+290"),
    Region(code = "KN", dialCode = "+1869"),
    Region(code = "LC", dialCode = "+1758"),
    Region(code = "MF", dialCode = "+590"),
    Region(code = "PM", dialCode = "+508"),
    Region(code = "VC", dialCode = "+1784"),
    Region(code = "WS", dialCode = "+685"),
    Region(code = "SM", dialCode = "+378"),
    Region(code = "ST", dialCode = "+239"),
    Region(code = "SA", dialCode = "+966"),
    Region(code = "SN", dialCode = "+221"),
    Region(code = "RS", dialCode = "+381"),
    Region(code = "SC", dialCode = "+248"),
    Region(code = "SL", dialCode = "+232"),
    Region(code = "SG", dialCode = "+65"),
    Region(code = "SK", dialCode = "+421"),
    Region(code = "SI", dialCode = "+386"),
    Region(code = "SB", dialCode = "+677"),
    Region(code = "SO", dialCode = "+252"),
    Region(code = "ZA", dialCode = "+27"),
    Region(code = "SS", dialCode = "+211"),
    Region(code = "GS", dialCode = "+500"),
    Region(code = "ES", dialCode = "+34"),
    Region(code = "LK", dialCode = "+94"),
    Region(code = "SD", dialCode = "+249"),
    Region(code = "SR", dialCode = "+597"),
    Region(code = "SJ", dialCode = "+47"),
    Region(code = "SZ", dialCode = "+268"),
    Region(code = "SE", dialCode = "+46"),
    Region(code = "CH", dialCode = "+41"),
    Region(code = "SY", dialCode = "+963"),
    Region(code = "TW", dialCode = "+886"),
    Region(code = "TJ", dialCode = "+992"),
    Region(code = "TZ", dialCode = "+255"),
    Region(code = "TH", dialCode = "+66"),
    Region(code = "TL", dialCode = "+670"),
    Region(code = "TG", dialCode = "+228"),
    Region(code = "TK", dialCode = "+690"),
    Region(code = "TO", dialCode = "+676"),
    Region(code = "TT", dialCode = "+1868"),
    Region(code = "TN", dialCode = "+216"),
    Region(code = "TR", dialCode = "+90"),
    Region(code = "TM", dialCode = "+993"),
    Region(code = "TC", dialCode = "+1649"),
    Region(code = "TV", dialCode = "+688"),
    Region(code = "UG", dialCode = "+256"),
    Region(code = "UA", dialCode = "+380"),
    Region(code = "AE", dialCode = "+971"),
    Region(code = "GB", dialCode = "+44"),
    Region(code = "US", dialCode = "+1"),
    Region(code = "UY", dialCode = "+598"),
    Region(code = "UZ", dialCode = "+998"),
    Region(code = "VU", dialCode = "+678"),
    Region(code = "VE", dialCode = "+58"),
    Region(code = "VN", dialCode = "+84"),
    Region(code = "VG", dialCode = "+1284"),
    Region(code = "VI", dialCode = "+1340"),
    Region(code = "WF", dialCode = "+681"),
    Region(code = "YE", dialCode = "+967"),
    Region(code = "ZM", dialCode = "+260"),
    Region(code = "ZW", dialCode = "+263")
)

internal val regionMap = regionList.associateBy { it.code }
