package com.fulus.ai.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for voice assistant interactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceRequest {

    /**
     * User ID for the voice request
     */
    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * Audio file (WAV or MP3)
     */
    private MultipartFile audioFile;

    /**
     * Optional: Language code for transcription (default: en)
     */
    private String language;

    /**
     * Optional: Generate audio response (default: true)
     */
    @Builder.Default
    private Boolean generateAudio = true;

    /**
     * Optional: Voice for TTS (alloy, echo, fable, onyx, nova, shimmer)
     */
    private String voice;
}
