package com.suitefish.suitefishapk.util;

import java.util.Locale;

/**
 * Maps the device UI language to the API language code sent to the catalog server.
 * English is the fallback for any unsupported language.
 *
 * Note: a few API codes differ from the ISO language code Android reports —
 * Korean is "kr" (Android "ko") and the Indian/Hindi option is "in" (Android "hi").
 */
public final class LanguageUtils {

    private LanguageUtils() {
    }

    /** @return one of the supported API codes, defaulting to "en". */
    public static String apiLanguageCode() {
        String lang = Locale.getDefault().getLanguage();
        if (lang == null) return "en";
        switch (lang.toLowerCase(Locale.ROOT)) {
            case "de": return "de";
            case "es": return "es";
            case "fr": return "fr";
            case "hi": return "in"; // Hindi -> API "in"
            case "in": return "in"; // legacy code some devices report
            case "it": return "it";
            case "ja": return "ja";
            case "ko": return "kr"; // Korean -> API "kr"
            case "pt": return "pt";
            case "ru": return "ru";
            case "tr": return "tr";
            case "zh": return "zh"; // Chinese (Simplified)
            case "en":
            default:
                return "en";
        }
    }
}
