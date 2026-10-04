package com.example.data.model

object LanguageHelper {
    val supportedLanguages = listOf(
        "English" to "English (US)",
        "Nepali" to "नेपाली (Nepal)",
        "Hindi" to "हिन्दी (India)",
        "Bengali" to "বাংলা (Bangladesh)",
        "Arabic" to "العربية (Middle East)",
        "Japanese" to "日本語 (Japan)",
        "Korean" to "한국어 (Korea)",
        "Chinese" to "中文 (China)"
    )

    private val translations = mapOf(
        "Nepali" to mapOf(
            "app_name" to "Lifscan",
            "tagline" to "स्मार्ट स्वास्थ्य र आपतकालीन सेवा",
            "sos_button" to "तत्काल SOS आपतकाल",
            "skin_scanner" to "छाला रोग एआई स्क्यानर",
            "skin_scanner_desc" to "छालाको समस्या पत्ता लगाउनुहोस् र तत्काल उपचार सल्लाह पाउनुहोस्",
            "symptom_scan" to "लक्षण जाँच",
            "ai_chat" to "स्वास्थ्य सहायक च्याट",
            "nearby_facilities" to "नजिकका अस्पताल र क्लिनिक",
            "book_appointment" to "डाक्टरसँग परामर्श",
            "health_reports" to "स्वास्थ्य रिपोर्टहरू",
            "active_sos" to "सक्रिय आपतकालीन सूचना",
            "dispatch_ambulance" to "एम्बुलेन्स बोलाउनुहोस्",
            "doctor_video_call" to "भिडियो कल परामर्श",
            "patient_role" to "बिरामी",
            "doctor_role" to "डाक्टर",
            "driver_role" to "एम्बुलेन्स चालक",
            "login_phone" to "फोन नम्बरबाट लगइन गर्नुहोस्",
            "verify_otp" to "ओटीपी प्रमाणीकरण",
            "pay_now" to "अहिले भुक्तानी गर्नुहोस्",
            "earnings" to "कुल आम्दानी",
            "withdraw" to "रकम निकाल्नुहोस्"
        ),
        "Hindi" to mapOf(
            "app_name" to "Lifscan",
            "tagline" to "स्मार्ट स्वास्थ्य एवं आपातकालीन सेवा",
            "sos_button" to "आपातकालीन SOS बटन",
            "skin_scanner" to "त्वचा रोग एआई स्कैनर",
            "skin_scanner_desc" to "त्वचा रोग का सटीक विश्लेषण और विशेषज्ञ सलाह",
            "symptom_scan" to "लक्षण जांच",
            "ai_chat" to "एआई स्वास्थ्य चैट",
            "nearby_facilities" to "निकटतम अस्पताल और फार्मेसी",
            "book_appointment" to "डॉक्टर अपॉइंटमेंट",
            "health_reports" to "स्वास्थ्य रिकॉर्ड्स",
            "active_sos" to "आपातकालीन चेतावनी",
            "dispatch_ambulance" to "एम्बुलेंस बुलाएं",
            "doctor_video_call" to "वीडियो परामर्श",
            "patient_role" to "मरीज",
            "doctor_role" to "डॉक्टर",
            "driver_role" to "एम्बुलेंस चालक",
            "login_phone" to "फ़ोन नंबर से लॉगिन करें",
            "verify_otp" to "ओटीपी सत्यापन",
            "pay_now" to "भुगतान करें",
            "earnings" to "कुल कमाई",
            "withdraw" to "पैसे निकालें"
        )
    )

    fun getString(lang: String, key: String, default: String): String {
        return translations[lang]?.get(key) ?: default
    }
}
