package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.NetworkEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VendorDetector @Inject constructor() {

    private val parsers: List<VendorParser> = listOf(
        CiscoWlcParser(),
        ArubaParser(),
        MerakiParser(),
        RuckusParser(),
        JuniperMistParser(),
        UniFiParser(),
    )

    fun parse(line: String, sessionId: String): NetworkEvent? {
        for (parser in parsers) {
            if (parser.canParse(line)) {
                return parser.parse(line, sessionId)
            }
        }
        return null
    }
}
