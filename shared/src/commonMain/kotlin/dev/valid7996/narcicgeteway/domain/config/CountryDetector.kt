package dev.valid7996.narcicgeteway.domain.config

/**
 * Intelligent country detector for VPN configs.
 * First checks for Unicode regional indicator flags (e.g. 🇺🇸, 🇩🇪),
 * then matches against country names and major server cities (in English and Persian).
 */
object CountryDetector {

    fun extractFlagEmoji(text: String): String? {
        var i = 0
        while (i <= text.length - 4) {
            if (text[i] == '\uD83C' && text[i + 1] in '\uDDE6'..'\uDDFF' &&
                text[i + 2] == '\uD83C' && text[i + 3] in '\uDDE6'..'\uDDFF') {
                val c1 = ('A'.code + (text[i + 1].code - 0xDDE6)).toChar()
                val c2 = ('A'.code + (text[i + 3].code - 0xDDE6)).toChar()
                return "$c1$c2"
            }
            i++
        }
        return null
    }

    fun detect(name: String?, address: String? = null): String? {
        val flag = name?.let { extractFlagEmoji(it) }
        if (flag != null) return flag

        val combined = "${name.orEmpty()} ${address.orEmpty()}".lowercase()
        if (combined.isBlank()) return null

        for ((code, keywords) in KEYWORDS) {
            for (kw in keywords) {
                if (combined.contains(kw)) {
                    return code
                }
            }
        }
        return null
    }

    private val KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "US" to listOf(
            "united states", "america", "atalanta", "atlanta", "dallas", "new york", "los angeles",
            "chicago", "miami", "seattle", "san jose", "silicon valley", "phoenix", "ashburn",
            "virginia", "california", "ohio", "واشنگتن", "نیویورک", "دالاس", "شیکاگو", "میامی",
            "کالیفرنیا", "آمریکا", "ایالات متحده", "-us-", ".us.", " us ", "(us)", "[us]", "_us_",
            "us-", "-us", "usa", "atl-", "jfk-", "dfw-", "ord-", "lax-", "mia-", "sea-",
        ),
        "FR" to listOf(
            "france", "paris", "marseille", "lyon", "پاریس", "فرانسه", "-fr-", ".fr.", " fr ",
            "(fr)", "[fr]", "_fr_", "fr-", "-fr", "fra", "cdg-", "ory-",
        ),
        "DE" to listOf(
            "germany", "deutschland", "frankfurt", "berlin", "munich", "dusseldorf", "hamburg",
            "فرانکفورت", "برلین", "مونیخ", "آلمان", "-de-", ".de.", " de ", "(de)", "[de]", "_de_",
            "de-", "-de", "ger", "fra-", "muc-",
        ),
        "GB" to listOf(
            "united kingdom", "great britain", "england", "london", "manchester", "لندن",
            "بریتانیا", "انگلیس", "-uk-", ".uk.", " uk ", "(uk)", "[uk]", "_uk_", "uk-", "-uk",
            "-gb-", ".gb.", " gb ", "(gb)", "[gb]", "_gb_",
        ),
        "CA" to listOf(
            "canada", "toronto", "montreal", "vancouver", "calgary", "ottawa", "تورنتو",
            "مونترال", "ونکوور", "کانادا", "-ca-", ".ca.", " ca ", "(ca)", "[ca]", "_ca_", "ca-", "-ca",
            "tor-", "yvr-", "yyz-",
        ),
        "NL" to listOf(
            "netherlands", "holland", "amsterdam", "rotterdam", "آمستردام", "هلند",
            "-nl-", ".nl.", " nl ", "(nl)", "[nl]", "_nl_", "nl-", "-nl", "nld", "ams-",
        ),
        "IL" to listOf(
            "israel", "tel aviv", "jerusalem", "تل آویو", "اسرائیل",
            "-il-", ".il.", " il ", "(il)", "[il]", "_il_", "il-", "-il", "isr", "tlv-",
        ),
        "IN" to listOf(
            "india", "mumbai", "delhi", "bangalore", "دهلی", "بمبئی", "هند",
            "-in-", ".in.", " in ", "(in)", "[in]", "_in_", "in-", "-in", "ind",
        ),
        "JP" to listOf(
            "japan", "tokyo", "osaka", "توکیو", "ژاپن",
            "-jp-", ".jp.", " jp ", "(jp)", "[jp]", "_jp_", "jp-", "-jp", "jpn",
        ),
        "ES" to listOf(
            "spain", "espana", "madrid", "barcelona", "مادرید", "بارسلونا", "اسپانیا",
            "-es-", ".es.", " es ", "(es)", "[es]", "_es_", "es-", "-es", "esp",
        ),
        "IT" to listOf(
            "italy", "italia", "milan", "milano", "rome", "roma", "میلان", "رم", "ایتالیا",
            "-it-", ".it.", " it ", "(it)", "[it]", "_it_", "it-", "-it", "ita",
        ),
        "TR" to listOf(
            "turkey", "turkiye", "istanbul", "ankara", "استانبول", "آنکارا", "ترکیه",
            "-tr-", ".tr.", " tr ", "(tr)", "[tr]", "_tr_", "tr-", "-tr", "tur",
        ),
        "RU" to listOf(
            "russia", "moscow", "saint petersburg", "مسکو", "روسیه",
            "-ru-", ".ru.", " ru ", "(ru)", "[ru]", "_ru_", "ru-", "-ru", "rus",
        ),
        "SG" to listOf(
            "singapore", "سنگاپور", "-sg-", ".sg.", " sg ", "(sg)", "[sg]", "_sg_", "sg-", "-sg", "sgp",
        ),
        "CH" to listOf(
            "switzerland", "zurich", "geneva", "زوریخ", "ژنو", "سوئیس",
            "-ch-", ".ch.", " ch ", "(ch)", "[ch]", "_ch_", "ch-", "-ch", "che",
        ),
        "SE" to listOf(
            "sweden", "stockholm", "استکهلم", "سوئد",
            "-se-", ".se.", " se ", "(se)", "[se]", "_se_", "se-", "-se", "swe",
        ),
        "FI" to listOf(
            "finland", "helsinki", "هلسینکی", "فنلاند",
            "-fi-", ".fi.", " fi ", "(fi)", "[fi]", "_fi_", "fi-", "-fi", "fin",
        ),
        "PL" to listOf(
            "poland", "warsaw", "ورشو", "لهستان",
            "-pl-", ".pl.", " pl ", "(pl)", "[pl]", "_pl_", "pl-", "-pl", "pol",
        ),
        "IR" to listOf(
            "iran", "tehran", "shiraz", "isfahan", "mashhad", "تهران", "شیراز", "اصفهان",
            "مشهد", "ایران", "-ir-", ".ir.", " ir ", "(ir)", "[ir]", "_ir_", "ir-", "-ir",
        ),
        "UA" to listOf(
            "ukraine", "kyiv", "kiev", "کیف", "اوکراین",
            "-ua-", ".ua.", " ua ", "(ua)", "[ua]", "_ua_", "ua-", "-ua", "ukr",
        ),
        "BR" to listOf(
            "brazil", "brasil", "sao paulo", "سائوپائولو", "برزیل",
            "-br-", ".br.", " br ", "(br)", "[br]", "_br_", "br-", "-br", "bra",
        ),
        "AU" to listOf(
            "australia", "sydney", "melbourne", "سیدنی", "ملبورن", "استرالیا",
            "-au-", ".au.", " au ", "(au)", "[au]", "_au_", "au-", "-au", "aus",
        ),
        "AE" to listOf(
            "dubai", "abu dhabi", "دبی", "ابوظبی", "امارات",
            "-ae-", ".ae.", " ae ", "(ae)", "[ae]", "_ae_", "ae-", "-ae", "uae",
        ),
        "HK" to listOf(
            "hong kong", "هنگ کنگ", "-hk-", ".hk.", " hk ", "(hk)", "[hk]", "_hk_", "hk-", "-hk", "hkg",
        ),
        "KR" to listOf(
            "korea", "seoul", "سئول", "کره", "-kr-", ".kr.", " kr ", "(kr)", "[kr]", "_kr_", "kr-", "-kr", "kor",
        ),
        "AT" to listOf(
            "austria", "vienna", "وین", "اتریش", "-at-", ".at.", " at ", "(at)", "[at]", "_at_", "at-", "-at", "aut",
        ),
        "BE" to listOf(
            "belgium", "brussels", "بروکسل", "بلژیک", "-be-", ".be.", " be ", "(be)", "[be]", "_be_", "be-", "-be", "bel",
        ),
        "NO" to listOf(
            "norway", "oslo", "اسلو", "نروژ", "-no-", ".no.", " no ", "(no)", "[no]", "_no_", "no-", "-no", "nor",
        ),
        "DK" to listOf(
            "denmark", "copenhagen", "کپنهاگ", "دانمارک", "-dk-", ".dk.", " dk ", "(dk)", "[dk]", "_dk_", "dk-", "-dk", "dnk",
        ),
        "IE" to listOf(
            "ireland", "dublin", "دوبلین", "ایرلند", "-ie-", ".ie.", " ie ", "(ie)", "[ie]", "_ie_", "ie-", "-ie", "irl",
        ),
        "RO" to listOf(
            "romania", "bucharest", "بخارست", "رومانی", "-ro-", ".ro.", " ro ", "(ro)", "[ro]", "_ro_", "ro-", "-ro", "rou",
        ),
    )
}
