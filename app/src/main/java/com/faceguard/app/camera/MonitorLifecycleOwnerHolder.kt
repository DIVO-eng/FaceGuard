package com.faceguard.app.camera

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * CameraX's bindToLifecycle requires a LifecycleOwner. Since capture is
 * triggered from a background service (no visible Activity exists at that
 * moment), we provide a minimal manually-driven lifecycle that is kept in
 * RESUMED state only for the few hundred ms needed to grab one frame, then
 * torn down, so the camera indicator/LED is only lit for that brief instant.
 */
object MonitorLifecycleOwnerHolder {

    private val registry = LifecycleRegistry(object : LifecycleOwner {
        override val lifecycle: Lifecycle get() = registry
    }).apply {
        currentState = Lifecycle.State.CREATED
    }

    val owner = object : LifecycleOwner {
        override val lifecycle: Lifecycle get() = registry
    }

    fun markResumed() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun markStopped() {
        registry.currentState = Lifecycle.State.CREATED
    }
}
