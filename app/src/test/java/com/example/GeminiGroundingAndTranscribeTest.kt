package com.example

import com.example.ai.GeminiHealthService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiGroundingAndTranscribeTest {

    @Test
    fun testAudioTranscriptionWithGemini35Transcribe() = runBlocking {
        val sampleAudioBytes = "RIFF....WAVEfmt ....data....".toByteArray()
        val transcription = GeminiHealthService.transcribeAudio(sampleAudioBytes, "audio/wav")

        assertNotNull(transcription)
        assertTrue("Transcription should not be empty", transcription.isNotBlank())
    }

    @Test
    fun testMultiTurnChatConversationHistoryAndRoles() = runBlocking {
        val chatHistory = listOf(
            true to "Hello doctor, I have a dry cough and throat irritation.",
            false to "Hello, I am Dr. Lifscan. Have you had fever or difficulty breathing?",
            true to "No fever, but symptoms started 2 days ago."
        )

        // 1. Test fast triage model (gemini-3.1-flash-lite-preview)
        val fastResponse = GeminiHealthService.multiTurnChat(
            chatHistory = chatHistory,
            selectedModel = GeminiHealthService.GeminiChatModel.FAST_TRIAGE,
            systemInstructionText = "You are a rapid medical triage AI."
        )
        assertNotNull(fastResponse)
        assertTrue(fastResponse.isNotBlank())

        // 2. Test general health model (gemini-3.5-flash)
        val generalResponse = GeminiHealthService.multiTurnChat(
            chatHistory = chatHistory,
            selectedModel = GeminiHealthService.GeminiChatModel.GENERAL_HEALTH,
            systemInstructionText = "You are an empathetic clinical medical assistant."
        )
        assertNotNull(generalResponse)
        assertTrue(generalResponse.isNotBlank())

        // 3. Test complex reasoning model (gemini-3.1-pro-preview)
        val complexResponse = GeminiHealthService.multiTurnChat(
            chatHistory = chatHistory,
            selectedModel = GeminiHealthService.GeminiChatModel.COMPLEX_REASONING,
            systemInstructionText = "You are a clinical differential diagnostic consultant."
        )
        assertNotNull(complexResponse)
        assertTrue(complexResponse.isNotBlank())
    }

    @Test
    fun testGoogleMapsDataGrounding() = runBlocking {
        val mapsGroundedResponse = GeminiHealthService.queryFacilitiesWithGoogleMapsGrounding(
            searchQuery = "24/7 Trauma Centers and Emergency Hospitals",
            userLatitude = 27.7058,
            userLongitude = 85.3142
        )

        assertNotNull(mapsGroundedResponse)
        assertTrue(mapsGroundedResponse.isNotBlank())
        assertTrue("Should contain hospital information", mapsGroundedResponse.contains("Hospital") || mapsGroundedResponse.contains("Medical") || mapsGroundedResponse.contains("Bir"))
    }

    @Test
    fun testGoogleSearchDataGrounding() = runBlocking {
        val searchGroundedResponse = GeminiHealthService.queryMedicalResearchWithGoogleSearch(
            searchQuery = "Current clinical guidelines for acute myocardial infarction treatment"
        )

        assertNotNull(searchGroundedResponse)
        assertTrue(searchGroundedResponse.isNotBlank())
        assertTrue("Should contain clinical brief", searchGroundedResponse.contains("Clinical") || searchGroundedResponse.contains("Medical") || searchGroundedResponse.contains("Google Search"))
    }
}
