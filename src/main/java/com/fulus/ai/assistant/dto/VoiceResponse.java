package com.fulus.ai.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for voice assistant interactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceResponse {

    /**
     * Whether the request was successful
     */
    private boolean success;

    /**
     * Status message
     */
    private String message;

    /**
     * Transcribed text from user's audio
     */
    private String transcribedText;

    /**
     * AI assistant's text response
     */
    private String aiResponse;

    /**
     * URL or path to the audio response file
     */
    private String audioResponseUrl;

    /**
     * Audio format (mp3, wav)
     */
    private String audioFormat;

    /**
     * Audio file size in bytes
     */
    private Long audioFileSize;

    /**
     * Conversation ID
     */
    private String conversationId;

    /**
     * Timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Processing time in milliseconds
     */
    private Long processingTimeMs;

    /**
     * Factory method for success response
     */
    public static VoiceResponse success(
            String transcribedText,
            String aiResponse,
            String audioResponseUrl,
            String conversationId,
            long processingTimeMs) {
        return VoiceResponse.builder()
                .success(true)
                .message("Voice request processed successfully")
                .transcribedText(transcribedText)
                .aiResponse(aiResponse)
                .audioResponseUrl(audioResponseUrl)
                .conversationId(conversationId)
                .processingTimeMs(processingTimeMs)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method for error response
     */
    public static VoiceResponse error(String errorMessage) {
        return VoiceResponse.builder()
                .success(false)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
