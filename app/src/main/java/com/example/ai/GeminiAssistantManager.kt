package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAssistantManager(
    private val actionExecutor: NativeActionExecutor
) {
    companion object {
        private const val TAG = "GeminiAssistant"
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Builds the JSON tool declaration schema for Gemini Function Calling.
     */
    private fun buildToolsDeclaration(): JSONArray {
        val toolsArray = JSONArray()
        val toolObject = JSONObject()
        val declarations = JSONArray()

        // 1. send_sms
        declarations.put(JSONObject().apply {
            put("name", "send_sms")
            put("description", "Invia un SMS a un numero di telefono o contatto specificato.")
            val params = JSONObject()
            params.put("type", "OBJECT")
            val props = JSONObject()
            props.put("phoneNumber", JSONObject().apply {
                put("type", "STRING")
                put("description", "Numero di telefono o nome del destinatario")
            })
            props.put("message", JSONObject().apply {
                put("type", "STRING")
                put("description", "Testo del messaggio SMS da inviare")
            })
            params.put("properties", props)
            params.put("required", JSONArray().apply { put("phoneNumber"); put("message") })
            put("parameters", params)
        })

        // 2. send_whatsapp
        declarations.put(JSONObject().apply {
            put("name", "send_whatsapp")
            put("description", "Invia o risponde a un messaggio WhatsApp per un contatto o chat.")
            val params = JSONObject()
            params.put("type", "OBJECT")
            val props = JSONObject()
            props.put("recipient", JSONObject().apply {
                put("type", "STRING")
                put("description", "Nome del contatto WhatsApp o numero di telefono")
            })
            props.put("message", JSONObject().apply {
                put("type", "STRING")
                put("description", "Testo del messaggio WhatsApp")
            })
            params.put("properties", props)
            params.put("required", JSONArray().apply { put("recipient"); put("message") })
            put("parameters", params)
        })

        // 3. trigger_emergency_sos
        declarations.put(JSONObject().apply {
            put("name", "trigger_emergency_sos")
            put("description", "Attiva immediatamente la procedura di emergenza SOS con allarme sonoro, invio coordinate GPS via SMS e chiamata di soccorso.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 4. toggle_torch
        declarations.put(JSONObject().apply {
            put("name", "toggle_torch")
            put("description", "Accende o spegne la torcia o flash del dispositivo.")
            val params = JSONObject()
            params.put("type", "OBJECT")
            val props = JSONObject()
            props.put("enable", JSONObject().apply {
                put("type", "BOOLEAN")
                put("description", "True per accendere, False per spegnere la torcia")
            })
            params.put("properties", props)
            put("parameters", params)
        })

        // 5. make_phone_call
        declarations.put(JSONObject().apply {
            put("name", "make_phone_call")
            put("description", "Avvia una chiamata vocale telefonica verso un contatto o numero.")
            val params = JSONObject()
            params.put("type", "OBJECT")
            val props = JSONObject()
            props.put("recipient", JSONObject().apply {
                put("type", "STRING")
                put("description", "Nome del contatto o numero da chiamare")
            })
            params.put("properties", props)
            params.put("required", JSONArray().apply { put("recipient") })
            put("parameters", params)
        })

        // 6. read_notifications
        declarations.put(JSONObject().apply {
            put("name", "read_notifications")
            put("description", "Legge a voce le ultime notifiche, messaggi WhatsApp o chiamate perse ricevute.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 7. open_app
        declarations.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Apre un'applicazione installata sul telefono (es. WhatsApp, Fotocamera, Galleria, Impostazioni).")
            val params = JSONObject()
            params.put("type", "OBJECT")
            val props = JSONObject()
            props.put("appName", JSONObject().apply {
                put("type", "STRING")
                put("description", "Nome dell'applicazione da aprire")
            })
            params.put("properties", props)
            params.put("required", JSONArray().apply { put("appName") })
            put("parameters", params)
        })

        // 8. get_device_status
        declarations.put(JSONObject().apply {
            put("name", "get_device_status")
            put("description", "Restituisce lo stato attuale del telefono: ora, data, livello batteria e stato torcia.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        toolObject.put("functionDeclarations", declarations)
        toolsArray.put(toolObject)
        return toolsArray
    }

    /**
     * Processes user speech intent with multi-turn Gemini Function Calling and fallback offline heuristics.
     */
    suspend fun processIntent(
        userSpeechInput: String,
        onToolExecuted: ((toolName: String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val cleanInput = userSpeechInput.trim()
        if (cleanInput.isEmpty()) {
            return@withContext "Non ho sentito niente. Tocca il microfono e dimmi come aiutarti."
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!hasValidKey) {
            Log.d(TAG, "No valid Gemini API key found. Using smart local rule-based intent executor.")
            return@withContext processLocalIntent(cleanInput, onToolExecuted)
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val contentsArray = JSONArray()

            // Initial user message
            val userContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()
                parts.put(JSONObject().apply {
                    put("text", cleanInput)
                })
                put("parts", parts)
            }
            contentsArray.put(userContent)

            val systemInstruction = JSONObject().apply {
                val parts = JSONArray()
                parts.put(JSONObject().apply {
                    put("text", "Sei l'assistente vocale accessibile di Easy Launcher per persone anziane o con difficoltà motorie e visive. " +
                            "Parla sempre in italiano chiaro, gentile, rassicurante e conciso. " +
                            "Se l'utente chiede un'azione (inviare SMS, mandare messaggi WhatsApp, chiamare, accendere la luce/torcia, attivare SOS, leggere notifiche, aprire app, sapere l'ora o la batteria), invoca SEMPRE il Function Calling relativo. " +
                            "Dopo l'esecuzione del tool, rispondi con una breve frase di conferma audio per la sintesi vocale.")
                })
                put("parts", parts)
            }

            var loopCount = 0
            var finalResponseText = ""

            while (loopCount < 4) {
                loopCount++

                val requestPayload = JSONObject().apply {
                    put("contents", contentsArray)
                    put("tools", buildToolsDeclaration())
                    put("systemInstruction", systemInstruction)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestPayload.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBodyString = response.body?.string()

                if (!response.isSuccessful || responseBodyString == null) {
                    Log.w(TAG, "Gemini API error code: ${response.code}. Falling back to local intent parser.")
                    return@withContext processLocalIntent(cleanInput, onToolExecuted)
                }

                val jsonResponse = JSONObject(responseBodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")

                if (parts == null || parts.length() == 0) {
                    return@withContext "Non sono riuscito a elaborare la richiesta."
                }

                // Append model's response to conversation history
                val modelContent = JSONObject().apply {
                    put("role", "model")
                    put("parts", parts)
                }
                contentsArray.put(modelContent)

                // Check for function calls
                var foundFunctionCall = false
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val functionCall = part.optJSONObject("functionCall")

                    if (functionCall != null) {
                        foundFunctionCall = true
                        val functionName = functionCall.optString("name")
                        val args = functionCall.optJSONObject("args") ?: JSONObject()

                        Log.d(TAG, "Executing function call: $functionName with args: $args")
                        onToolExecuted?.invoke(functionName)

                        val executionResultJson = executeNativeTool(functionName, args)

                        // Send FunctionResponse back to model
                        val functionResponseContent = JSONObject().apply {
                            put("role", "user")
                            val respParts = JSONArray()
                            val respPart = JSONObject().apply {
                                val funcResp = JSONObject().apply {
                                    put("name", functionName)
                                    put("response", executionResultJson)
                                }
                                put("functionResponse", funcResp)
                            }
                            respParts.put(respPart)
                            put("parts", respParts)
                        }
                        contentsArray.put(functionResponseContent)
                        break
                    } else {
                        val text = part.optString("text", "")
                        if (text.isNotEmpty()) {
                            finalResponseText += text
                        }
                    }
                }

                if (!foundFunctionCall) {
                    // Model delivered final answer
                    break
                }
            }

            return@withContext if (finalResponseText.isNotBlank()) finalResponseText else "Operazione completata con successo."
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed with exception, falling back to local intent parser", e)
            return@withContext processLocalIntent(cleanInput, onToolExecuted)
        }
    }

    private fun executeNativeTool(name: String, args: JSONObject): JSONObject {
        return when (name) {
            "send_sms" -> {
                val phone = args.optString("phoneNumber", "")
                val msg = args.optString("message", "")
                actionExecutor.sendSms(phone, msg)
            }
            "send_whatsapp" -> {
                val recipient = args.optString("recipient", "")
                val msg = args.optString("message", "")
                actionExecutor.sendWhatsAppMessage(recipient, msg)
            }
            "trigger_emergency_sos" -> {
                actionExecutor.triggerEmergencySos()
            }
            "toggle_torch" -> {
                val enable = if (args.has("enable")) args.optBoolean("enable") else null
                actionExecutor.toggleTorch(enable)
            }
            "make_phone_call" -> {
                val recipient = args.optString("recipient", "")
                actionExecutor.makePhoneCall(recipient)
            }
            "read_notifications" -> {
                actionExecutor.readLatestNotifications()
            }
            "open_app" -> {
                val app = args.optString("appName", "")
                actionExecutor.openApp(app)
            }
            "get_device_status" -> {
                actionExecutor.getDeviceStatus()
            }
            else -> JSONObject().apply {
                put("status", "error")
                put("reason", "Funzione non supportata: $name")
            }
        }
    }

    /**
     * Local heuristic intent parser for 100% reliable offline / senior accessibility usage.
     */
    private fun processLocalIntent(
        input: String,
        onToolExecuted: ((toolName: String) -> Unit)?
    ): String {
        val lower = input.lowercase()

        // SOS / Emergency
        if (lower.contains("sos") || lower.contains("aiuto") || lower.contains("soccorso") || lower.contains("emergenza")) {
            onToolExecuted?.invoke("trigger_emergency_sos")
            actionExecutor.triggerEmergencySos()
            return "Avvio procedura di emergenza SOS con invio posizione e chiamata ai tuoi contatti."
        }

        // Flashlight / Torch
        if (lower.contains("torcia") || lower.contains("luce") || lower.contains("lampada")) {
            onToolExecuted?.invoke("toggle_torch")
            val isTurnOn = lower.contains("accendi") || lower.contains("attiva") || !lower.contains("spegni")
            val res = actionExecutor.toggleTorch(isTurnOn)
            val state = res.optString("torchState", if (isTurnOn) "accesa" else "spenta")
            return "Ho ${if (state == "accesa") "acceso" else "spento"} la torcia per te."
        }

        // WhatsApp message
        if (lower.contains("whatsapp") || (lower.contains("messaggio a") && !lower.contains("sms"))) {
            onToolExecuted?.invoke("send_whatsapp")
            val parts = extractRecipientAndMessage(input)
            val res = actionExecutor.sendWhatsAppMessage(parts.first, parts.second)
            return if (res.optString("status") == "success") {
                "Messaggio WhatsApp inviato a ${parts.first}: ${parts.second}"
            } else {
                "Non sono riuscito a inviare il messaggio WhatsApp: ${res.optString("reason")}"
            }
        }

        // SMS message
        if (lower.contains("sms") || lower.contains("invia messaggio")) {
            onToolExecuted?.invoke("send_sms")
            val parts = extractRecipientAndMessage(input)
            val res = actionExecutor.sendSms(parts.first, parts.second)
            return if (res.optString("status") == "success") {
                "SMS inviato con successo a ${parts.first}."
            } else {
                "Invio SMS non riuscito: ${res.optString("reason")}"
            }
        }

        // Phone call
        if (lower.contains("chiama") || lower.contains("telefona") || lower.contains("chiamata")) {
            onToolExecuted?.invoke("make_phone_call")
            val contact = lower.replace("chiama", "")
                .replace("telefona", "")
                .replace("a", "")
                .replace("per favore", "")
                .trim()
            actionExecutor.makePhoneCall(contact)
            return "Sto chiamando $contact."
        }

        // Read notifications
        if (lower.contains("notifiche") || lower.contains("messaggi") || lower.contains("chiamate perse") || lower.contains("leggi")) {
            onToolExecuted?.invoke("read_notifications")
            val res = actionExecutor.readLatestNotifications()
            return res.optString("summary", "Nessuna notifica presente.")
        }

        // Time / Date / Battery
        if (lower.contains("ora") || lower.contains("data") || lower.contains("batteria") || lower.contains("giorno")) {
            onToolExecuted?.invoke("get_device_status")
            val status = actionExecutor.getDeviceStatus()
            val time = status.optString("time")
            val date = status.optString("date")
            val bat = status.optString("batteryLevel")
            return "Sono le $time di $date. La batteria è al $bat."
        }

        // Open App
        if (lower.contains("apri") || lower.contains("avvia")) {
            val app = lower.replace("apri", "").replace("avvia", "").trim()
            onToolExecuted?.invoke("open_app")
            val res = actionExecutor.openApp(app)
            return if (res.optString("status") == "success") {
                "Apro l'applicazione $app."
            } else {
                "Non ho trovato l'applicazione $app."
            }
        }

        return "Ho capito: \"$input\". Dimmi pure se vuoi chiamare qualcuno, inviare un messaggio WhatsApp, accendere la torcia o chiedere aiuto."
    }

    private fun extractRecipientAndMessage(input: String): Pair<String, String> {
        // e.g. "Manda un messaggio a Maria arrivo tra dieci minuti"
        val clean = input.replace(Regex("(?i)invia messaggio a|manda messaggio a|scrivi a|a "), " ")
            .replace(Regex("(?i)messaggio whatsapp a|whatsapp a"), " ")
            .trim()

        val tokens = clean.split(" ")
        if (tokens.isNotEmpty()) {
            val recipient = tokens[0]
            val message = if (tokens.size > 1) tokens.drop(1).joinToString(" ") else "Ciao!"
            return Pair(recipient, message)
        }
        return Pair("Contatto", "Ciao!")
    }
}
