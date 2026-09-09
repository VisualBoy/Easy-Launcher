System instructions let you give the model a persona, set the tone of its responses, or provide specific rules it must follow. These instructions are separate from the user's prompt and are treated with higher priority by the model to ensure it behaves as expected.

Common use cases include:
- **Setting a persona:** For example, "You are a helpful math tutor."
- **Enforcing output format:** For example, "Always respond in bullet points."
- **Setting constraints:** For example, "Do not answer questions about politics."

## Prerequisites

System instructions work on devices running Gemini Nano V3 and higher.

## Limitations

We don't recommend using system instructions with prefix caching. In general, use system instructions for short instructions that define how the model should behave; use prefix caching if you need to repeat a large part of your prompt across queries and need to optimize performance.

## How to use system instructions

To provide system instructions, create a `SystemInstruction` object and pass it to the `GenerateContentRequest` builder:

```kotlin
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest

// 1. Define the system instruction
val systemInstruction =
    SystemInstruction("You are a concise assistant. Answer in 2 sentences or less.")

// 2. Create the request
val request = generateContentRequest(TextPart("How does photosynthesis work?")) {
    this.systemInstruction = systemInstruction
}

// 3. Run inference
try {
    val response = generativeModel.generateContent(request)
    val text = response.candidates[0].text
} catch (e: Exception) {
    Log.e("SystemInstructions", "Error generating content: ${e.message}")
}
```
