package com.dayone.app.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Thin wrapper around CameraX so the Compose capture screen stays simple: lens switching,
 * pinch zoom, tap-to-focus and torch, with the current state exposed as flows.
 */
class CameraController(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _useFrontCamera = MutableStateFlow(true)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera

    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn

    private val _hasTorch = MutableStateFlow(false)
    val hasTorch: StateFlow<Boolean> = _hasTorch

    private val _maxZoom = MutableStateFlow(1f)
    val maxZoom: StateFlow<Float> = _maxZoom

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        front: Boolean = _useFrontCamera.value,
        onReady: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        _useFrontCamera.value = front
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                imageCapture = capture

                val preferred = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val selector = when {
                    provider.hasCamera(preferred) -> preferred
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> CameraSelector.DEFAULT_FRONT_CAMERA
                }

                provider.unbindAll()
                val bound = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                camera = bound

                _hasTorch.value = bound.cameraInfo.hasFlashUnit()
                _torchOn.value = false
                _maxZoom.value = bound.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                _zoomRatio.value = bound.cameraInfo.zoomState.value?.zoomRatio ?: 1f

                onReady()
            } catch (t: Throwable) {
                onError(t)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView, onSwitched: (Boolean) -> Unit = {}) {
        val next = !_useFrontCamera.value
        bind(lifecycleOwner, previewView, front = next) { onSwitched(next) }
    }

    /** Pinch-to-zoom; [ratio] is clamped to what the current lens supports. */
    fun setZoomRatio(ratio: Float) {
        val cam = camera ?: return
        val max = cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
        val min = cam.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
        val clamped = ratio.coerceIn(min, max)
        cam.cameraControl.setZoomRatio(clamped)
        _zoomRatio.value = clamped
    }

    fun setTorch(on: Boolean) {
        val cam = camera ?: return
        if (!cam.cameraInfo.hasFlashUnit()) return
        cam.cameraControl.enableTorch(on)
        _torchOn.value = on
    }

    /** Tap-to-focus at a point in PreviewView coordinates. */
    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val cam = camera ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(4, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        runCatching { cam.cameraControl.startFocusAndMetering(action) }
    }

    /**
     * @param mirror whether the saved JPEG should be flipped horizontally. Selfies look
     *   natural mirrored (that's what you saw in the viewfinder) but text reads backwards,
     *   so it is a user setting rather than a hard-coded choice.
     */
    fun capture(outFile: File, mirror: Boolean, onResult: (Boolean, Throwable?) -> Unit) {
        val capture = imageCapture ?: return onResult(false, IllegalStateException("Camera not ready"))
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = mirror }
        val options = ImageCapture.OutputFileOptions.Builder(outFile)
            .setMetadata(metadata)
            .build()
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = onResult(true, null)
            override fun onError(exception: ImageCaptureException) = onResult(false, exception)
        })
    }

    fun shutdown() {
        runCatching { cameraProvider?.unbindAll() }
        camera = null
        imageCapture = null
        cameraExecutor.shutdown()
    }
}
