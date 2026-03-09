package dev.aiaerial.signal.data.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VendorDetector @Inject constructor() {

    private val parsers: List<VendorParser> = listOf(
        CiscoWlcParser(),
        // Future: ArubaParser(), MerakiParser(), etc.
    )

    fun parse(line: String, sessionId: String): dev.aiaerial.signal.data.model.NetworkEvent? {
        for (parser in parsers) {
            if (parser.canParse(line)) {
                return parser.parse(line, sessionId)
            }
        }
        return null
    }
}
