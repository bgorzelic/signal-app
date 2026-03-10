package dev.aiaerial.signal.data.syslog

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SyslogReceiver(
    private val port: Int = 1514,
) {
    private val _messages = MutableSharedFlow<SyslogMessage>(extraBufferCapacity = 256)
    val messages: SharedFlow<SyslogMessage> = _messages.asSharedFlow()

    private var job: Job? = null

    suspend fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return // prevent double-start
        job = scope.launch(Dispatchers.IO) {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = try {
                aSocket(selectorManager).udp().bind(InetSocketAddress("0.0.0.0", port))
            } catch (e: Exception) {
                selectorManager.close()
                throw e
            }
            try {
                while (isActive) {
                    val datagram = socket.receive()
                    val raw = datagram.packet.readText()
                    val message = SyslogMessage.parse(raw.trim())
                    _messages.emit(message)
                }
            } finally {
                socket.close()
                selectorManager.close()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
