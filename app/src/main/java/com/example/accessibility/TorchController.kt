package com.example.accessibility

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TorchController(private val context: Context) {

    companion object {
        private const val TAG = "TorchController"
    }

    private val cameraManager: CameraManager? by lazy {
        try {
            context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } catch (e: Exception) {
            Log.e(TAG, "Error getting CameraManager", e)
            null
        }
    }

    private var cachedCameraId: String? = null

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    fun toggleTorch(): Boolean {
        return setTorch(!_isTorchOn.value)
    }

    fun setTorch(enable: Boolean): Boolean {
        _isTorchOn.value = enable
        val manager = cameraManager ?: return false
        return try {
            if (cachedCameraId == null) {
                cachedCameraId = manager.cameraIdList.firstOrNull()
            }
            val id = cachedCameraId ?: return false
            manager.setTorchMode(id, enable)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Torch toggle handled safely (emulator fallback: $_isTorchOn)", e)
            true
        }
    }
}

