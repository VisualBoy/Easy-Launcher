> [!NOTE]
> **Note:** Prefix caching is experimental and may change in the future. This feature is only available on a subset of the supported devices for Prompt API.

*Prefix caching* is a feature that reduces inference time by storing and reusing the intermediate LLM state of processing a shared and recurring prompt prefix part. To enable prefix caching, you only have to separate the static prefix from the dynamic suffix in your API request.

Prefix caching currently only supports text-only input, so you shouldn't use this feature if you're providing an image in your prompt.

There are two approaches to implement prefix caching: implicit or explicit:

- **Implicit (automatic) prefix caching** is a lightweight approach where the application only needs to define a shared portion of the prompt.
- **Explicit (manual) prefix caching** allows applications to have more control over caches, including cache creation, querying, and deletion.

## Use prefix caching implicitly

To enable prefix caching, add the shared portion of the prompt to the `promptPrefix` field:

### Kotlin

```kotlin
val promptPrefix = "Reverse the given sentence: "
val dynamicSuffix = "Hello World"

val result = generativeModel.generateContent(
  generateContentRequest(TextPart(dynamicSuffix)) {
    promptPrefix = PromptPrefix(promptPrefix)
  }
)
```

### Java

```java
String promptPrefix = "Reverse the given sentence: ";
String dynamicSuffix = "Hello World";

GenerateContentResponse response = generativeModelFutures.generateContent(
    new GenerateContentRequest.Builder(new TextPart(dynamicSuffix))
    .setPromptPrefix(new PromptPrefix(promptPrefix))
    .build()
).get();
```

## Use explicit cache management

### Kotlin

```kotlin
val cacheName = "my_cache"
val promptPrefix = "Reverse the given sentence: "
val dynamicSuffix = "Hello World"

// Create a cache
val cacheRequest = createCachedContextRequest(cacheName, PromptPrefix(promptPrefix))
val cache = generativeModel.caches.create(cacheRequest)

// Run inference with the cache
val response = generativeModel.generateContent(
  generateContentRequest(TextPart(dynamicSuffix)) {
    cachedContextName = cache.name
  }
)

// Query pre-created caches
for (cache in generativeModel.caches.list()) {
  // Do something with cache
}

// Get specific cache
val cache = generativeModel.caches.get(cacheName)

// Delete a pre-created cache
generativeModel.caches.delete(cacheName)
```
