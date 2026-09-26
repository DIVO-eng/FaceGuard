package com.faceguard.app.camera

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.faceguard.app.data.RecognitionStatus
import kotlinx.coroutines.tasks.await

/**
 * Fully on-device via ML Kit (bundled model, no network calls, no images ever
 * leave the phone for this step). This is a lightweight "is this geometry
 * close to the owner's enrolled face?" comparison using landmark distances —
 * good enough for "known vs. unknown", not a production-grade face-ID system.
 * A note to that effect is shown in Settings so expectations are set
 * correctly (per spec: no pretending to capabilities Android/ML Kit doesn't
 * really offer out of the box).
 */
class FaceMatcher(private val context: Context) {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
    )

    suspend fun evaluate(jpegBytes: ByteArray, sensitivity: Int): RecognitionStatus {
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return RecognitionStatus.NO_FACE_DETECTED
        val input = InputImage.fromBitmap(bitmap, 0)

        val faces = try {
            detector.process(input).await()
        } catch (_: Throwable) {
            return RecognitionStatus.NOT_EVALUATED
        }

        if (faces.isEmpty()) return RecognitionStatus.NO_FACE_DETECTED

        val enrolled = FaceEnrollmentStore(context).getEnrolledLandmarks() ?: run {
            // Owner hasn't enrolled their own face yet -> can't classify known/unknown.
            return RecognitionStatus.NOT_EVALUATED
        }

        val candidate = faces.first()
        val distance = LandmarkComparer.distance(candidate, enrolled)
        val threshold = LandmarkComparer.thresholdForSensitivity(sensitivity)

        return if (distance <= threshold) RecognitionStatus.RECOGNIZED_OWNER
        else RecognitionStatus.UNKNOWN_FACE
    }
}
