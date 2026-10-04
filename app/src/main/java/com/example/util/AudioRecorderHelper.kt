package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderHelper(private val context: Context) {
    companion object {
        private const val TAG = "AudioRecorderHelper"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false

    fun startRecording(): Boolean {
        return try {
            val cacheDir = context.cacheDir
            currentAudioFile = File(cacheDir, "user_audio_${System.currentTimeMillis()}.mp4")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(currentAudioFile?.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            isRecording = false
            false
        }
    }

    fun stopRecording(): ByteArray? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Stop exception (short recording): ${e.message}")
                }
                release()
            }
            mediaRecorder = null
            isRecording = false

            val file = currentAudioFile
            if (file != null && file.exists() && file.length() > 0) {
                file.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording: ${e.message}", e)
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
