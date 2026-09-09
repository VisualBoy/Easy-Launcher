If you need to parse the responses from the Prompt API into certain formats, such as JSON, for further processing, use the Structured Output API.

With the Structured Output API, you define the target output structure using Kotlin classes and annotations. The Prompt API then returns a response in the form of your Kotlin object.

Generating structured output is particularly useful for tasks like:
- **Entity extraction:** Extracting structured fields (e.g., event name, date, location) from unstructured text.
- **Classification:** Categorizing input text into predefined categories.
- **Data serialization:** Converting unstructured user input into a format suitable for database storage or API calls.

## Prerequisites

To verify that the Structured Output API is available on the device, use `isStructuredOutputFeatureAvailable()`:

```kotlin
suspend fun isStructuredOutputFeatureAvailable(): Boolean
```

Requirements:
- Android API level 26 or higher (`minSdk 26`)
- KSP plugin version 2.3.6 or higher

## Configure project

1. Add the ML Kit Prompt API dependency:
   ```kotlin
   implementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
   ```
2. Add KSP plugin in project-level `build.gradle.kts` / `build.gradle`:
   ```kotlin
   classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6")
   ```
3. Add schema compiler in app-level `build.gradle.kts`:
   ```kotlin
   dependencies {
       ksp("com.google.mlkit:genai-schema-compiler:1.0.0-alpha1")
   }
   ```

## Define output structure

```kotlin
import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

@Generable
data class PlantList(
    @Guide(description = "The list of plants found", minItems = 1, maxItems = 5)
    val plants: List<Plant>
)

@Generable("Information about a plant species")
data class Plant(
    @Guide(description = "The common name of the plant")
    val commonName: String,
    
    @Guide(description = "The full latin scientific name of the plant")
    val scientificName: String,
    
    @Guide(description = "The maximum height of the plant in centimeters.", minimum = 1.0, maximum = 10000.0)
    val maxHeightCm: Int,
    
    @Guide(description = "Whether the plant is poisonous or not")
    val isPoisonous: Boolean?,
    
    @Guide(
        description = "The primary continent where this plant is native to",
        enumValues = ["Africa", "Antarctica", "Asia", "Australia", "Europe", "North America", "South America"]
    )
    val nativeContinent: String
)
```

## Generate structured content

```kotlin
// 1. Initialize GenerativeModel
val generativeModel = Generation.getClient()

// 2. Prepare prompt
val promptText = "List some common plants found in California."
val baseRequest = GenerateContentRequest.Builder(TextPart(promptText)).build()

// 3. Create typed request
val typedRequest = generateTypedContentRequest(
    generateContentRequest = baseRequest,
    outputClass = PlantList::class
)

// 4. Run inference
try {
    val typedResponse = generativeModel.generateContent(typedRequest)
    val plantList: PlantList? = typedResponse.candidates.firstOrNull()?.response
    if (plantList != null) {
        for (plant in plantList.plants) {
            Log.d("StructuredOutput", "Found plant: ${plant.commonName} (${plant.scientificName})")
        }
    }
} catch (e: GenAiException) {
    Log.e("StructuredOutput", "API error: ${e.message}")
}
```
