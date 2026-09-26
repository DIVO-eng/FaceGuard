package com.faceguard.app.camera

import android.content.Context
import com.google.mlkit.vision.face.Face
import com.faceguard.app.util.SecurePrefs
import android.util.Base64
import kotlin.math.sqrt

/**
 * Simplified landmark-distance comparator. This is intentionally NOT a
 * cryptographic-grade face-ID system (that's outside what ML Kit's bundled
 * detector provides) — it's a "close enough to flag as familiar" heuristic,
 * exactly as strong as similar consumer intruder-detection apps. The
 * Settings/Privacy screen states this plainly rather than overclaiming.
 */
object LandmarkComparer {

    fun extractVector(face: Face): FloatArray {
        val pts = listOfNotNull(
            face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)?.position,
            face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)?.position,
            face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.NOSE_BASE)?.position,
            face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_LEFT)?.position,
            face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.MOUTH_RIGHT)?.position
        )
        return pts.flatMap { listOf(it.x, it.y) }.toFloatArray()
    }

    fun distance(face: Face, enrolled: FloatArray): Float {
        val v = extractVector(face)
        if (v.size != enrolled.size || v.isEmpty()) return Float.MAX_VALUE
        var sum = 0f
        for (i in v.indices) {
            val d = v[i] - enrolled[i]
            sum += d * d
        }
        return sqrt(sum)
    }

    /** Higher sensitivity (0-100) => smaller (stricter) match threshold. */
    fun thresholdForSensitivity(sensitivity: Int): Float {
        val clamped = sensitivity.coerceIn(0, 100)
        // 100 sensitivity -> tight threshold (~15px), 0 -> loose (~80px)
        return 80f - (clamped / 100f) * 65f
    }
}

class FaceEnrollmentStore(private val context: Context) {
    private val KEY = "enrolled_face_vector"

    fun saveEnrolledLandmarks(vector: FloatArray) {
        val encoded = vector.joinToString(",")
        SecurePrefs.get(context).edit().putString(KEY, encoded).apply()
    }

    fun getEnrolledLandmarks(): FloatArray? {
        val raw = SecurePrefs.get(context).getString(KEY, null) ?: return null
        return raw.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
    }

    fun isEnrolled(): Boolean = getEnrolledLandmarks() != null
}
