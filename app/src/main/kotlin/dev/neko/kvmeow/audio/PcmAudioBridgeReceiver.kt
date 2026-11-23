package dev.neko.kvmeow.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * 🎧 Low-Latency PCM Audio Bridge Receiver (LAN / USB Forward) 🎧
 *
 * Receives raw 16-bit PCM stereo/mono audio from PC/Mac over:
 * 1. USB Reverse/Forward Tunnel (e.g. adb forward tcp:28200 tcp:28200 / scrcpy audio)
 * 2. Low-latency local LAN stream (SonoBus / AudioRelay style PCM raw stream)
 *
 * Plays audio out to connected Bluetooth earbuds (e.g. Soundcore C30i) with minimal buffer latency,
 * and splits a copy of raw PCM samples directly to MeetingAudioRecorder for synchronized dual-track capture.
 */
class PcmAudioBridgeReceiver(
    private val context: Context,
    private val port: Int = 28200,
    private val sampleRate: Int = 48000,
    private val channelConfig: Int = AudioFormat.CHANNEL_OUT_STEREO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var audioTrack: AudioTrack? = null
    private var isStreaming = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var listenerJob: Job? = null

    // Callback to share incoming audio stream with recorder
    var onPcmChunkReceived: ((ByteArray, Int) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean, String) -> Unit)? = null

    fun startServer() {
        if (isStreaming) return
        isStreaming = true

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormat)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        audioTrack?.play()

        listenerJob = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply { reuseAddress = true }
                Log.d("PcmAudioBridge", "Listening for PC/Mac audio bridge on port $port...")
                onConnectionStateChanged?.invoke(false, "Waiting for host on port $port")

                while (isActive && isStreaming) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        clientSocket = socket
                        val remoteAddress = socket.remoteSocketAddress.toString()
                        Log.d("PcmAudioBridge", "Audio stream connected from: $remoteAddress")
                        onConnectionStateChanged?.invoke(true, "Connected: $remoteAddress")

                        handleStream(socket.getInputStream(), bufferSize)
                    } catch (e: Exception) {
                        if (isStreaming) {
                            Log.w("PcmAudioBridge", "Client disconnected: ${e.message}")
                            onConnectionStateChanged?.invoke(false, "Disconnected: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PcmAudioBridge", "Server socket error on port $port", e)
                onConnectionStateChanged?.invoke(false, "Error: ${e.message}")
            }
        }
    }

    private fun handleStream(inputStream: InputStream, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        while (isStreaming && clientSocket?.isConnected == true) {
            val bytesRead = inputStream.read(buffer, 0, buffer.size)
            if (bytesRead <= 0) break

            // 1. Play out to connected Bluetooth earbuds (Soundcore C30i)
            audioTrack?.write(buffer, 0, bytesRead)

            // 2. Relay chunk to meeting recorder if active
            onPcmChunkReceived?.invoke(buffer.copyOf(bytesRead), bytesRead)
        }
    }

    fun stop() {
        isStreaming = false
        listenerJob?.cancel()
        listenerJob = null

        try {
            clientSocket?.close()
        } catch (_: Exception) {}
        clientSocket = null

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        onConnectionStateChanged?.invoke(false, "Stopped")
        Log.d("PcmAudioBridge", "Audio Bridge Server stopped")
    }

    fun isConnected(): Boolean = clientSocket?.isConnected == true && !clientSocket!!.isClosed
    fun isRunning(): Boolean = isStreaming
}
