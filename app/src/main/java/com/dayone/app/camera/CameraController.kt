package com.dayone.app.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Thin wrapper around CameraX so the Compose capture screen stays simple.
 * Defaults to the front camera since this is a self-portrait app, but lets you flip.
 */
class CameraController(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var useFrontCamera = true

    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit = {}
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val selector = if (useFrontCamera)
                CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            onReady()
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        useFrontCamera = !useFrontCamera
        bind(lifecycleOwner, previewView)
    }

    fun capture(outFile: File, onResult: (Boolean, Throwable?) -> Unit) {
        val capture = imageCapture ?: return onResult(false, IllegalStateException("Camera not ready"))
        val options = ImageCapture.OutputFileOptions.Builder(outFile).build()
        capture.takePicture(options, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult(true, null)
            }
            override fun onError(exception: ImageCaptureException) {
                onResult(false, exception)
            }
        })
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
