package com.example.sip

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class DtmfTonePlayer {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 75)
        } catch (e: Exception) {
            Log.w("DtmfTonePlayer", "ToneGenerator unavailable: ${e.message}")
        }
    }

    fun playDigit(digit: Char) {
        val toneType = when (digit) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        try {
            toneGenerator?.startTone(toneType, 120)
        } catch (e: Exception) {
            Log.w("DtmfTonePlayer", "Failed to play tone: ${e.message}")
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Exception) {
        }
        toneGenerator = null
    }
}
