package com.example.data.model

object AsianCountriesProvider {
    val countries: List<AsianCountry> = listOf(
        AsianCountry(
            code = "NP",
            name = "Nepal",
            dialCode = "+977",
            flagEmoji = "🇳🇵",
            currency = "NPR",
            currencySymbol = "रू",
            defaultEmergencyNumber = "102",
            exampleNumber = "9841234567"
        ),
        AsianCountry(
            code = "IN",
            name = "India",
            dialCode = "+91",
            flagEmoji = "🇮🇳",
            currency = "INR",
            currencySymbol = "₹",
            defaultEmergencyNumber = "112",
            exampleNumber = "9876543210"
        ),
        AsianCountry(
            code = "BD",
            name = "Bangladesh",
            dialCode = "+880",
            flagEmoji = "🇧🇩",
            currency = "BDT",
            currencySymbol = "৳",
            defaultEmergencyNumber = "999",
            exampleNumber = "1712345678"
        ),
        AsianCountry(
            code = "LK",
            name = "Sri Lanka",
            dialCode = "+94",
            flagEmoji = "🇱🇰",
            currency = "LKR",
            currencySymbol = "Rs",
            defaultEmergencyNumber = "1990",
            exampleNumber = "712345678"
        ),
        AsianCountry(
            code = "PK",
            name = "Pakistan",
            dialCode = "+92",
            flagEmoji = "🇵🇰",
            currency = "PKR",
            currencySymbol = "Rs",
            defaultEmergencyNumber = "1122",
            exampleNumber = "3001234567"
        ),
        AsianCountry(
            code = "BT",
            name = "Bhutan",
            dialCode = "+975",
            flagEmoji = "🇧🇹",
            currency = "BTN",
            currencySymbol = "Nu.",
            defaultEmergencyNumber = "112",
            exampleNumber = "17123456"
        ),
        AsianCountry(
            code = "MV",
            name = "Maldives",
            dialCode = "+960",
            flagEmoji = "🇲🇻",
            currency = "MVR",
            currencySymbol = "Rf",
            defaultEmergencyNumber = "102",
            exampleNumber = "7712345"
        ),
        AsianCountry(
            code = "SG",
            name = "Singapore",
            dialCode = "+65",
            flagEmoji = "🇸🇬",
            currency = "SGD",
            currencySymbol = "S$",
            defaultEmergencyNumber = "995",
            exampleNumber = "91234567"
        ),
        AsianCountry(
            code = "MY",
            name = "Malaysia",
            dialCode = "+60",
            flagEmoji = "🇲🇾",
            currency = "MYR",
            currencySymbol = "RM",
            defaultEmergencyNumber = "999",
            exampleNumber = "123456789"
        ),
        AsianCountry(
            code = "ID",
            name = "Indonesia",
            dialCode = "+62",
            flagEmoji = "🇮🇩",
            currency = "IDR",
            currencySymbol = "Rp",
            defaultEmergencyNumber = "119",
            exampleNumber = "81234567890"
        ),
        AsianCountry(
            code = "PH",
            name = "Philippines",
            dialCode = "+63",
            flagEmoji = "🇵🇭",
            currency = "PHP",
            currencySymbol = "₱",
            defaultEmergencyNumber = "911",
            exampleNumber = "9171234567"
        ),
        AsianCountry(
            code = "TH",
            name = "Thailand",
            dialCode = "+66",
            flagEmoji = "🇹🇭",
            currency = "THB",
            currencySymbol = "฿",
            defaultEmergencyNumber = "1669",
            exampleNumber = "812345678"
        ),
        AsianCountry(
            code = "VN",
            name = "Vietnam",
            dialCode = "+84",
            flagEmoji = "🇻🇳",
            currency = "VND",
            currencySymbol = "₫",
            defaultEmergencyNumber = "115",
            exampleNumber = "912345678"
        ),
        AsianCountry(
            code = "JP",
            name = "Japan",
            dialCode = "+81",
            flagEmoji = "🇯🇵",
            currency = "JPY",
            currencySymbol = "¥",
            defaultEmergencyNumber = "119",
            exampleNumber = "9012345678"
        ),
        AsianCountry(
            code = "KR",
            name = "South Korea",
            dialCode = "+82",
            flagEmoji = "🇰🇷",
            currency = "KRW",
            currencySymbol = "₩",
            defaultEmergencyNumber = "119",
            exampleNumber = "1012345678"
        ),
        AsianCountry(
            code = "CN",
            name = "China",
            dialCode = "+86",
            flagEmoji = "🇨🇳",
            currency = "CNY",
            currencySymbol = "¥",
            defaultEmergencyNumber = "120",
            exampleNumber = "13800138000"
        ),
        AsianCountry(
            code = "AE",
            name = "United Arab Emirates",
            dialCode = "+971",
            flagEmoji = "🇦🇪",
            currency = "AED",
            currencySymbol = "د.إ",
            defaultEmergencyNumber = "998",
            exampleNumber = "501234567"
        ),
        AsianCountry(
            code = "SA",
            name = "Saudi Arabia",
            dialCode = "+966",
            flagEmoji = "🇸🇦",
            currency = "SAR",
            currencySymbol = "﷼",
            defaultEmergencyNumber = "997",
            exampleNumber = "501234567"
        ),
        AsianCountry(
            code = "QA",
            name = "Qatar",
            dialCode = "+974",
            flagEmoji = "🇶🇦",
            currency = "QAR",
            currencySymbol = "QR",
            defaultEmergencyNumber = "999",
            exampleNumber = "33123456"
        )
    )

    fun getCountryByDialCode(dialCode: String): AsianCountry {
        return countries.find { it.dialCode == dialCode } ?: countries.first()
    }
}
