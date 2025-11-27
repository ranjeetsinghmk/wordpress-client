package com.sikhsiyasat.wordpress.detail

import com.sikhsiyasat.wordpress.ui.detail.SentenceTextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceTextToSpeechTest {

    @Test
    fun splitIntoSentences_emptyString_returnsEmptyList() {
        val result = SentenceTextToSpeech.splitIntoSentences("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun splitIntoSentences_blankString_returnsEmptyList() {
        val result = SentenceTextToSpeech.splitIntoSentences("   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun splitIntoSentences_singleSentence_returnsSingleElement() {
        val result = SentenceTextToSpeech.splitIntoSentences("Hello World.")
        assertEquals(1, result.size)
        assertEquals("Hello World.", result[0])
    }

    @Test
    fun splitIntoSentences_multipleSentencesWithPeriods_splitsCorrectly() {
        val text = "First sentence. Second sentence. Third sentence."
        val result = SentenceTextToSpeech.splitIntoSentences(text)
        assertEquals(3, result.size)
        assertEquals("First sentence.", result[0])
        assertEquals("Second sentence.", result[1])
        assertEquals("Third sentence.", result[2])
    }

    @Test
    fun splitIntoSentences_sentencesWithExclamationMarks_splitsCorrectly() {
        val text = "Hello! How are you? I am fine."
        val result = SentenceTextToSpeech.splitIntoSentences(text)
        assertEquals(3, result.size)
        assertEquals("Hello!", result[0])
        assertEquals("How are you?", result[1])
        assertEquals("I am fine.", result[2])
    }

    @Test
    fun splitIntoSentences_sentencesWithNewlines_splitsCorrectly() {
        val text = "First line.\n Second line.\n Third line."
        val result = SentenceTextToSpeech.splitIntoSentences(text)
        assertEquals(3, result.size)
        assertEquals("First line.", result[0])
        assertEquals("Second line.", result[1])
        assertEquals("Third line.", result[2])
    }

    @Test
    fun splitIntoSentences_mixedPunctuation_splitsCorrectly() {
        val text = "Is this working? Yes! It works perfectly."
        val result = SentenceTextToSpeech.splitIntoSentences(text)
        assertEquals(3, result.size)
        assertEquals("Is this working?", result[0])
        assertEquals("Yes!", result[1])
        assertEquals("It works perfectly.", result[2])
    }

    @Test
    fun splitIntoSentences_realWorldContent_splitsCorrectly() {
        val text = "Article Title.\n published by Author on January 1, 2024.\n This is the first paragraph. It has multiple sentences. Here is another paragraph!"
        val result = SentenceTextToSpeech.splitIntoSentences(text)
        assertTrue(result.size >= 4)
        assertEquals("Article Title.", result[0])
    }

    @Test
    fun splitIntoSentences_trimWhitespace_returnsCleanSentences() {
        val text = "  First.   Second.   Third.  "
        val result = SentenceTextToSpeech.splitIntoSentences(text)
        assertEquals(3, result.size)
        assertEquals("First.", result[0])
        assertEquals("Second.", result[1])
        assertEquals("Third.", result[2])
    }
}
