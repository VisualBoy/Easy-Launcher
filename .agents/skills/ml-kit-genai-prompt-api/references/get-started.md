This page describes how to do the following:

- Configure your project to use Prompt API
- Provide text-only input and receive a response
- Provide an image input with related text input and receive a response

For more details about the Prompt API, see the reference documentation for Kotlin ([com.google.mlkit.genai.prompt](https://developer.android.com/android/reference/kotlin/com/google/mlkit/genai/prompt/package-summary)) and Java ([com.google.mlkit.genai.prompt.java](https://developer.android.com/android/reference/com/google/mlkit/genai/prompt/java/package-summary), [com.google.mlkit.genai.prompt](https://developer.android.com/android/reference/com/google/mlkit/genai/prompt/package-summary)).

## Configure project

> [!NOTE]
> **Note:** This API requires Android API level 26 or higher.

Add the ML Kit Prompt API as a dependency in your `build.gradle` configuration:

```groovy
implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
```

If you need your responses in a certain format using the Structured Output API, you need to configure KSP and add additional dependencies. For details, see [Generate structured output](structured-output.md).

## Implement generative model

To implement the code in your project, follow these steps:

- Create a `generativeModel` object:

### Kotlin

```kotlin
// Get a GenerativeModel instance
val generativeModel = Generation.getClient()
```

### Java

```java
// Get a GenerativeModel instance
GenerativeModelFutures generativeModelFutures = GenerativeModelFutures
    .from(Generation.INSTANCE.getClient());
```

- Check if Gemini Nano is `AVAILABLE`, `DOWNLOADABLE`, or `UNAVAILABLE`. Then, download the feature if it is downloadable:

### Kotlin

```kotlin
val status = generativeModel.checkStatus()
when (status) {
    FeatureStatus.UNAVAILABLE -> {
        // Gemini Nano not supported on this device or device hasn't fetched latest configuration
    }
    FeatureStatus.DOWNLOADABLE -> {
        // Gemini Nano can be downloaded on this device
        generativeModel.download().collect { status ->
            when (status) {
                is DownloadStatus.DownloadStarted -> {
                    Log.d(TAG, "starting download for Gemini Nano")
                }
                is DownloadStatus.DownloadProgress -> {
                    Log.d(TAG, "Nano ${status.totalBytesDownloaded} bytes downloaded")
                }
                DownloadStatus.DownloadCompleted -> {
                    Log.d(TAG, "Gemini Nano download complete")
                    modelDownloaded = true
                }
                is DownloadStatus.DownloadFailed -> {
                    Log.e(TAG, "Nano download failed ${status.e.message}")
                }
            }
        }
    }
    FeatureStatus.DOWNLOADING -> {
        // Gemini Nano currently being downloaded
    }
    FeatureStatus.AVAILABLE -> {
        // Gemini Nano currently downloaded and available to use on this device
    }
}
```

### Java

```java
ListenableFuture<Integer> status = generativeModelFutures.checkStatus();
Futures.addCallback(generativeModelFutures.checkStatus(), new FutureCallback<>() {
    @Override
    public void onSuccess(Integer featureStatus) {
        switch (featureStatus) {
            case FeatureStatus.AVAILABLE -> {
                // Gemini Nano currently downloaded and available
            }
            case FeatureStatus.UNAVAILABLE -> {
                // Gemini Nano not supported
            }
            case FeatureStatus.DOWNLOADING -> {
                // Gemini Nano currently being downloaded
            }
            case FeatureStatus.DOWNLOADABLE -> {
                generativeModelFutures.download(new DownloadCallback() {
                    @Override
                    public void onDownloadStarted(long l) {
                        Log.d(TAG, "starting download for Gemini Nano");
                    }
                    @Override
                    public void onDownloadProgress(long l) {
                        Log.d(TAG, "Nano " + l + " bytes downloaded");
                    }
                    @Override
                    public void onDownloadCompleted() {
                        Log.d(TAG, "Gemini Nano download complete");
                    }
                    @Override
                    public void onDownloadFailed(@NonNull GenAiException e) {
                        Log.e(TAG, "Nano download failed: " + e.getMessage());
                    }
                });
            }
        }
    }
    @Override
    public void onFailure(@NonNull Throwable t) {
        // Failed to check status
    }
}, ContextCompat.getMainExecutor(context));
```

## Provide text-only input

### Kotlin

```kotlin
val response = generativeModel.generateContent("Write a 3 sentence story about a magical dog.")
```

### Java

```java
GenerateContentResponse response = generativeModelFutures.generateContent(
    new GenerateContentRequest.Builder(
        new TextPart("Write a 3 sentence story about a magical dog."))
    .build())
    .get();
```

Alternatively, add optional parameters:

### Kotlin

```kotlin
val response = generativeModel.generateContent(
    generateContentRequest(
        TextPart("Write a 3 sentence story about a magical dog."),
    ) {
        temperature = 0.2f
        topK = 10
        candidateCount = 3
    }
)
```

### Java

```java
GenerateContentRequest.Builder requestBuilder =
    new GenerateContentRequest.Builder(
        new TextPart("Write a 3 sentence story about a magical dog."));
requestBuilder.setTemperature(.2f);
requestBuilder.setTopK(10);
requestBuilder.setCandidateCount(3);

GenerateContentResponse response =
    generativeModelFutures.generateContent(requestBuilder.build()).get();
```

## Provide multimodal (image and text) input

Bundle an image and a text input together in the `generateContentRequest()` function.

### Kotlin

```kotlin
val response = generativeModel.generateContent(
    generateContentRequest(ImagePart(bitmap), TextPart(textPrompt)) {
        // optional parameters
    }
)
```

### Java

```java
GenerateContentResponse response = generativeModelFutures.generateContent(
    new GenerateContentRequest.Builder(
        new ImagePart(bitmap),
        new TextPart("textPrompt"))
    .build()
).get();
```

## Process inference result

### Kotlin

```kotlin
// Non-streaming
val response = generativeModel.generateContent("Write a 3 sentence story about a magical dog")

// Streaming
var fullResponse = ""
generativeModel.generateContentStream("Write a 3 sentence story about a magical dog").collect { chunk ->
    val newChunkReceived = chunk.candidates[0].text
    print(newChunkReceived)
    fullResponse += newChunkReceived
}
```

## Latency optimization

To optimize for the first inference call, your application may optionally call `warmup()`. This loads Gemini Nano into memory and initializes runtime components:

```kotlin
generativeModel.warmup()
```

## Supported features and limitations

- Input must be under 4000 tokens (or approximately 3000 English words).
- Use cases that require long output (more than 4K tokens) should be avoided.
- AICore enforces an inference quota per app.
