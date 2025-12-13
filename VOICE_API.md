# Voice Assistant API Documentation

The Fulus Pay AI Assistant provides voice interaction capabilities using OpenAI Whisper (speech-to-text) and OpenAI TTS (text-to-speech).

## Overview

The Voice API enables users to:
1. Upload audio files (voice recordings)
2. Get transcription using OpenAI Whisper
3. Receive AI-generated responses
4. Get text-to-speech audio responses

## Base URL

```
http://localhost:8080/api/v1/voice
```

## Endpoints

### 1. Process Voice Request

**Endpoint:** `POST /api/v1/voice`

Uploads audio file, transcribes it, processes with AI, and generates audio response.

**Request Type:** `multipart/form-data`

**Headers:**
- `X-User-Id` (required): User identifier

**Form Parameters:**
- `audioFile` (required): Audio file (WAV, MP3, M4A, MP4, WebM)
- `generateAudio` (optional): Generate audio response (default: `true`)
- `voice` (optional): TTS voice - `alloy`, `echo`, `fable`, `onyx`, `nova`, `shimmer` (default: `nova`)
- `language` (optional): Language code for transcription (default: `en`)

**Response:**
```json
{
  "success": true,
  "message": "Voice request processed successfully",
  "transcribedText": "How much did I spend on food last month?",
  "aiResponse": "Based on your transaction history for last month, you spent ₦45,230 on food...",
  "audioResponseUrl": "/api/v1/voice/audio/response_20241211_143022_a3f8c2d1.mp3",
  "audioFormat": "mp3",
  "audioFileSize": 124567,
  "conversationId": "user-123",
  "timestamp": "2024-12-11T14:30:22",
  "processingTimeMs": 3450
}
```

**Example (cURL):**
```bash
curl -X POST http://localhost:8080/api/v1/voice \
  -H "X-User-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -F "audioFile=@recording.wav" \
  -F "generateAudio=true" \
  -F "voice=nova"
```

**Example (JavaScript/Fetch):**
```javascript
const formData = new FormData();
formData.append('audioFile', audioBlob, 'recording.wav');
formData.append('generateAudio', 'true');
formData.append('voice', 'nova');

const response = await fetch('http://localhost:8080/api/v1/voice', {
  method: 'POST',
  headers: {
    'X-User-Id': '123e4567-e89b-12d3-a456-426614174000'
  },
  body: formData
});

const result = await response.json();
console.log('Transcription:', result.transcribedText);
console.log('AI Response:', result.aiResponse);
console.log('Audio URL:', result.audioResponseUrl);
```

**Example (Python):**
```python
import requests

url = 'http://localhost:8080/api/v1/voice'
headers = {'X-User-Id': '123e4567-e89b-12d3-a456-426614174000'}

with open('recording.wav', 'rb') as audio_file:
    files = {'audioFile': audio_file}
    data = {
        'generateAudio': 'true',
        'voice': 'nova'
    }

    response = requests.post(url, headers=headers, files=files, data=data)
    result = response.json()

    print(f"Transcription: {result['transcribedText']}")
    print(f"AI Response: {result['aiResponse']}")
    print(f"Audio URL: {result['audioResponseUrl']}")
```

---

### 2. Download Audio Response

**Endpoint:** `GET /api/v1/voice/audio/{filename}`

Downloads the generated audio response.

**Path Parameters:**
- `filename`: Audio filename from the response

**Response:** Audio file (MP3)

**Example (cURL):**
```bash
curl -X GET http://localhost:8080/api/v1/voice/audio/response_20241211_143022_a3f8c2d1.mp3 \
  --output response.mp3
```

**Example (JavaScript):**
```javascript
const audioUrl = 'http://localhost:8080' + result.audioResponseUrl;

// Play in browser
const audio = new Audio(audioUrl);
audio.play();

// Or download
const link = document.createElement('a');
link.href = audioUrl;
link.download = 'response.mp3';
link.click();
```

**Example (Python):**
```python
import requests

audio_url = f"http://localhost:8080{result['audioResponseUrl']}"
response = requests.get(audio_url)

with open('response.mp3', 'wb') as f:
    f.write(response.content)
```

---

### 3. Delete Audio File

**Endpoint:** `DELETE /api/v1/voice/audio/{filename}`

Deletes an audio file.

**Headers:**
- `X-User-Id` (required): User identifier

**Path Parameters:**
- `filename`: Audio filename to delete

**Response:**
```json
{
  "success": true,
  "message": "Audio file deleted successfully"
}
```

**Example (cURL):**
```bash
curl -X DELETE http://localhost:8080/api/v1/voice/audio/response_20241211_143022_a3f8c2d1.mp3 \
  -H "X-User-Id: 123e4567-e89b-12d3-a456-426614174000"
```

---

### 4. Health Check

**Endpoint:** `GET /api/v1/voice/health`

Check voice service health.

**Response:** `Voice Assistant Service is running`

---

## Supported Audio Formats

### Input Formats (Upload)
- **MP3** (audio/mp3, audio/mpeg)
- **WAV** (audio/wav, audio/wave)
- **M4A** (audio/m4a)
- **MP4** (audio/mp4)
- **WebM** (audio/webm)

### Output Format
- **MP3** (audio/mpeg)

## File Size Limits

- **Maximum file size:** 10MB (10,485,760 bytes)
- **Maximum request size:** 15MB (including multipart overhead)

## TTS Voice Options

Choose from 6 different voices:
- `alloy` - Neutral and balanced
- `echo` - Male voice
- `fable` - British accent
- `onyx` - Deep male voice
- `nova` - Female voice (default)
- `shimmer` - Female voice with upbeat tone

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Audio file is required",
  "timestamp": "2024-12-11T14:30:22"
}
```

**Common Causes:**
- Missing audio file
- File too large (>10MB)
- Unsupported format
- Missing X-User-Id header

### 404 Not Found
```json
{
  "success": false,
  "message": "Audio file not found: response_xyz.mp3",
  "timestamp": "2024-12-11T14:30:22"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "An error occurred processing your voice request. Please try again.",
  "timestamp": "2024-12-11T14:30:22"
}
```

**Common Causes:**
- OpenAI API errors
- Transcription failed
- TTS generation failed

## Complete Example: React Voice Recorder

```jsx
import React, { useState, useRef } from 'react';

function VoiceAssistant() {
  const [isRecording, setIsRecording] = useState(false);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const mediaRecorder = useRef(null);
  const audioChunks = useRef([]);

  const startRecording = async () => {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaRecorder.current = new MediaRecorder(stream);
    audioChunks.current = [];

    mediaRecorder.current.ondataavailable = (event) => {
      audioChunks.current.push(event.data);
    };

    mediaRecorder.current.onstop = async () => {
      const audioBlob = new Blob(audioChunks.current, { type: 'audio/wav' });
      await processVoice(audioBlob);
    };

    mediaRecorder.current.start();
    setIsRecording(true);
  };

  const stopRecording = () => {
    mediaRecorder.current.stop();
    mediaRecorder.current.stream.getTracks().forEach(track => track.stop());
    setIsRecording(false);
  };

  const processVoice = async (audioBlob) => {
    setLoading(true);

    const formData = new FormData();
    formData.append('audioFile', audioBlob, 'recording.wav');
    formData.append('generateAudio', 'true');
    formData.append('voice', 'nova');

    try {
      const response = await fetch('http://localhost:8080/api/v1/voice', {
        method: 'POST',
        headers: {
          'X-User-Id': '123e4567-e89b-12d3-a456-426614174000'
        },
        body: formData
      });

      const data = await response.json();
      setResult(data);

      // Play audio response
      if (data.audioResponseUrl) {
        const audio = new Audio('http://localhost:8080' + data.audioResponseUrl);
        audio.play();
      }
    } catch (error) {
      console.error('Error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Voice Assistant</h2>

      <button
        onClick={isRecording ? stopRecording : startRecording}
        disabled={loading}
      >
        {isRecording ? 'Stop Recording' : 'Start Recording'}
      </button>

      {loading && <p>Processing...</p>}

      {result && (
        <div>
          <h3>Transcription:</h3>
          <p>{result.transcribedText}</p>

          <h3>AI Response:</h3>
          <p>{result.aiResponse}</p>

          <p>Processing time: {result.processingTimeMs}ms</p>
        </div>
      )}
    </div>
  );
}

export default VoiceAssistant;
```

## Complete Example: Python Voice Client

```python
import requests
import pyaudio
import wave
import time

class VoiceAssistant:
    def __init__(self, base_url, user_id):
        self.base_url = base_url
        self.user_id = user_id
        self.headers = {'X-User-Id': user_id}

    def record_audio(self, filename='recording.wav', duration=5):
        """Record audio from microphone"""
        CHUNK = 1024
        FORMAT = pyaudio.paInt16
        CHANNELS = 1
        RATE = 16000

        p = pyaudio.PyAudio()
        stream = p.open(format=FORMAT, channels=CHANNELS,
                       rate=RATE, input=True,
                       frames_per_buffer=CHUNK)

        print(f"Recording for {duration} seconds...")
        frames = []

        for _ in range(0, int(RATE / CHUNK * duration)):
            data = stream.read(CHUNK)
            frames.append(data)

        print("Recording finished")
        stream.stop_stream()
        stream.close()
        p.terminate()

        # Save to file
        wf = wave.open(filename, 'wb')
        wf.setnchannels(CHANNELS)
        wf.setsampwidth(p.get_sample_size(FORMAT))
        wf.setframerate(RATE)
        wf.writeframes(b''.join(frames))
        wf.close()

        return filename

    def process_voice(self, audio_file, voice='nova', generate_audio=True):
        """Send audio to voice assistant"""
        url = f"{self.base_url}/api/v1/voice"

        with open(audio_file, 'rb') as f:
            files = {'audioFile': f}
            data = {
                'generateAudio': str(generate_audio).lower(),
                'voice': voice
            }

            response = requests.post(url, headers=self.headers,
                                    files=files, data=data)
            return response.json()

    def download_audio(self, audio_url, output_file='response.mp3'):
        """Download audio response"""
        full_url = f"{self.base_url}{audio_url}"
        response = requests.get(full_url)

        with open(output_file, 'wb') as f:
            f.write(response.content)

        return output_file

    def delete_audio(self, filename):
        """Delete audio file"""
        url = f"{self.base_url}/api/v1/voice/audio/{filename}"
        response = requests.delete(url, headers=self.headers)
        return response.json()

# Usage example
if __name__ == '__main__':
    assistant = VoiceAssistant(
        base_url='http://localhost:8080',
        user_id='123e4567-e89b-12d3-a456-426614174000'
    )

    # Record audio
    audio_file = assistant.record_audio(duration=5)

    # Process voice
    result = assistant.process_voice(audio_file, voice='nova')

    print(f"Transcription: {result['transcribedText']}")
    print(f"AI Response: {result['aiResponse']}")
    print(f"Processing time: {result['processingTimeMs']}ms")

    # Download and play response
    if result.get('audioResponseUrl'):
        response_file = assistant.download_audio(result['audioResponseUrl'])
        print(f"Audio saved to: {response_file}")

        # Play audio (requires pygame or similar)
        # import pygame
        # pygame.mixer.init()
        # pygame.mixer.music.load(response_file)
        # pygame.mixer.music.play()
```

## Configuration

### Environment Variables

```bash
# Voice upload directory
VOICE_UPLOAD_DIR=/tmp/fulus-voice

# Default TTS voice
VOICE_TTS_VOICE=nova
```

### application.yml

```yaml
voice:
  upload:
    directory: ${VOICE_UPLOAD_DIR:${java.io.tmpdir}/fulus-voice}
  max-file-size: 10485760  # 10MB
  tts:
    voice: ${VOICE_TTS_VOICE:nova}
  cleanup:
    enabled: true
    age-hours: 24  # Delete files older than 24 hours

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 15MB
```

## File Cleanup

The service automatically cleans up old audio files:
- **Cleanup frequency:** Every 6 hours
- **Retention period:** 24 hours (configurable)
- **Cleanup on startup:** Initial cleanup 1 hour after startup

Files older than the configured retention period are automatically deleted.

## Performance Considerations

### Processing Time
Typical processing times:
- **Transcription (Whisper):** 1-3 seconds
- **AI Processing:** 2-5 seconds
- **TTS Generation:** 1-3 seconds
- **Total:** 4-11 seconds

### Optimization Tips
1. Use compressed audio formats (MP3 preferred over WAV)
2. Keep recordings under 60 seconds for faster processing
3. Set `generateAudio=false` if text response is sufficient
4. Consider caching common responses

## Security Considerations

1. **User Authentication:** Always include valid `X-User-Id` header
2. **File Validation:** Server validates file size and format
3. **Path Traversal Protection:** Filenames are sanitized to prevent directory traversal
4. **Temporary Storage:** Files are stored in isolated temporary directory
5. **Automatic Cleanup:** Old files are automatically deleted

## Integration with AI Chat

Voice requests are processed through the same AI assistant as text chat:
- **Conversation memory:** Maintained across voice and text interactions
- **Function calling:** All 6 function tools available (transactions, budgets, transfers, etc.)
- **Context awareness:** Voice requests contribute to conversation history
- **User profile:** Same user balance, name, and preferences

## Troubleshooting

### "Audio file is required"
- Ensure `audioFile` parameter is included in form data
- Verify file is not empty

### "File size exceeds maximum"
- Maximum file size is 10MB
- Compress audio or reduce recording duration

### "Unsupported audio format"
- Use supported formats: MP3, WAV, M4A, MP4, WebM
- Check file extension and content type

### "Failed to transcribe audio"
- Ensure audio quality is sufficient
- Speak clearly and reduce background noise
- Check audio file is not corrupted

### "Audio file not found"
- File may have been cleaned up (older than 24 hours)
- Verify filename in URL

## API Rate Limits

OpenAI API rate limits apply:
- **Whisper API:** 50 requests/minute
- **TTS API:** 50 requests/minute
- **GPT-4 Turbo:** Based on your OpenAI plan

Monitor usage in OpenAI dashboard: https://platform.openai.com/usage

## Related Documentation

- [AI Chat API](./AI_CHAT_API.md) - Text-based chat API
- [Spring AI Functions](./SPRING_AI_FUNCTIONS.md) - Available AI function tools
- [System Prompt Guide](./AI_SYSTEM_PROMPT_GUIDE.md) - AI personality and behavior
- [README](./README.md) - Main documentation

---

**Generated for Fulus Pay AI Assistant - Voice API v1.0**
