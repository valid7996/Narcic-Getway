package dev.valid7996.narcicgeteway.data.config

/**
 * Obfuscated vault storing default subscription endpoints.
 * Keeps URLs encrypted in the source code and bytecodes to prevent
 * scraping or reuse by third parties.
 */
object DefaultSubscriptionVault {
    private val MASK: ByteArray = byteArrayOf(
        0x5E.toByte(), 0x7A.toByte(), 0x3F.toByte(), 0x91.toByte(),
        0x2B.toByte(), 0x8C.toByte(), 0x4D.toByte(), 0x1E.toByte(),
        0xF3.toByte(), 0x6A.toByte(), 0xA5.toByte(), 0x7B.toByte(),
        0x19.toByte(), 0xC4.toByte(), 0x82.toByte(), 0x5D.toByte()
    )

    private fun decode(encrypted: ByteArray): String {
        val out = ByteArray(encrypted.size)
        for (i in encrypted.indices) {
            val keyByte = MASK[i % MASK.size].toInt() xor (i * 37 and 0xFF)
            out[i] = (encrypted[i].toInt() xor keyByte).toByte()
        }
        return out.decodeToString()
    }

    private val ENC_IRANCELL = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-93).toByte(), (-1).toByte(), 115.toByte(), (-68).toByte(), 87.toByte(), (-33).toByte(),
        77.toByte(), (-80).toByte(), (-114).toByte(), 117.toByte(), 115.toByte(), 106.toByte(), 106.toByte(), (-9).toByte(),
        (-36).toByte(), 12.toByte(), 108.toByte(), (-83).toByte(), (-16).toByte(), 100.toByte(), (-45).toByte(), (-90).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-110).toByte(), 20.toByte(), 58.toByte(), 51.toByte(),
        (-76).toByte(), (-111).toByte(), 123.toByte(), (-55).toByte(), (-106).toByte(), (-106).toByte(), (-83).toByte(), 68.toByte(),
        (-81).toByte(), (-95).toByte(), (-122).toByte(), 11.toByte(), 99.toByte(), (-87).toByte(), 13.toByte(), (-7).toByte(),
        51.toByte(), (-61).toByte()
    )

    private val ENC_MCI = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-93).toByte(), (-1).toByte(), 115.toByte(), (-68).toByte(), 87.toByte(), (-33).toByte(),
        77.toByte(), (-80).toByte(), (-114).toByte(), 117.toByte(), 115.toByte(), 106.toByte(), 106.toByte(), (-9).toByte(),
        (-36).toByte(), 12.toByte(), 108.toByte(), (-83).toByte(), (-16).toByte(), 100.toByte(), (-45).toByte(), (-90).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-110).toByte(), 20.toByte(), 58.toByte(), 51.toByte(),
        (-76).toByte(), (-111).toByte(), 123.toByte(), (-55).toByte(), (-106).toByte(), (-106).toByte(), (-87).toByte(), 85.toByte(),
        (-89).toByte(), (-31).toByte(), (-111).toByte(), 22.toByte(), 123.toByte()
    )

    private val ENC_WIREGUARD = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-93).toByte(), (-1).toByte(), 115.toByte(), (-68).toByte(), 87.toByte(), (-33).toByte(),
        77.toByte(), (-80).toByte(), (-114).toByte(), 117.toByte(), 115.toByte(), 106.toByte(), 106.toByte(), (-9).toByte(),
        (-36).toByte(), 12.toByte(), 108.toByte(), (-83).toByte(), (-16).toByte(), 100.toByte(), (-45).toByte(), (-90).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-110).toByte(), 20.toByte(), 58.toByte(), 51.toByte(),
        (-76).toByte(), (-119).toByte(), 112.toByte(), (-37).toByte(), (-116).toByte(), (-105).toByte(), (-93).toByte(), 67.toByte(),
        (-81).toByte(), (-67).toByte(), (-127).toByte(), 64.toByte(), 123.toByte(), (-67).toByte(), 87.toByte()
    )

    private val ENC_WINDSCRIBE_WG = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-93).toByte(), (-1).toByte(), 115.toByte(), (-68).toByte(), 87.toByte(), (-33).toByte(),
        77.toByte(), (-80).toByte(), (-114).toByte(), 117.toByte(), 115.toByte(), 106.toByte(), 106.toByte(), (-9).toByte(),
        (-36).toByte(), 12.toByte(), 108.toByte(), (-83).toByte(), (-16).toByte(), 100.toByte(), (-45).toByte(), (-90).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-110).toByte(), 20.toByte(), 58.toByte(), 51.toByte(),
        (-76).toByte(), (-112).toByte(), 126.toByte(), (-62).toByte(), (-127).toByte(), (-106).toByte(), (-89).toByte(), 68.toByte(),
        (-89).toByte(), (-83).toByte(), (-128).toByte(), 25.toByte(), 104.toByte(), (-21).toByte(), 87.toByte(), (-11).toByte(),
        63.toByte()
    )

    private val ENC_WINDSCRIBE_OPENVPN = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-93).toByte(), (-1).toByte(), 115.toByte(), (-68).toByte(), 87.toByte(), (-33).toByte(),
        77.toByte(), (-80).toByte(), (-114).toByte(), 117.toByte(), 115.toByte(), 106.toByte(), 106.toByte(), (-9).toByte(),
        (-36).toByte(), 12.toByte(), 108.toByte(), (-83).toByte(), (-16).toByte(), 100.toByte(), (-45).toByte(), (-90).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-110).toByte(), 20.toByte(), 58.toByte(), 51.toByte(),
        (-76).toByte(), (-112).toByte(), 126.toByte(), (-62).toByte(), (-127).toByte(), (-106).toByte(), (-89).toByte(), 68.toByte(),
        (-89).toByte(), (-83).toByte(), (-128).toByte(), 49.toByte(), 96.toByte(), (-75).toByte(), 70.toByte(), (-29).toByte(),
        61.toByte(), (-57).toByte(), (-55).toByte(), 114.toByte(), 63.toByte(), (-58).toByte(), 123.toByte(), (-120).toByte()
    )

    private val ENC_WINDSCRIBE_IKEV2 = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-93).toByte(), (-1).toByte(), 115.toByte(), (-68).toByte(), 87.toByte(), (-33).toByte(),
        77.toByte(), (-80).toByte(), (-114).toByte(), 117.toByte(), 115.toByte(), 106.toByte(), 106.toByte(), (-9).toByte(),
        (-36).toByte(), 12.toByte(), 108.toByte(), (-83).toByte(), (-16).toByte(), 100.toByte(), (-45).toByte(), (-90).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-110).toByte(), 20.toByte(), 58.toByte(), 51.toByte(),
        (-76).toByte(), (-112).toByte(), 126.toByte(), (-62).toByte(), (-127).toByte(), (-106).toByte(), (-89).toByte(), 68.toByte(),
        (-89).toByte(), (-83).toByte(), (-128).toByte(), 49.toByte(), 102.toByte(), (-82).toByte(), 70.toByte(), (-5).toByte(),
        121.toByte(), (-103).toByte(), (-45).toByte(), 36.toByte(), 33.toByte()
    )

    private val ENC_SPOOF = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-93).toByte(), (-1).toByte(), 115.toByte(), (-68).toByte(), 87.toByte(), (-33).toByte(),
        77.toByte(), (-80).toByte(), (-114).toByte(), 117.toByte(), 115.toByte(), 106.toByte(), 106.toByte(), (-9).toByte(),
        (-36).toByte(), 12.toByte(), 108.toByte(), (-83).toByte(), (-16).toByte(), 100.toByte(), (-45).toByte(), (-90).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-110).toByte(), 20.toByte(), 58.toByte(), 51.toByte(),
        (-76).toByte(), (-87).toByte(), 118.toByte(), (-34).toByte(), (-122).toByte(), (-116).toByte(), (-89).toByte(), 27.toByte(),
        (-99).toByte(), (-65).toByte(), (-118).toByte(), 1.toByte(), 96.toByte(), (-93).toByte(), 13.toByte(), (-7).toByte(),
        51.toByte(), (-61).toByte()
    )

    private val ENC_HAMEDVPNS = byteArrayOf(
        54.toByte(), 43.toByte(), 1.toByte(), (-114).toByte(), (-52).toByte(), 15.toByte(), (-68).toByte(), 50.toByte(),
        (-87).toByte(), 70.toByte(), (-96).toByte(), (-62).toByte(), (-62).toByte(), 76.toByte(), (-16).toByte(), 30.toByte(),
        123.toByte(), 109.toByte(), (-48).toByte(), 93.toByte(), (-86).toByte(), (-9).toByte(), 0.toByte(), 34.toByte(),
        (-27).toByte(), (-125).toByte(), 2.toByte(), (-14).toByte(), 97.toByte(), (-37).toByte(), (-73).toByte(), 73.toByte(),
        (-109).toByte(), (-112).toByte(), (-67).toByte(), (-1).toByte(), 114.toByte(), (-80).toByte(), 87.toByte(), (-51).toByte(),
        22.toByte(), (-80).toByte(), (-122).toByte(), 99.toByte(), 11.toByte(), 26.toByte(), 87.toByte(), (-29).toByte(),
        (-52).toByte(), 48.toByte(), 102.toByte(), (-90).toByte(), (-54).toByte(), 78.toByte(), (-26).toByte(), (-97).toByte(),
        (-60).toByte(), 37.toByte(), (-94).toByte(), (-102).toByte(), (-58).toByte(), 58.toByte(), 28.toByte(), 35.toByte(),
        127.toByte(), 123.toByte(), (-58).toByte(), 17.toByte(), (-113).toByte(), 20.toByte(), 39.toByte(), 62.toByte(),
        (-13).toByte(), (-54).toByte(), 38.toByte(), (-125).toByte(), (-125).toByte(), (-116).toByte(), (-86).toByte(), 87.toByte(),
        (-94).toByte(), (-31).toByte(), (-111).toByte(), 22.toByte(), 123.toByte()
    )

    val irancellUrl: String by lazy { decode(ENC_IRANCELL) }
    val mciUrl: String by lazy { decode(ENC_MCI) }
    val wireguardUrl: String by lazy { decode(ENC_WIREGUARD) }
    val windscribeWgUrl: String by lazy { decode(ENC_WINDSCRIBE_WG) }
    val windscribeOpenvpnUrl: String by lazy { decode(ENC_WINDSCRIBE_OPENVPN) }
    val windscribeIkev2Url: String by lazy { decode(ENC_WINDSCRIBE_IKEV2) }
    val spoofUrl: String by lazy { decode(ENC_SPOOF) }
    val hamedvpnsUrl: String by lazy { decode(ENC_HAMEDVPNS) }
}
