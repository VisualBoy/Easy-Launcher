## Firebase Javascript SDK - On-Device AI (Hybrid)

Hybrid on-device inference for web apps, where the Firebase Javascript SDK automatically checks for Gemini Nano's availability (after installation) and switches between on-device or cloud-hosted prompt execution. This requires specific steps to enable model usage in the Chrome browser, more info in the hybrid-on-device-inference documentation.

[SKILL firebase-ai-logic-basics](https://github.com/firebase/agent-skills/raw/refs/heads/main/skills/firebase-ai-logic-basics/SKILL.md)

---

> [!WARNING]
> **Preview** : Using the Firebase AI Logic SDKs to build hybrid experiences is a Preview feature, which means that it isn't subject to any SLA or deprecation policy and could change in backwards-incompatible ways.
>
> This initial release only **supports on-device inference for
> web apps running on Chrome on Desktop.**

<br />

[Video](https://www.youtube.com/watch?v=wBfqpPxUwqM)

Build AI-powered web apps and features with hybrid inference using
Firebase AI Logic. Hybrid inference enables running inference using
on-device models when available and seamlessly falling back to cloud-hosted
models otherwise (and vice versa).

This page describes how to
[get started using the client SDK](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#get-started), as well as showing
[additional configuration options and capabilities](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#what-else-to-do), like
structured output.

Note that on-device inference is supported for
*web apps running on Chrome on Desktop*.

[Jump to the code examples](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#call-api)

## Recommended use cases and supported capabilities

**Recommended use cases:**

- Using an on-device model for inference offers:

  - Enhanced privacy
  - Local context
  - Inference at no-cost
  - Offline functionality
- Using hybrid functionality offers:

  - Reach 100% of your audience, regardless of on-device model availability or internet connectivity

**Supported capabilities and features for on-device inference:**

- Single-turn content generation, streaming and non-streaming
- Generating [text from text-only input](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#text-in-text-out)
- Generating [text from text-and-image input](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#image-in-text-out), specifically input image types of JPEG and PNG
- Generating [structured output](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#model-config-structured-output), including JSON and enums

## Before you begin

Take note of the following:

- Inference using an on-device model uses the
  [Prompt API from Chrome](https://developer.chrome.com/docs/extensions/ai/prompt-api);
  whereas inference using a cloud-hosted model uses your chosen Gemini API
  provider (either the Gemini Developer API or the
  Vertex AI Gemini API).

- This page describes how to **get started developing using localhost** (learn
  more about
  [using APIs on localhost](https://developer.chrome.com/docs/ai/get-started#use_apis_on_localhost)
  in the Chrome documentation). Also, check out the
  [additional configuration options and capabilities](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#what-else-to-do) (like
  structured output) later on this page.

- After you've implemented your feature, you can
  [**enable end-users to try your feature**](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#enable-end-users-to-try-feature)
  in your actual app.

## Get started on localhost

These get started steps describe the required general setup for any supported
prompt request that you want to send.

### **Step 1**: Set up Chrome and the Prompt API for on-device inference

1. Make sure you're using a recent version of Chrome. Update in
   <chrome://settings/help>.  

   On-device inference is available from Chrome v139 and higher.

2. Enable the on-device multimodal model by setting the following flag to
   **Enabled**:

   - `chrome://flags/#prompt-api-for-gemini-nano-multimodal-input`
3. Restart Chrome.

4. *(Optional)* Download the on-device model before the first request.

   The Prompt API is built into Chrome; however, the on-device model isn't
   available by default. If you haven't yet downloaded the model before your
   first request for on-device inference, the request will automatically start
   the model download in the background.

   > [!NOTE]
   > **Note:** Downloading the model can take several minutes, so waiting to auto-download with the first request can significantly delay receiving a response to that request.

   <br />

   View instructions to download the on-device model

   <br />

   1. Open **Developer Tools \> Console**.

   2. Run the following:

          await LanguageModel.availability();

   3. Make sure that the output is `available`, `downloading`, or
      `downloadable`.

   4. If the output is `downloadable`, start the model download by running:

          await LanguageModel.create();

   5. You can use the following `monitor` callback to listen for download
      progress and make sure that the model is `available` before making
      requests:

          const session = await LanguageModel.create({
            monitor(m) {
              m.addEventListener("downloadprogress", (e) => {
                console.log(`Downloaded ${e.loaded * 100}%`);
              });
            },
          });

   <br />

   <br />

### **Step 2**: Set up a Firebase project and connect your app to Firebase

1. Sign into the [Firebase console](https://console.firebase.google.com/),
   and then select your Firebase project.

   <br />

   Don't already have a Firebase project?

   <br />

   If you don't already have a Firebase project, click the button to create a
   new Firebase project, and then use either of the following options:
   - **Option 1** : Create a wholly new Firebase project (and its underlying
     Google Cloud project automatically) by entering a new project name in the
     first step of the workflow.

   - **Option 2** : "Add Firebase" to an existing Google Cloud project by
     clicking **Add Firebase to Google Cloud project** (at bottom of page).
     In the first step of the workflow, start entering the **project name** of
     the existing project, and then select the project from the displayed list.

   Complete the remaining steps of the on-screen workflow to create a Firebase
   project. Note that when prompted, you do ***not*** need to set up
   Google Analytics to use the Firebase AI Logic SDKs.

   <br />

   <br />

2. In the Firebase console, go to the [**Firebase AI Logic** page](https://console.firebase.google.com/project/_/ailogic).

3. Click **Get started** to launch a guided workflow that helps you set up the
   [required APIs](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting#required-apis)
   and resources for your project.

4. Set up your project to use a "Gemini API" provider.

   **We recommend getting started using the Gemini Developer API.**
   At any point, you can always
   [set up the Vertex AI Gemini API](https://console.firebase.google.com/project/_/ailogic?openVertexAiOnboarding=true)
   (and its requirement for billing).

   For the Gemini Developer API, the console will enable the required
   APIs and create a Gemini API key in your project.  

   *Do **not** add this Gemini API key into your app's codebase.*
   [Learn more.](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting#add-gemini-api-key-to-codebase)
5. If prompted in the console's workflow, follow the on-screen instructions to
   register your app and connect it to Firebase.

6. Continue to the next step in this guide to add the SDK to your app.

> [!NOTE]
> **Note:** In the Firebase console, you're strongly encouraged to [set up Firebase App Check](https://firebase.google.com/docs/ai-logic/app-check). If you're just trying out the Gemini API, you don't need to set up App Check right away; however, we recommend setting it up as soon as you start seriously developing your app.

### **Step 3**: Add the SDK

The Firebase library provides access to the APIs for interacting with generative
models. The library is included as part of the Firebase JavaScript SDK for Web.

1. Install the Firebase JS SDK for Web using npm:

       npm install firebase

2. Initialize Firebase in your app:

       import { initializeApp } from "firebase/app";

       // TODO(developer) Replace the following with your app's Firebase configuration
       // See: https://firebase.google.com/docs/web/learn-more#config-object
       const firebaseConfig = {
         // ...
       };

       // Initialize FirebaseApp
       const firebaseApp = initializeApp(firebaseConfig);

### **Step 4**: Initialize the service and create a model instance

|---|
| *Click your Gemini API provider to view provider-specific content and code on this page.* <button value="dev" default="">Gemini Developer API</button> <button value="vertex">Vertex AI Gemini API</button> |

Set up the following before you send a prompt request to the model.

1. Initialize the service for your chosen API provider.

2. Create a `GenerativeModel` instance. Make sure to do the following:

   1. Call `getGenerativeModel` *after or on an end-user interaction* (like a
      button click). This is a prerequisite for `inferenceMode`.

   2. Set the `mode` to one of:

      - **`PREFER_ON_DEVICE`** : Use the on-device model if it's available;
        otherwise, *fall back to the cloud-hosted model*.

      - **`ONLY_ON_DEVICE`** : Use the on-device model if it's available;
        otherwise, *throw an exception*.

      - **`PREFER_IN_CLOUD`** : Use the cloud-hosted model if it's available;
        otherwise, *fall back to the on-device model*.

      - **`ONLY_IN_CLOUD`** : Use the cloud-hosted model if it's available;
        otherwise, *throw an exception*.

      > [!NOTE]
      > **Note:** For requests sent to the on-device model, make sure it's [supported by on-device inference](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#features-not-yet-available).   
      > For requests sent off-device, the device must be online. Also, the **default cloud-hosted model is
      > `gemini-2.5-flash-lite`** , but you can [override the default](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#override-default-cloud-model).

    import { initializeApp } from "firebase/app";
    import { getAI, getGenerativeModel, GoogleAIBackend, InferenceMode } from "firebase/ai";

    // TODO(developer) Replace the following with your app's Firebase configuration
    // See: https://firebase.google.com/docs/web/learn-more#config-object
    const firebaseConfig = {
      // ...
    };

    // Initialize FirebaseApp
    const firebaseApp = initializeApp(firebaseConfig);

    // Initialize the Gemini Developer API backend service
    const ai = getAI(firebaseApp, { backend: new GoogleAIBackend() });
    // Create a \`GenerativeModel\` instance
    // Call \`getGenerativeModel\` after or on an end-user interaction
    // Set the mode (for example, use the on-device model if it's available)
    const model = getGenerativeModel(ai, { mode: InferenceMode.PREFER_ON_DEVICE });

> [!NOTE]
> **Note:** **Downloading the on-device model can take several minutes.**   
> If you haven't yet [downloaded the model before your first request for on-device inference](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#instructions-to-download-on-device-model), the request will automatically start the model download in the background (which can significantly delay receiving a response to that request).

### **Step 5**: Send a prompt request to a model

This section shows you how to send various types of input to generate different
types of output, including:

- [Generate text from text-only input](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#text-in-text-out)
- [Generate text from text-and-image (multimodal) input](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#image-in-text-out)

If you want to generate structured output (like JSON or enums), then
use one of the following "generate text" examples and additionally
[configure the model to respond according to a provided schema](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#model-config-structured-output).

#### Generate text from text-only input

|---|
| *Before trying this sample, make sure that you've completed the [Get started](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#get-started) section of this guide.* |

You can use
[`generateContent()`](https://firebase.google.com/docs/reference/js/vertexai.generativemodel#generativemodelgeneratecontent)
to generate text from a prompt that contains text:

    // Imports + initialization of FirebaseApp and backend service + creation of model instance

    // Wrap in an async function so you can use await
    async function run() {
      // Provide a prompt that contains text
      const prompt = "Write a story about a magic backpack."

      // To generate text output, call `generateContent` with the text input
      const result = await model.generateContent(prompt);

      const response = result.response;
      const text = response.text();
      console.log(text);
    }

    run();

Note that Firebase AI Logic also supports streaming of text responses using
`generateContentStream` (instead of `generateContent`).

#### Generate text from text-and-image (multimodal) input

|---|
| *Before trying this sample, make sure that you've completed the [Get started](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#get-started) section of this guide.* |

You can use [`generateContent()`](https://firebase.google.com/docs/reference/js/vertexai.generativemodel#generativemodelgeneratecontent)
to generate text from a prompt that contains text and image files---providing each
input file's `mimeType` and the file itself.

The supported input image types for on-device inference are PNG and JPEG.

> [!IMPORTANT]
> **Important:** For on-device inference, the maximum token limit is 6000 tokens.

    // Imports + initialization of FirebaseApp and backend service + creation of model instance

    // Converts a File object to a Part object.
    async function fileToGenerativePart(file) {
      const base64EncodedDataPromise = new Promise((resolve) => {
        const reader = new FileReader();
        reader.onloadend = () => resolve(reader.result.split(',')[1]);
        reader.readAsDataURL(file);
      });
      return {
        inlineData: { data: await base64EncodedDataPromise, mimeType: file.type },
      };
    }

    async function run() {
      // Provide a text prompt to include with the image
      const prompt = "Write a poem about this picture:";

      const fileInputEl = document.querySelector("input[type=file]");
      const imagePart = await fileToGenerativePart(fileInputEl.files[0]);

      // To generate text output, call `generateContent` with the text and image
      const result = await model.generateContent([prompt, imagePart]);

      const response = result.response;
      const text = response.text();
      console.log(text);
    }

    run();

Note that Firebase AI Logic also supports streaming of text responses using
`generateContentStream` (instead of `generateContent`).

## Enable end-users to try your feature

For end-users to try your feature in your app, you must
[enroll in the Chrome Origin Trials](https://developer.chrome.com/docs/web-platform/origin-trials).
Note that there's a limited duration and usage for these trials.

1. Register for the
   [Prompt API Chrome Origin Trial](https://developer.chrome.com/origintrials/#/view_trial/2533837740349325313).
   You'll be given a token.

2. Provide the token on every web page for which you want the trial feature to
   be enabled. Use one of the following options:

   - Provide the token as a meta tag in the `<head>` tag:
     `<meta http-equiv="origin-trial" content="TOKEN">`

   - Provide the token as an HTTP header:
     `Origin-Trial: TOKEN`

   - Provide the token
     [programmatically](https://developer.chrome.com/docs/web-platform/origin-trials#programmatic).

## What else can you do?

In this section, learn how to
[set an inference mode](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#inference-modes),
[override the default cloud-hosted fallback model](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#override-default-cloud-model),
and
[use model configuration to control responses](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#model-config) (like generate
structured output).

### Set an inference mode

The examples in the general setup use the `PREFER_ON_DEVICE` mode, but this is
only one of the four available
[inference modes](https://firebase.google.com/docs/reference/js/ai.hybridparams#hybridparamsmode).

> [!NOTE]
> **Note:** For requests sent to the on-device model, make sure it's [supported by on-device inference](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#features-not-yet-available).   
> For requests sent off-device, the device must be online. Also, the **default cloud-hosted model is
> `gemini-2.5-flash-lite`** , but you can [override the default](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#override-default-cloud-model).

- **`PREFER_ON_DEVICE`** : Use the on-device model if it's available;
  otherwise, *fall back to the cloud-hosted model*.

      const model = getGenerativeModel(ai, { mode: InferenceMode.PREFER_ON_DEVICE });

- **`ONLY_ON_DEVICE`** : Use the on-device model if it's available;
  otherwise, *throw an exception*.

      const model = getGenerativeModel(ai, { mode: InferenceMode.ONLY_ON_DEVICE });

- **`PREFER_IN_CLOUD`** : Use the cloud-hosted model if it's available;
  otherwise, *fall back to the on-device model*.

      const model = getGenerativeModel(ai, { mode: InferenceMode.PREFER_IN_CLOUD });

- **`ONLY_IN_CLOUD`** : Use the cloud-hosted model if it's available;
  otherwise, *throw an exception*.

      const model = getGenerativeModel(ai, { mode: InferenceMode.ONLY_IN_CLOUD });

> [!NOTE]
> **Note:** **Downloading the on-device model can take several minutes.**   
> If you haven't yet [downloaded the model before your first request for on-device inference](https://firebase.google.com/docs/ai-logic/hybrid-on-device-inference#instructions-to-download-on-device-model), the request will automatically start the model download in the background (which can significantly delay receiving a response to that request).

#### Determine whether on-device or in-cloud inference was used

If you use `PREFER_ON_DEVICE` or `PREFER_IN_CLOUD` inference modes, then it
might be helpful to know which mode was used for given requests. This
information is provided by the `inferenceSource` property of each response
(available starting with JS SDK v12.5.0).

When you access this property, the returned value will be either
`ON_DEVICE` or `IN_CLOUD`.

    // ...

    console.log('You used: ' + result.response.inferenceSource);

    console.log(result.response.text());

### Override the default fallback model

**The default cloud-hosted model is
`gemini-2.5-flash-lite`**
(starting with JS SDK v12.8.0).

This model is the fallback cloud-hosted model when you use the
`PREFER_ON_DEVICE` mode. It's also the default model when you use the
`ONLY_IN_CLOUD` mode or the `PREFER_IN_CLOUD` mode.

You can use the
[`inCloudParams`](https://firebase.google.com/docs/reference/js/ai.hybridparams#hybridparamsincloudparams)
configuration option to specify an alternative default cloud-hosted model.

    const model = getGenerativeModel(ai, {
      mode: InferenceMode.INFERENCE_MODE,
      inCloudParams: {
        model: "GEMINI_MODEL_NAME"
      }
    });

Find model names for all
[supported Gemini models](https://firebase.google.com/docs/ai-logic/models).

### Use model configuration to control responses

In each request to a model, you can send along a model configuration to control
how the model generates a response. Cloud-hosted models and on-device models
offer different configuration options.

The configuration is maintained for the lifetime of the instance. If you want to
use a different config, create a new `GenerativeModel` instance with that
config.

#### Set the configuration for a cloud-hosted model

Use the
[`inCloudParams`](https://firebase.google.com/docs/reference/js/ai.hybridparams#hybridparamsincloudparams)
option to configure a cloud-hosted Gemini model. Learn about
[available parameters](https://firebase.google.com/docs/ai-logic/model-parameters#parameters-descriptions-gemini).

    const model = getGenerativeModel(ai, {
      mode: InferenceMode.INFERENCE_MODE,
      inCloudParams: {
        model: "GEMINI_MODEL_NAME"
        temperature: 0.8,
        topK: 10
      }
    });

#### Set the configuration for an on-device model

Note that inference using an on-device model uses the
[Prompt API from Chrome](https://developer.chrome.com/docs/extensions/ai/prompt-api).

Use the
[`onDeviceParams`](https://firebase.google.com/docs/reference/js/ai.hybridparams#hybridparamsondeviceparams)
option to configure an on-device model. Learn about
[available parameters](https://github.com/webmachinelearning/prompt-api?tab=readme-ov-file#configuration-of-per-session-parameters).

    const model = getGenerativeModel(ai, {
      mode: InferenceMode.INFERENCE_MODE,
      onDeviceParams: {
        createOptions: {
          temperature: 0.8,
          topK: 8
        }
      }
    });

#### Set the configuration for structured output (like JSON)

Generating structured output (like JSON and enums) is supported for
inference using both cloud-hosted and on-device models.

For hybrid inference, use both
[`inCloudParams`](https://firebase.google.com/docs/reference/js/ai.hybridparams#hybridparamsincloudparams)
and
[`onDeviceParams`](https://firebase.google.com/docs/reference/js/ai.hybridparams#hybridparamsondeviceparams)
to configure the model to respond with structured output. For the other modes,
use only the applicable configuration.

- **For `inCloudParams`** : Specify the appropriate `responseMimeType` (in
  this example, `application/json`) as well as the `responseSchema` that you
  want the model to use.

- **For `onDeviceParams`** : Specify the `responseConstraint` that you
  want the model to use.

##### JSON output

The following example adapts the
[general JSON output example](https://firebase.google.com/docs/ai-logic/generate-structured-output#generate-json-basic)
for hybrid inference:

    import {
      getAI,
      getGenerativeModel,
      Schema
    } from "firebase/ai";

    const jsonSchema = Schema.object({
     properties: {
        characters: Schema.array({
          items: Schema.object({
            properties: {
              name: Schema.string(),
              accessory: Schema.string(),
              age: Schema.number(),
              species: Schema.string(),
            },
            optionalProperties: ["accessory"],
          }),
        }),
      }
    });

    const model = getGenerativeModel(ai, {
      mode: InferenceMode.INFERENCE_MODE,
      inCloudParams: {
        model: "gemini-2.5-flash"
        generationConfig: {
          responseMimeType: "application/json",
          responseSchema: jsonSchema
        },
      }
      onDeviceParams: {
        promptOptions: {
          responseConstraint: jsonSchema
        }
      }
    });

##### Enum output

As above, but adapting the
[documentation on enum output](https://firebase.google.com/docs/ai-logic/generate-structured-output#generate-enum-basic)
for hybrid inference:

    // ...

    const enumSchema = Schema.enumString({
      enum: ["drama", "comedy", "documentary"],
    });

    const model = getGenerativeModel(ai, {

    // ...

        generationConfig: {
          responseMimeType: "text/x.enum",
          responseSchema: enumSchema
        },

    // ...
    });

    // ...

## Features not yet available for on-device inference

As an experimental release, not all the capabilities of the Web SDK are
available for *on-device* inference. **The following features are *not yet
supported for on-device inference* (but they are usually available for
cloud-based inference).**

> [!NOTE]
> **Note:** For many of these features, if you set the mode to `PREFER_ON_DEVICE`, the SDK will just automatically fall back to use the cloud-hosted model for these not-yet-available capabilities.

- Generating text from image file input types other than JPEG and PNG

  - Can fallback to the cloud-hosted model; however, `ONLY_ON_DEVICE` mode will throw an error.
- Generating text from audio, video, and documents (like PDFs) inputs

  - Can fallback to the cloud-hosted model; however, `ONLY_ON_DEVICE` mode will throw an error.
- Generating images using Gemini or Imagen models

  - Can fallback to the cloud-hosted model; however, `ONLY_ON_DEVICE` mode will throw an error.
- Providing files using URLs in multimodal requests. You must provide files as
  inline data to on-device models.

- Multi-turn chat

  - Can fallback to the cloud-hosted model; however, `ONLY_ON_DEVICE` mode will throw an error.
- Bi-directional streaming with the Gemini Live API

- Providing the model with *tools* to help it generate its response
  (like function calling, code execution, URL context, and
  grounding with Google Search)

- Count tokens

  - Always throws an error. The count will differ between cloud-hosted and on-device models, so there is no intuitive fallback.
- AI monitoring in the Firebase console for on-device inference.

  - Note that any inference using the cloud-hosted models can be monitored just like other inference using the Firebase AI Logic client SDK for Web.

<br />

[Give feedback
about your experience with Firebase AI Logic](https://firebase.google.com/docs/ai-logic/feedback)

<br />
