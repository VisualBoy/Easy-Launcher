For mobile and web apps, the Firebase AI Logic SDKs let you interact
with the supported **Gemini models** directly from your app.

Gemini models are considered *multimodal* because they're capable of
processing and even generating multiple modalities, including text, code, PDFs,
images, video, and audio.

Also, review our [FAQ](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting#supported-models)
about all the models that Firebase AI Logic supports and does not support.

## Featured models

FAST AND INTELLIGENT

### Gemini 3.8 Flash

`gemini-3.8-flash`


Frontier-class performance rivaling larger models at a fraction of
the cost.
*(billing **not** required)*
ULTRA FAST

### Gemini 3.5 Flash-Lite

`gemini-3.5-flash-lite`


High-volume, cost-sensitive workhorse model with the performance and
quality of the Gemini 3 series.
*(billing **not** required)*
IMAGE GENERATING

### Gemini 3.1 Flash Image (*Nano Banana 2*)

`gemini-3.1-flash-image`

Powerful, high-efficiency image generation and editing model,
optimized for speed and high-volume use cases.
*(billing required)*

## General-use models

[Go to tables with model details](https://firebase.google.com/docs/ai-logic/models#compare-models)

### Gemini 3.1 Pro

`gemini-3.1-pro-preview`


Advanced intelligence, complex problem-solving skills, and powerful
agentic and vibe coding capabilities.
*(billing required)*

### Gemini 3.8 Flash

`gemini-3.8-flash`


Frontier-class performance rivaling larger models at a fraction of
the cost.
*(billing **not** required)*

### Gemini 3.5 Flash-Lite

`gemini-3.5-flash-lite`


High-volume, cost-sensitive workhorse model with the performance and
quality of the Gemini 3 series.
*(billing **not** required)*

##### Older stable general-use models

- **Gemini 3.7 Flash**
  (`gemini-3.7-flash`):
  Previous Gemini 3.x Flash model for frontier-class performance
  rivaling larger models at a fraction of the cost.
  *(billing **not** required)*

- **Gemini 3.6 Flash**
  (`gemini-3.6-flash`):
  Previous Gemini 3.x Flash model for frontier-class performance
  rivaling larger models at a fraction of the cost.
  *(billing **not** required)*

- **Gemini 3.5 Flash**
  (`gemini-3.5-flash`):
  Previous Gemini 3.x Flash model for frontier-class performance
  rivaling larger models at a fraction of the cost.
  *(billing **not** required)*

- **Gemini 3.1 Flash‑Lite**
  (`gemini-3.1-flash-lite`):
  Previous Gemini 3.x Flash‑Lite model for high-volume, cost-sensitive
  workhorse tasks.
  *(billing **not** required)*

## Image-generating models

[Go to tables with model details](https://firebase.google.com/docs/ai-logic/models#compare-models)

### Gemini 3 Pro Image (*Nano Banana Pro*)

`gemini-3-pro-image`

State-of-the-art image generation and editing model for highly
contextual native image creation.
*(billing required)*

### Gemini 3.1 Flash Image (*Nano Banana 2*)

`gemini-3.1-flash-image`

Powerful, high-efficiency image generation and editing model,
optimized for speed and high-volume use cases.
*(billing required)*

### Gemini 3.1 Flash-Lite Image (*Nano Banana 2 Lite*)

`gemini-3.1-flash-lite-image`

Ultra-low latency and cost-effective image generation and editing
model, designed for high-volume interactive use cases.
*(billing required)*

## Audio-generating models

### Text-to-speech (TTS) models

You can generate speech from text input with Gemini TTS models.

[Go to tables with model details](https://firebase.google.com/docs/ai-logic/models#compare-models)

### Gemini 3.x Flash TTS

`gemini-3.1-flash-tts-preview`

Powerful, low-latency speech generation from text input.
*(billing **not** required)*

### Live API models

You can generate *bidirectional streamed* audio with models that support the
Gemini Live API.

[Go to tables with model details](https://firebase.google.com/docs/ai-logic/models#compare-models)

### Gemini 3.x Flash with Gemini Live API native audio

Gemini Developer API:  
`gemini-3.1-flash-live-preview`

Agent Platform Gemini API:  
*not supported*

Enables low-latency, real-time voice and video interactions with a
Gemini model that is *bidirectional* .
*(billing **not** required)*

### Gemini 2.5 Flash with Gemini Live API native audio

Gemini Developer API:  
`gemini-2.5-flash-native-audio-preview-12-2025`

Agent Platform Gemini API:  
`gemini-live-2.5-flash-native-audio`

Enables low-latency, real-time voice and video interactions with a
Gemini model that is *bidirectional* .
*(billing **not** required)*

<br />

**The remainder of this page provides detailed information about the models
supported by Firebase AI Logic.**

- [Compare models](https://firebase.google.com/docs/ai-logic/models#compare-models):

  - Supported input and output
  - High-level comparison of the supported capabilities
  - Specifications and limitations, for example max input tokens or max length of input video
- Description of [how models are versioned](https://firebase.google.com/docs/ai-logic/models#versions), specifically their
  *stable* , *preview* , and *experimental* versions

- Lists of [available model names](https://firebase.google.com/docs/ai-logic/models#available-model-names) to include in your
  code during initialization

- Lists of [supported languages](https://firebase.google.com/docs/ai-logic/models#languages) for the models

At the bottom of this page, you can
[view detailed information about previous generation models](https://firebase.google.com/docs/ai-logic/models#older-models).

<br />

*** ** * ** ***

## Compare models

Each model has different capabilities to support various use cases. Note that
each of tables in this section describe each model
*when used with Firebase AI Logic*. Each model might have additional
capabilities that aren't available when using our SDKs.

If you can't find the information you're looking for in the following
sub-sections, you can find even more information in your chosen API provider
documentation:
[Gemini Developer API](https://ai.google.dev/gemini-api/docs/models)
or
[Agent Platform Gemini API (formerly Vertex AI)](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/google-models).

> [!NOTE]
> **Note:** We recommend reviewing details about [the location for where you access a model](https://firebase.google.com/docs/ai-logic/locations?api=vertex). The Gemini Developer API provides only global access to models, but the Agent Platform Gemini API (formerly Vertex AI) provides both global access (recommended for most use cases) and setting a specific location (available locations depend on the model).

### Supported input and output

The following table lists the supported input and output types
*when using each model with Firebase AI Logic*.

To learn about supported file types, see
[Supported input files and requirements](https://firebase.google.com/docs/ai-logic/input-file-requirements).

|   | Gemini 3.x Pro, Flash, Flash‑Lite | Gemini 3.x Pro Image | Gemini 3.x Flash Image | Gemini 3.x Flash‑Lite Image | Gemini 3.x Flash TTS | Gemini Live |
|---|---|---|---|---|---|---|
| **Input types** |||||||
| Text | Yes | Yes | Yes | Yes | Yes | Yes |
| Code | Yes | Yes | Yes | Yes | No | No |
| Documents (PDFs or plain-text) | Yes | Yes | Yes | Yes | No | No |
| Images | Yes | Yes | Yes | Yes | No | Yes |
| Video | Yes | No | Yes | Yes | No | Yes |
| Audio | Yes | No | No | No | No | Yes |
| **Output types** |||||||
| Text | Yes | Yes | Yes | Yes | No | Yes |
| Text (streaming) | Yes | No | No | No | No | Yes |
| Code | Yes | Yes | Yes | Yes | No | No |
| Structured output (like JSON) | Yes | No | No | No | No | No |
| Images | No | Yes | Yes | Yes | No | No |
| Video | No | No | No | No | No | No |
| Audio | No | No | No | No | Yes | Yes |

### Supported capabilities and features

The following table lists the supported capabilities and features
*when using each model with Firebase AI Logic*.

|   | Gemini 3.x Pro, Flash, Flash‑Lite | Gemini 3.x Pro Image | Gemini 3.x Flash Image | Gemini 3.x Flash‑Lite Image | Gemini 3.x Flash TTS | Gemini Live |
|---|---|---|---|---|---|---|
| [Thinking](https://firebase.google.com/docs/ai-logic/thinking) | Yes | Yes | Yes | Yes | No | Yes |
| [Generate text](https://firebase.google.com/docs/ai-logic/generate-text) from text-only or multimodal inputs | Yes | [*interleaved or as part of image*](https://firebase.google.com/docs/ai-logic/generate-images-gemini) | [*interleaved or as part of image*](https://firebase.google.com/docs/ai-logic/generate-images-gemini) | [*interleaved or as part of image*](https://firebase.google.com/docs/ai-logic/generate-images-gemini) | No | [*transcription of audio*](https://firebase.google.com/docs/ai-logic/live-api/capabilities#text-in-audio-out) |
| [Generate images](https://firebase.google.com/docs/ai-logic/generate-images-gemini) | No | Yes | Yes | Yes | No | No |
| [Edit images](https://firebase.google.com/docs/ai-logic/generate-images-gemini) | No | Yes | Yes | Yes | No | No |
| Generate audio | No | No | No | No | [*speech*](https://firebase.google.com/docs/ai-logic/generate-speech) | [*streamed audio*](https://firebase.google.com/docs/ai-logic/live-api/capabilities#input-modalities) |
| [Generate structured output](https://firebase.google.com/docs/ai-logic/generate-structured-output) (like JSON) | Yes | No | No | No | No | No |
| Analyze documents (PDFs or plain-text) ([text-output](https://firebase.google.com/docs/ai-logic/analyze-documents) \| [image-output](https://firebase.google.com/docs/ai-logic/generate-images-gemini)) | Yes | Yes | Yes | Yes | No | No |
| Analyze images ([text-output](https://firebase.google.com/docs/ai-logic/analyze-images) \| [image-output](https://firebase.google.com/docs/ai-logic/generate-images-gemini)) | Yes | Yes | Yes | Yes | No | No |
| Analyze video ([text-output](https://firebase.google.com/docs/ai-logic/analyze-videos) \| [image-output](https://firebase.google.com/docs/ai-logic/generate-images-gemini)) | Yes | No | Yes | Yes | No | [*streamed video*](https://firebase.google.com/docs/ai-logic/live-api/capabilities#input-modalities) |
| [Analyze audio](https://firebase.google.com/docs/ai-logic/analyze-audio) | Yes | No | No | No | No | [*streamed audio*](https://firebase.google.com/docs/ai-logic/live-api/capabilities#input-modalities) |
| [Multi-turn chat](https://firebase.google.com/docs/ai-logic/chat) | Yes | Yes | Yes | Yes | No | No |
| [Bidirectional multimodal streaming](https://firebase.google.com/docs/ai-logic/live-api) | No | No | No | No | No | Yes |
| **Supported tools** |||||||
| [Function calling](https://firebase.google.com/docs/ai-logic/function-calling) | Yes | No | No | No | No | Yes |
| [Code execution](https://firebase.google.com/docs/ai-logic/code-execution) | Yes | No | No | No | No | No |
| [URL context](https://firebase.google.com/docs/ai-logic/url-context) | Yes | No | No | No | No | No |
| [Grounding with Google Search](https://firebase.google.com/docs/ai-logic/grounding-google-search) | Yes | Yes | Yes | No | No | Yes |
| [Grounding with Google Maps](https://firebase.google.com/docs/ai-logic/grounding-google-maps) | Yes | No | No | No | No | No |

> [!NOTE]
> **Note:** *When using Firebase AI Logic* , the following capabilities are ***not*** supported: grounding with Google Image Search, fine tuning a model, embeddings generation, and semantic retrieval.

### Specifications and limitations

The following table lists the specifications and limitations
*when using each model with Firebase AI Logic*.

| Property | Gemini 3.x Pro, Flash, Flash‑Lite | Gemini 3.x Pro Image | Gemini 3.x Flash Image | Gemini 3.x Flash‑Lite Image | Gemini 3.x Flash TTS | Gemini Live |
|---|---|---|---|---|---|---|
| Input token limit **\*** | 1,048,576 tokens | 65,536 tokens | 131,072 tokens | 65,536 tokens | 8,192 tokens | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Output token limit **\*** | 65,536 tokens | 32,768 tokens | 32,768 tokens | 4,096 tokens | 16,384 tokens | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| **PDFs (per request)** |||||||
| Max number of input PDF files **\*\*** | 900 files | 14 files | 14 files | 14 files | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Max number of pages per input PDF file **\*\*** | 900 pages | 14 pages | 14 pages | 14 pages | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Max size per input PDF file | 50 MB | 50 MB | 50 MB | 50 MB | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| **Images (per request)** |||||||
| Max number of *input* images | 1,000 images | 14 images | 14 images | 14 images | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Max size per input base64-encoded image | 7 MB | 7 MB | 7 MB | 7 MB | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Max number of *output* images | --- | Up to output token limit | Up to output token limit | Up to output token limit | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| **Video (per request)** |||||||
| Max number of input video files | 10 files | --- | Up to input token limit | Up to input token limit | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Max length of all input video (frames only) | \~60 minutes | --- | \~25 minutes | \~12 minutes | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Max length of all input video (frames+audio) | \~45 minutes | --- | --- | --- | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| **Audio (per request)** |||||||
| Max number of input audio files | 1 file | --- | --- | --- | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |
| Max length of all input audio | \~8.4 hours | --- | --- | --- | --- | [*see docs*](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs) |


^\*
*For all Gemini models, a token is equivalent to about 4 characters,
so 100 tokens are about 60-80 English words. For Gemini models, you can
determine the total count of tokens in your requests using
[`countTokens`](https://firebase.google.com/docs/ai-logic/count-tokens).*^


^\*\*
*PDFs are treated as images, so a single page of a PDF is treated as
one image. The number of pages allowed in a request is limited to the number
of images the model can support.*^

#### Find additional detailed information

- [Quotas](https://firebase.google.com/docs/ai-logic/quotas) and [pricing](https://firebase.google.com/docs/ai-logic/pricing) are
  different for each model. Pricing also depends on input and output.

- Learn about supported input file types, how to specify MIME type, and how to
  make sure that your input files and multimodal requests meet the requirements
  and follow best practices in
  [Supported input files and requirements](https://firebase.google.com/docs/ai-logic/input-file-requirements).


  > [!NOTE]
  > **Important** : **The total request size limit is
  > 20 MB.** To send large files, review the [options for providing files in multimodal requests](https://firebase.google.com/docs/ai-logic/input-file-requirements).

  <br />

<br />

*** ** * ** ***

## Model versioning and naming patterns

Models are offered in *stable* , *preview* , and *experimental* versions. For
convenience, the `-latest` aliases are supported, but
they're not recommended due to the inconsistency in which model version they
point to.

**Make sure to review our
[best practices for using model versions](https://firebase.google.com/docs/ai-logic/models#versions-best-practices).**

To find specific model names to use in your code, see the
["available model names"](https://firebase.google.com/docs/ai-logic/models#available-model-names) section later on this page.

| Version type / Release stage || Description | Model name pattern |
|---|---|---|---|
| **Stable** || ***Stable*** versions are available and supported for production use starting on the release date. - A stable model version is typically released with a retirement date, which indicates the last day that the model is available. After this date, the model is no longer accessible or supported by Google. - Most stable models are available for 12 months after their release date (even if a replacement model is released). - For the Agent Platform Gemini API (formerly Vertex AI), some stable models only have [*short-term availability*](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/model-versions#short-term-availability), which means that they may be shut down (retired) as early as 45 days after a replacement model is released. | Model names of stable versions have no suffix Example: `gemini-3.8-flash` |
| **Preview** || ***Preview*** versions have new capabilities and are considered *not stable* . - These models are *not* recommended for production use, come with more restrictive rate limits, and may have billing requirements. - These models are shut down (retired) within a few weeks or months after their associated stable version is released. - For the Agent Platform Gemini API (formerly Vertex AI), preview models are *only* available in the [`global` location](https://firebase.google.com/docs/ai-logic/locations?api=vertex). | Model names of preview versions are appended with `-preview` Example: `gemini-3-pro-preview` |
| **Experimental** || ***Experimental*** versions have new capabilities and are considered *not stable* . - These models are *not* recommended for production use and come with more restrictive rate limits. Experimental models are intended for gathering feedback and to enable experimentation with our latest features. - These models are shut down (retired) within a few weeks or months after their associated stable version is released. - For the Agent Platform Gemini API (formerly Vertex AI), experimental models are *only* available in the [`global` location](https://firebase.google.com/docs/ai-logic/locations?api=vertex). | Model names of experimental versions are appended with `-exp` along with the model's release date (`-MM-DD`) Example: `gemini-2.5-pro-exp-03-25` (released on March 25, 2025) |
| **Shutdown (retired)** || ***Shutdown (retired)*** versions are past their shutdown (retirement) date and have been permanently deactivated. - Shutdown (retired) models are no longer accessible or supported by Google, and a request using a retired model name returns a 404 error. | --- |

### Best practices for using model versions

- **In your *production apps* , use the explicit model name for the most recent
  *stable* version.**

  For Agent Platform Gemini API (formerly Vertex AI), if you choose to use a
  *short-term availability model* in your production app, it's even more
  critical that you use
  [Firebase Remote Config](https://firebase.google.com/docs/ai-logic/change-model-name-remotely) or
  [server prompt templates](https://firebase.google.com/docs/ai-logic/server-prompt-templates/get-started)
  to control the model name used for your AI feature.
- **Use *preview* and *experimental* versions *only during prototyping*** . We
  recommend using a *stable* version when you start developing and testing for
  a production use case.

- We do *not* recommend using the `-latest` aliases
  (even during development). This alias points to the latest release for a
  specific model variation (which could be a stable, preview, or experimental
  version). This alias will get hot-swapped with every new release of a specific
  model variation, and only for breaking changes will a 2-week-prior
  notification email be sent. This instability of which model you're actually
  using can lead to unexpected behavior changes for your AI feature.

> [!CAUTION]
> **We *strongly* recommend using
> [Firebase Remote Config](https://firebase.google.com/docs/ai-logic/change-model-name-remotely)
> or
> [server prompt templates](https://firebase.google.com/docs/ai-logic/server-prompt-templates/get-started)
> so that you can make on-demand changes to the model name used for your AI
> feature without releasing a new version of your app.**

<br />

*** ** * ** ***

## Available model names

Model names are the explicit values that you include *in your code* during
initialization of the model.

- [**General-use models**](https://firebase.google.com/docs/ai-logic/models#model-names-general-use)
  (like `gemini-3.8-flash`)

- [**Image-generating models**](https://firebase.google.com/docs/ai-logic/models#model-names-image-generating)
  (like `gemini-3.1-flash-image`, aka the "Nano Banana" models)

- **Audio-generating models**:

  - [Text-to-speech (TTS) models](https://firebase.google.com/docs/ai-logic/models#model-names-audio-generating-tts) (like `gemini-3.1-flash-tts-preview`)
  - [Live API models](https://firebase.google.com/docs/ai-logic/models#model-names-audio-generating-live) (like `gemini-live-2.5-flash-native-audio`)

For initialization examples for your platform, see the
[getting started guide](https://firebase.google.com/docs/ai-logic/get-started).

For details about the release stages (especially for use cases, billing, and
shutdown), see
[model versioning and naming patterns](https://firebase.google.com/docs/ai-logic/models#versions).

#### Programmatically list all available models

You can list all available models names using the REST API:

- Gemini Developer API: Call the
  [`models.list` endpoint](https://ai.google.dev/api/models#method:-models.list)

- Agent Platform Gemini API (formerly Vertex AI): Call the
  [`publishers.models.list` endpoint](https://docs.cloud.google.com/gemini-enterprise-agent-platform/reference/rest/v1beta1/publishers.models/list)

Note that this returned list will include *all* models supported by the
API providers, but Firebase AI Logic only supports the
Gemini models described on this page.

### General-use models

#### Gemini 3.x Pro model names

^*Requires the [pay-as-you-go Blaze pricing plan](https://firebase.google.com/pricing) regardless of your Gemini API
provider.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3.1-pro-preview` | Latest preview version of Gemini 3.x Pro | Preview | 2026-02-19 | To be determined |

#### Gemini 3.x Flash model names

^*Does **not** require the pay-as-you-go Blaze pricing plan if you're
using the Gemini Developer API.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3.8-flash` | Latest stable version of Gemini 3.x Flash This is a *short-term availability model* (see [Model versioning and naming patterns](https://firebase.google.com/docs/ai-logic/models#versions)). | Stable | 2026-09-02 | To be determined |
| `gemini-3.7-flash` | Stable version of Gemini 3.x Flash This is a *short-term availability model* (see [Model versioning and naming patterns](https://firebase.google.com/docs/ai-logic/models#versions)). | Stable | 2026-08-13 | To be determined |
| `gemini-3.6-flash` | Stable version of Gemini 3.x Flash This is a *short-term availability model* (see [Model versioning and naming patterns](https://firebase.google.com/docs/ai-logic/models#versions)). | Stable | 2026-07-21 | To be determined |
| `gemini-3.5-flash` | Stable version of Gemini 3.x Flash | Stable | 2026-05-19 | No earlier than 2027-05-19 |
| `gemini-3-flash-preview` | Preview version of Gemini 3.x Flash | Preview | 2025-12-17 | To be determined |

#### Gemini 3.x Flash‑Lite model names

^*Does **not** require the pay-as-you-go Blaze pricing plan if you're
using the Gemini Developer API.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3.5-flash-lite` | Latest stable version of Gemini 3.x Flash‑Lite | Stable | 2026-07-21 | No earlier than 2027-07-21 |
| `gemini-3.1-flash-lite` | Stable version of Gemini 3.x Flash‑Lite | Stable | 2026-05-07 | No earlier than 2027-05-07 |

### Image-generating models

#### Gemini 3.x Pro Image model names (aka "Nano Banana Pro")

^*Requires the [pay-as-you-go Blaze pricing plan](https://firebase.google.com/pricing) regardless of your Gemini API
provider.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3-pro-image` | Stable version of Gemini 3.x Pro Image (aka "Nano Banana Pro") | Stable | 2026-05-28 | No earlier than 2027-05-28 |
| `gemini-3-pro-image-preview` | Preview version of Gemini 3.x Pro Image (aka "Nano Banana Pro") | Preview | 2025-11-20 | As early as 2026-06-25 |

#### Gemini 3.x Flash Image model names (aka "Nano Banana 2")

^*Requires the [pay-as-you-go Blaze pricing plan](https://firebase.google.com/pricing) regardless of your Gemini API
provider.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3.1-flash-image` | Stable version of Gemini 3.x Flash Image (aka "Nano Banana 2") | Stable | 2026-05-28 | No earlier than 2027-05-28 |
| `gemini-3.1-flash-image-preview` | Preview version of Gemini 3.x Flash Image (aka "Nano Banana 2") | Preview | 2026-02-26 | As early as 2026-06-25 |

#### Gemini 3.x Flash‑Lite Image model names (aka "Nano Banana 2 Lite")

^*Requires the [pay-as-you-go Blaze pricing plan](https://firebase.google.com/pricing) regardless of your Gemini API
provider.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3.1-flash-lite-image` | Stable version of Gemini 3.x Flash‑Lite Image (aka "Nano Banana 2 Lite") This is a *short-term availability model* (see [Model versioning and naming patterns](https://firebase.google.com/docs/ai-logic/models#versions)). | Stable | 2026-06-30 | To be determined |

### Audio - text-to-speech (TTS) models

#### Gemini 3.x Flash TTS model names

^*Does **not** require the pay-as-you-go Blaze pricing plan if you're
using the Gemini Developer API.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3.1-flash-tts-preview` | Preview version for Gemini 3.x Flash TTS | Preview | 2026-06-17 | To be determined |

> [!NOTE]
> **Note:** Firebase AI Logic supports the Gemini 2.x TTS models, but our documentation for TTS models is focused on using the 3.x TTS models.

### Audio - Live API models

#### Gemini 3.x Flash Live model names

| **Gemini Developer API Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-3.1-flash-live-preview` ^1^ | Preview version for the Live API on the Gemini Developer API | Preview | 2026-03-26 | To be determined |

| **Agent Platform Gemini API (formerly Vertex AI) Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| The Agent Platform Gemini API (formerly Vertex AI) does not support any Gemini Live 3.x models. ||||

^**1** ***Only** supported by the Gemini Developer API.
Also, even though this is a preview model, it's available on the
"free tier" of the Gemini Developer API.*^  

#### Gemini 2.5 Flash Live model names

^*Does **not** require the pay-as-you-go Blaze pricing plan if you're
using the Gemini Developer API (usually preview models require a paid
plan).*^

Even though the following models have different model names depending on the
Gemini API provider, the features of the model are the same.

| **Gemini Developer API Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-2.5-flash-native-audio-preview-12-2025` ^1^ | Preview version for the Live API on the Gemini Developer API | Preview | 2025-12-12 | To be determined |
| `gemini-2.5-flash-native-audio-preview-09-2025` ^1^ | Initial preview version for the Live API on the Gemini Developer API | Preview | 2025-09-18 | To be determined |

| **Agent Platform Gemini API (formerly Vertex AI) Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-live-2.5-flash-native-audio` ^2^ | Stable version for the Live API on the Agent Platform Gemini API (formerly Vertex AI) | Stable | 2025-12-12 | No earlier than 2026-12-12 |
| `gemini-live-2.5-flash-preview-native-audio-09-2025` ^2^ | Preview version for the Live API on the Agent Platform Gemini API (formerly Vertex AI) | Preview | 2025-09-18 | To be determined |

^**1** ***Only** supported by the Gemini Developer API.
Also, even though these are preview models, they're available on the
"free tier" of the Gemini Developer API.*^  

^**2** ***Only** supported by the Agent Platform Gemini API (formerly Vertex AI).
Also, these models are **not** available in the `global`
location.*^

<br />

*** ** * ** ***

## Supported languages

> [!NOTE]
> **Note:** These languages *do not represent the locations for accessing the model* ; instead, these are the *languages* that the models can understand and respond in (for example, the text input and output). If needed, see [specify the location for accessing a model](https://firebase.google.com/docs/ai-logic/locations?api=vertex).

### General-use models

All Gemini general-use models (like Gemini 3.8 Flash,
Gemini 3.5 Flash-Lite, Gemini 3.1 Pro, and earlier 3.x models)
can understand and respond in over 100 languages:

Afrikaans (`af`), Albanian (`sq`), Amharic (`am`), Arabic (`ar`),
Armenian (`hy`), Assamese (`as`), Azerbaijani (`az`), Basque (`eu`),
Belarusian (`be`), Bengali (`bn`), Bosnian (`bs`), Bulgarian (`bg`),
Catalan (`ca`), Cebuano (`ceb`), Chinese simplified and traditional (`zh`),
Corsican (`co`), Croatian (`hr`), Czech (`cs`), Danish (`da`), Dhivehi (`dv`),
Dutch (`nl`), English (`en`), Esperanto (`eo`), Estonian (`et`),
Filipino (Tagalog) (`fil`), Finnish (`fi`), French (`fr`), Frisian (`fy`),
Galician (`gl`), Georgian (`ka`), German (`de`), Greek (`el`), Gujarati (`gu`),
Haitian Creole (`ht`), Hausa (`ha`), Hawaiian (`haw`), Hebrew (`iw`),
Hindi (`hi`), Hmong (`hmn`), Hungarian (`hu`), Icelandic (`is`), Igbo (`ig`),
Indonesian (`id`), Irish (`ga`), Italian (`it`), Japanese (`ja`),
Javanese (`jv`), Kannada (`kn`), Kazakh (`kk`), Khmer (`km`), Korean (`ko`),
Krio (`kri`), Kurdish (`ku`), Kyrgyz (`ky`), Lao (`lo`), Latin (`la`),
Latvian (`lv`), Lithuanian (`lt`), Luxembourgish (`lb`), Macedonian (`mk`),
Malagasy (`mg`), Malay (`ms`), Malayalam (`ml`), Maltese (`mt`), Maori (`mi`),
Marathi (`mr`), Meiteilon (Manipuri) (`mni-Mtei`), Mongolian (`mn`),
Myanmar (Burmese) (`my`), Nepali (`ne`), Norwegian (`no`),
Nyanja (Chichewa) (`ny`), Odia (Oriya) (`or`), Pashto (`ps`), Persian (`fa`),
Polish (`pl`), Portuguese (`pt`), Punjabi (`pa`), Romanian (`ro`),
Russian (`ru`), Samoan (`sm`), Scots Gaelic (`gd`), Serbian (`sr`),
Sesotho (`st`), Shona (`sn`), Sindhi (`sd`), Sinhala (Sinhalese) (`si`),
Slovak (`sk`), Slovenian (`sl`), Somali (`so`), Spanish (`es`),
Sundanese (`su`), Swahili (`sw`), Swedish (`sv`), Tajik (`tg`), Tamil (`ta`),
Telugu (`te`), Thai (`th`), Turkish (`tr`), Ukrainian (`uk`), Urdu (`ur`),
Uyghur (`ug`), Uzbek (`uz`), Vietnamese (`vi`), Welsh (`cy`), Xhosa (`xh`),
Yiddish (`yi`), Yoruba (`yo`), and Zulu (`zu`).

### Image-generating models

The Gemini 3.x Image models can accept text prompts across many
languages. However, they are optimized for best performance (both for text
prompts and for rendering text within generated images) in the following
languages and locales:

Arabic (`ar-EG`), German (`de-DE`), English (`EN`), Spanish (`es-MX`),
French (`fr-FR`), Hindi (`hi-IN`), Indonesian (`id-ID`), Italian (`it-IT`),
Japanese (`ja-JP`), Korean (`ko-KR`), Portuguese (`pt-BR`), Russian (`ru-RU`),
Ukrainian (`uk-UA`), Vietnamese (`vi-VN`), and Simplified Chinese (`zh-CN`).

To learn more about prompt best practices and generating text in images, see
[Supported languages for image generation](https://firebase.google.com/docs/ai-logic/generate-images-gemini#supported-languages).

### Audio-generating models (TTS and Live API)

Audio-generating models support specialized language sets optimized for speech
synthesis and (for the Live API) real-time bidirectional dialogue:

- **Text-to-speech (TTS) models** :
  The Gemini 3.x TTS models can automatically detect and generate
  speech in nearly 80 languages and dialects.

  To view the full list of supported languages and available BCP-47 locale
  codes, see
  [Supported languages for speech generation](https://firebase.google.com/docs/ai-logic/generate-speech#languages).
- **Live API models** :
  Live API models support real-time, bidirectional voice interactions in
  nearly 80 languages and dialects.

  To view the full list of supported languages and learn how to influence the
  spoken response language, see
  [Supported languages for the Live API](https://firebase.google.com/docs/ai-logic/live-api/limits-and-specs#languages).

<br />

*** ** * ** ***

## Information about previous models

The following are active, but previous generation models. We recommend using one
of the latest models instead when possible.

If you can't find the information you're looking for in the following
sub-sections, you can find even more information in your chosen API provider
documentation:
[Gemini Developer API](https://ai.google.dev/gemini-api/docs/models)
or
[Agent Platform Gemini API (formerly Vertex AI)](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/google-models)

> [!WARNING]
>
> Gemini 2.5 models are limited to projects that actively used them
> in the past. Stable Gemini Live API 2.5 models are not impacted.  
>
> For the *Gemini Developer API* , these models can continue
> to be used by previously active projects until further notice. For the
> *Agent Platform Gemini API (formerly Vertex AI)* , these models will shut down for all
> projects in October 2026.
>
>
> Gemini 2.0 Flash and Gemini 2.0 Flash‑Lite models were
> shut down on June 1, 2026 (stable Gemini Live API 2.0 models are not
> impacted). All Gemini 1.0 models and Gemini 1.5
> are already shutdown, and all requests to these models return a 404 error.
>
>
> To avoid service disruption, update to a
> [newer model](https://firebase.google.com/docs/ai-logic/models) (for example,
> `gemini-3.8-flash` or `gemini-3.1-flash-lite`).
> [Learn more.](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting#discontinued-models)
>
>
> **We *strongly* recommend using
> [Firebase Remote Config](https://firebase.google.com/docs/ai-logic/change-model-name-remotely)
> or
> [server prompt templates](https://firebase.google.com/docs/ai-logic/server-prompt-templates/get-started)
> so that you can make on-demand changes to the model name used for your AI
> feature without releasing a new version of your app.**

> [!WARNING]
> **All Imagen models shut down on
> August 17, 2026.** As a replacement, you can [migrate your apps to use
> Gemini Image models (the "Nano Banana" models)](https://firebase.google.com/docs/ai-logic/imagen-models-migration).

#### Older Gemini models

- `gemini-2.5-pro`
- `gemini-2.5-flash`
- `gemini-2.5-flash-lite`
- `gemini-2.5-flash-image` (aka "Nano Banana")
- `gemini-2.0-flash-001` (and its auto-updated alias `gemini-2.0-flash`)
- `gemini-2.0-flash-lite-001` (and its auto-updated alias `gemini-2.0-flash-lite`)

For information about older Gemini Live API models, see the
Gemini API provider documentation:

- [`gemini-2.0-flash-live-001`](https://ai.google.dev/gemini-api/docs/models#gemini-2.0-flash-live)
- [`gemini-2.0-flash-live-preview-04-09`](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/gemini/2-0-flash#live-api)
- [`gemini-live-2.5-flash-preview`](https://ai.google.dev/gemini-api/docs/models#gemini-2.5-flash-live)

#### Older Imagen models

- `imagen-4.0-ultra-generate-001`
- `imagen-4.0-generate-001`
- `imagen-4.0-fast-generate-001`
- `imagen-3.0-capability-001`
- `imagen-3.0-generate-002`
- `imagen-3.0-generate-001`
- `imagen-3.0-fast-generate-001`

#### View details about about previous models

<br />

View supported input and output of previous generation models

<br />

These are the input and output types
*when using each model with Firebase AI Logic*:

|   | Gemini 2.5 Pro, Flash, Flash‑Lite | Gemini 2.5 Flash Image | Gemini 2.0 Flash | Gemini 2.0 Flash‑Lite | Imagen (generate) | Imagen (capability) |
|---|---|---|---|---|---|---|
| **Input types** |||||||
| Text | Yes | Yes | Yes | Yes | Yes | Yes |
| Code | Yes | Yes | Yes | Yes | No | No |
| Documents (PDFs or plain-text) | Yes | Yes | Yes | Yes | No | No |
| Images | Yes | Yes | Yes | Yes | No | Yes |
| Video | Yes | No | Yes | Yes | No | No |
| Audio | Yes | No | Yes | Yes | No | No |
| Audio (streaming) | No | No | No | No | No | No |
| **Output types** |||||||
| Text | Yes | Yes | Yes | Yes | No | No |
| Text (streaming) | Yes | No | Yes | Yes | No | No |
| Code | Yes | Yes | Yes | Yes | No | No |
| Structured output (like JSON) | Yes | No | Yes | Yes | No | No |
| Images | No | Yes | No | No | Yes | Yes |
| Video | No | No | No | No | No | No |
| Audio | No | No | No | No | No | No |
| Audio (streaming) | No | No | No | No | No | No |

<br />

<br />

<br />

Supported capabilities and features of previous generation models

<br />

These are the capabilities and features
*when using each model with Firebase AI Logic*:

|   | Gemini 2.5 Pro, Flash, Flash‑Lite | Gemini 2.5 Flash Image | Gemini 2.0 Flash | Gemini 2.0 Flash‑Lite | Imagen (generate) | Imagen (capability) |
|---|---|---|---|---|---|---|
| [Thinking](https://firebase.google.com/docs/ai-logic/thinking) | Yes | No | No | No | No | No |
| [Generate text](https://firebase.google.com/docs/ai-logic/generate-text) from text-only or multimodal inputs | Yes | [*interleaved or as part of image*](https://firebase.google.com/docs/ai-logic/generate-images-gemini) | Yes | Yes | No | No |
| Generate images ([Gemini](https://firebase.google.com/docs/ai-logic/generate-images-gemini) or [Imagen](https://firebase.google.com/docs/ai-logic/generate-images-imagen)) | No | Yes | No | No | Yes | Yes |
| Edit images ([Gemini](https://firebase.google.com/docs/ai-logic/generate-images-gemini) or [Imagen](https://firebase.google.com/docs/ai-logic/edit-images-imagen-overview)) | No | Yes | No | No | No | Yes |
| Generate audio | No | No | No | No | No | No |
| [Generate structured output](https://firebase.google.com/docs/ai-logic/generate-structured-output) (like JSON) | Yes | No | Yes | Yes | No | No |
| Analyze documents (PDFs or plain-text) ([text-output](https://firebase.google.com/docs/ai-logic/analyze-documents) \| [image-output](https://firebase.google.com/docs/ai-logic/generate-images-gemini)) | Yes | Yes | Yes | Yes | No | No |
| Analyze images ([text-output](https://firebase.google.com/docs/ai-logic/analyze-images) \| [image-output](https://firebase.google.com/docs/ai-logic/generate-images-gemini)) | Yes | Yes | Yes | Yes | No | No |
| Analyze video ([text-output](https://firebase.google.com/docs/ai-logic/analyze-videos) \| [image-output](https://firebase.google.com/docs/ai-logic/generate-images-gemini)) | Yes | No | Yes | Yes | No | No |
| [Analyze audio](https://firebase.google.com/docs/ai-logic/analyze-audio) | Yes | No | Yes | Yes | No | No |
| [Multi-turn chat](https://firebase.google.com/docs/ai-logic/chat) | Yes | Yes | Yes | Yes | No | No |
| [Bidirectional multimodal streaming](https://firebase.google.com/docs/ai-logic/live-api) | No | No | No | No | No | No |
| **Supported tools** |||||||
| [Function calling](https://firebase.google.com/docs/ai-logic/function-calling) | Yes | No | Yes | Yes | No | No |
| [Code execution](https://firebase.google.com/docs/ai-logic/code-execution) | Yes | No | Yes | No | No | No |
| [URL context](https://firebase.google.com/docs/ai-logic/url-context) | Yes | No | No | No | No | No |
| [Grounding with Google Search](https://firebase.google.com/docs/ai-logic/grounding-google-search) | Yes | No | Yes | No | No | No |
| [Grounding with Google Maps](https://firebase.google.com/docs/ai-logic/grounding-google-maps) | Yes | No | Yes | No | No | No |
| [System instructions](https://firebase.google.com/docs/ai-logic/system-instructions) | Yes | Yes | Yes | Yes | No | No |
| [Count tokens](https://firebase.google.com/docs/ai-logic/count-tokens) | Yes | Yes | Yes | Yes | No | No |

<br />

<br />

<br />

Specifications and limitations of previous generation models

<br />

These are the specifications and limitations
*when using each model with Firebase AI Logic*:

| Property | Gemini 2.5 Pro, Flash, Flash‑Lite | Gemini 2.5 Flash Image | Gemini 2.0 Flash | Gemini 2.0 Flash‑Lite | Imagen (generate) | Imagen (capability) |
|---|---|---|---|---|---|---|
| Input token limit **\*** | 1,048,576 tokens | 32,768 tokens | 1,048,576 tokens | 1,048,576 tokens | 480 tokens | 480 tokens |
| Output token limit **\*** | 65,536 tokens | 8,192 tokens | 8,192 tokens | 8,192 tokens | --- | --- |
| Knowledge cutoff date | January 2025 | --- | June 2024 | June 2024 | --- | --- |
| **PDFs (per request)** |||||||
| Max number of input PDF files **\*\*** | 3,000 files | 3 files | 3,000 files | 3,000 files | --- | --- |
| Max number of pages per input PDF file **\*\*** | 1,000 pages | 3 pages | 1,000 pages | 1,000 pages | --- | --- |
| Max size per input PDF file | 50 MB | 50 MB | 50 MB | 50 MB | --- | --- |
| **Images (per request)** |||||||
| Max number of *input* images | 3,000 images | 3 images | 3,000 images | 3,000 images | --- | 4 images |
| Max number of *output* images | --- | Up to output token limit | --- | --- | 4 images | 4 images |
| Max size per input base64-encoded image | 7 MB | 7 MB | 7 MB | 7 MB | --- | --- |
| **Video (per request)** |||||||
| Max number of input video files | 10 files | --- | 10 files | 10 files | --- | --- |
| Max length of all input video (frames only) | \~60 minutes | --- | \~60 minutes | \~60 minutes | --- | --- |
| Max length of all input video (frames+audio) | \~45 minutes | --- | \~45 minutes | \~45 minutes | --- | --- |
| **Audio (per request)** |||||||
| Max number of *input* audio files | 1 file | --- | 1 file | 1 file | --- | --- |
| Max number of *output* audio files | --- | --- | --- | --- | --- | --- |
| Max length of all *input* audio | \~8.4 hours | --- | \~8.4 hours | \~8.4 hours | --- | --- |
| Max length of all *output* audio | --- | --- | --- | --- | --- | --- |


^\*
*For all Gemini models, a token is equivalent to about 4 characters,
so 100 tokens are about 60-80 English words. For Gemini models, you can
determine the total count of tokens in your requests using
[`countTokens`](https://firebase.google.com/docs/ai-logic/count-tokens).*^


^\*\*
*PDFs are treated as images, so a single page of a PDF is treated as
one image. The number of pages allowed in a request is limited to the number
of images the model can support.*^

<br />

<br />

<br />

Available model names of previous generation models (including shutdown dates)

<br />

Model names are the explicit values that you include *in your code* during
initialization of the model.

### Gemini models

#### Gemini 2.5 Pro model names

> [!WARNING]
> **Warning:** Gemini 2.5 Pro (`gemini-2.5-pro`) is limited to projects that actively used it in the past. For the Gemini Developer API, the model is deprecated but no shutdown date is announced yet (only previously active projects can continue using it). For the Agent Platform Gemini API (formerly Vertex AI), the model will shut down on October 20, 2026.

^*Does **not** require the pay-as-you-go Blaze pricing plan if you're
using the Gemini Developer API.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-2.5-pro` | Stable version of Gemini 2.5 Pro | Stable | 2025-06-17 | Developer API: To be determined Agent Platform API: 2026-10-20 |

#### Gemini 2.5 Flash model names

> [!WARNING]
> **Warning:** Gemini 2.5 Flash (`gemini-2.5-flash`) is limited to projects that actively used it in the past. For the Gemini Developer API, the model is deprecated but no shutdown date is announced yet (only previously active projects can continue using it). For the Agent Platform Gemini API (formerly Vertex AI), the model will shut down on October 20, 2026.

^*Does **not** require the pay-as-you-go Blaze pricing plan if you're
using the Gemini Developer API.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-2.5-flash` | Stable version of Gemini 2.5 Flash | Stable | 2025-06-17 | Developer API: To be determined Agent Platform API: 2026-10-20 |

#### Gemini 2.5 Flash‑Lite model names

> [!WARNING]
> **Warning:** Gemini 2.5 Flash‑Lite (`gemini-2.5-flash-lite`) is limited to projects that actively used it in the past. For the Gemini Developer API, the model is deprecated but no shutdown date is announced yet (only previously active projects can continue using it). For the Agent Platform Gemini API (formerly Vertex AI), the model will shut down on October 20, 2026.

^*Does **not** require the pay-as-you-go Blaze pricing plan if you're
using the Gemini Developer API.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-2.5-flash-lite` | Stable version of Gemini 2.5 Flash‑Lite | Stable | 2025-07-22 | Developer API: To be determined Agent Platform API: 2026-10-20 |

#### Gemini 2.5 Flash Image model names (aka "Nano Banana")

> [!WARNING]
> **Warning:** Gemini 2.5 Flash Image (`gemini-2.5-flash-image`) will shut down on October 02, 2026.

^*Requires the [pay-as-you-go Blaze pricing plan](https://firebase.google.com/pricing) regardless of your Gemini API
provider.*^

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-2.5-flash-image` | Stable version of Gemini 2.5 Flash Image (aka "Nano Banana") | Stable | 2025-10-02 | 2026-10-02 |

#### Gemini 2.0 Flash model names

> [!WARNING]
>
> Gemini 2.0 Flash
> shut down on June 1, 2026. To avoid service disruption, update to a newer
> model like `gemini-3.1-flash-lite`.
> [Learn more.](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting#discontinued-models)

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-2.0-flash-001` | Latest stable version of Gemini 2.0 Flash | Stable | 2025-02-05 | 2026-06-01 |
| `gemini-2.0-flash` | Auto-updated alias pointing to the *latest stable* version of Gemini 2.0 Flash (currently `gemini-2.0-flash-001`) | Stable | 2025-02-10 | 2026-06-01 |

#### Gemini 2.0 Flash‑Lite model names

> [!WARNING]
>
> Gemini 2.0 Flash‑Lite
> shut down on June 1, 2026. To avoid service disruption, update to a newer
> model like `gemini-3.1-flash-lite`.
> [Learn more.](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting#discontinued-models)

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `gemini-2.0-flash-lite-001` | Latest stable version of Gemini 2.0 Flash‑Lite | Stable | 2025-02-25 | 2026-06-01 |
| `gemini-2.0-flash-lite` | Auto-updated alias pointing to the *latest stable* version of Gemini 2.0 Flash‑Lite (currently `gemini-2.0-flash-lite-001`) | Stable | 2025-02-25 | 2026-06-01 |

### Imagen models

> [!WARNING]
> **All Imagen models shut down on
> August 17, 2026.** As a replacement, you can [migrate your apps to use
> Gemini Image models (the "Nano Banana" models)](https://firebase.google.com/docs/ai-logic/imagen-models-migration).

#### Imagen 4 model names

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `imagen-4.0-generate-001` | Stable version of Imagen 4 | Stable | 2025-08-14 | 2026-06-30 |

#### Imagen 4 Fast model names

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `imagen-4.0-fast-generate-001` | Stable version of Imagen 4 Fast | Stable | 2025-08-14 | 2026-06-30 |

#### Imagen 4 Ultra model names

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `imagen-4.0-ultra-generate-001` | Stable version of Imagen 4 Ultra | Stable | 2025-08-14 | 2026-06-30 |

#### Imagen 3 Capability model names

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `imagen-3.0-capability-001` | Initial stable version of Imagen 3 Capability | Stable | 2024-12-10 | 2026-06-30 |

#### Imagen 3 model names

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `imagen-3.0-generate-002` | Latest stable version of Imagen 3 | Stable | 2025-01-23 | 2026-06-30 |
| `imagen-3.0-generate-001` | Initial stable version of Imagen 3 | Stable | 2024-07-31 | 2026-06-30 |

#### Imagen 3 Fast model names

| **Model name** | **Description** | **Release stage** | **Release date** | **Shutdown date** |
|---|---|---|---|---|
| `imagen-3.0-fast-generate-001` | Initial stable version of Imagen 3 Fast | Stable | 2024-07-31 | 2026-06-30 |

<br />

<br />

<br />

*** ** * ** ***

## Next steps


#### Try out the capabilities of the Gemini API

- Build [multi-turn conversations (chat)](https://firebase.google.com/docs/ai-logic/chat).
- Generate text from [text-only prompts](https://firebase.google.com/docs/ai-logic/generate-text).
- Generate text by prompting with various file types, like [images](https://firebase.google.com/docs/ai-logic/analyze-images), [PDFs](https://firebase.google.com/docs/ai-logic/analyze-documents), [video](https://firebase.google.com/docs/ai-logic/analyze-video), and [audio](https://firebase.google.com/docs/ai-logic/analyze-audio).
- [Generate structured output (like JSON)](https://firebase.google.com/docs/ai-logic/generate-structured-output) from both text and multimodal prompts.
- [Generate and edit images](https://firebase.google.com/docs/ai-logic/generate-images-gemini) from both text and multimodal prompts.
- [Generate speech (both single- and
  multiple-speakers)](https://firebase.google.com/docs/ai-logic/generate-speech) using Gemini text-to-speech (TTS) models.
- [Stream input and output](https://firebase.google.com/docs/ai-logic/live-api) (including audio) using the Gemini Live API.
- Use tools (like [function calling](https://firebase.google.com/docs/ai-logic/function-calling) and Grounding with [Google Search](https://firebase.google.com/docs/ai-logic/grounding-google-search) or [Google Maps](https://firebase.google.com/docs/ai-logic/grounding-google-maps)) to connect a Gemini model to other parts of your app and external systems and information.

<br />
