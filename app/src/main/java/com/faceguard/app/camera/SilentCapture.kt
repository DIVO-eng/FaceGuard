package com.faceguard.app.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Captures a single still frame from the FRONT camera only, only when the
 * CAMERA permission has already been explicitly granted by the owner.
 * Returns null (never throws to the caller) if the camera is unavailable,
 * e.g. permission revoked or another app holds it — this is surfaced to the
 * dashboard as "capture failed" rather than silently pretending to succeed.
 */
object SilentCapture {

    private const val TAG = "FaceGuard.Capture"

    suspend fun captureFrontJpeg(context: Context): ByteArray? {
        val provider = getCameraProvider(context) ?: return null

        return suspendCancellableCoroutine { cont ->
            try {
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val selector = CameraSelector.DEFAULT_FRONT_CAMERA
                provider.unbindAll()
                // NOTE: on real devices this requires binding to a LifecycleOwner.
                // In the foreground service we use a ProcessLifecycleOwner-backed
                // stub lifecycle (see MonitorService.CaptureLifecycleOwner) since
                // there is no visible Activity when the event fires.
                provider.bindToLifecycle(
                    MonitorLifecycleOwnerHolder.owner,
                    selector,
                    imageCapture
                )

                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bytes = imageProxyToJpegBytes(image)
                            image.close()
                            provider.unbindAll()
                            if (cont.isActive) cont.resume(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.w(TAG, "Capture failed: ${exception.message}")
                            provider.unbindAll()
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Capture setup failed: ${t.message}")
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private suspend fun getCameraProvider(context: Context): ProcessCameraProvider? =
        suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cont.resume(future.get())
                } catch (t: Throwable) {
                    Log.w(TAG, "No camera provider: ${t.message}")
                    cont.resume(null)
                }
            }, ContextCompat.getMainExecutor(context))
        }

    private fun imageProxyToJpegBytes(image: ImageProxy): ByteArray {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }
}
