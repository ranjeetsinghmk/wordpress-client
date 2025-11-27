package com.sikhsiyasat.wordpress.ui.detail

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

/**
 * Helper class for sentence-by-sentence Text-to-Speech reading.
 * Splits text into sentences and reads them sequentially, allowing
 * for better progress tracking and user experience.
 */
class SentenceTextToSpeech(
    private val textToSpeech: TextToSpeech,
    private val listener: SentenceProgressListener? = null
) {
    private var sentences: List<String> = emptyList()
    private var currentSentenceIndex: Int = 0
    private var isPlaying: Boolean = false

    init {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                listener?.onSentenceStarted(currentSentenceIndex, sentences.getOrNull(currentSentenceIndex) ?: "")
            }

            override fun onDone(utteranceId: String?) {
                if (isPlaying && currentSentenceIndex < sentences.size - 1) {
                    currentSentenceIndex++
                    speakCurrentSentence()
                } else {
                    isPlaying = false
                    listener?.onComplete()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isPlaying = false
                listener?.onError(utteranceId)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isPlaying = false
                listener?.onError(utteranceId)
            }
        })
    }

    /**
     * Starts reading the provided text sentence by sentence.
     * @param text The full text to be read
     * @param locale The locale to use for speech (default: Locale.US)
     */
    fun speak(text: String, locale: Locale = Locale.US) {
        stop()
        textToSpeech.language = locale
        sentences = splitIntoSentences(text)
        currentSentenceIndex = 0
        isPlaying = true
        
        if (sentences.isNotEmpty()) {
            listener?.onStarted(sentences.size)
            speakCurrentSentence()
        }
    }

    /**
     * Stops the current speech playback.
     */
    fun stop() {
        isPlaying = false
        textToSpeech.stop()
        listener?.onStopped()
    }

    /**
     * Returns whether speech is currently playing.
     */
    fun isPlaying(): Boolean = isPlaying

    /**
     * Returns the current sentence index being read.
     */
    fun getCurrentSentenceIndex(): Int = currentSentenceIndex

    /**
     * Returns the total number of sentences.
     */
    fun getTotalSentences(): Int = sentences.size

    private fun speakCurrentSentence() {
        val sentence = sentences.getOrNull(currentSentenceIndex) ?: return
        val utteranceId = "sentence_$currentSentenceIndex"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId)
            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    companion object {
        /**
         * Splits text into sentences using common sentence delimiters.
         * Handles periods, exclamation marks, question marks, and newlines.
         * 
         * @param text The text to split into sentences
         * @return A list of sentences (trimmed, non-empty)
         */
        fun splitIntoSentences(text: String): List<String> {
            if (text.isBlank()) return emptyList()
            
            // Split on sentence-ending punctuation followed by whitespace
            // or on newlines (which act as natural sentence breaks in article content)
            val sentencePattern = Regex("""(?<=[.!?])\s+|\n+""")
            
            return text.split(sentencePattern)
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
    }

    /**
     * Listener interface for sentence-by-sentence progress callbacks.
     */
    interface SentenceProgressListener {
        /** Called when speech starts with the total number of sentences. */
        fun onStarted(totalSentences: Int)
        
        /** Called when a sentence begins being read. */
        fun onSentenceStarted(index: Int, sentence: String)
        
        /** Called when all sentences have been read. */
        fun onComplete()
        
        /** Called when speech is stopped manually. */
        fun onStopped()
        
        /** Called when an error occurs during speech. */
        fun onError(utteranceId: String?)
    }
}
