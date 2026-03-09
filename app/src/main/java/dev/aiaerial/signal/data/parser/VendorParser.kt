package dev.aiaerial.signal.data.parser

import dev.aiaerial.signal.data.model.NetworkEvent

interface VendorParser {
    fun canParse(line: String): Boolean
    fun parse(line: String, sessionId: String): NetworkEvent?
}
