package me.moeyinlo.visualize.jvm.gui

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal object JavaFxSmokeTestHarness {
    private val toolkitStarted = AtomicBoolean(false)

    fun <T> runAndWait(action: () -> T): T {
        startToolkit()
        if (Platform.isFxApplicationThread()) {
            return action()
        }

        val result = AtomicReference<Result<T>>()
        val finished = CountDownLatch(1)
        Platform.runLater {
            result.set(runCatching(action))
            finished.countDown()
        }
        await(finished, "JavaFX test action did not finish")
        return result.get().getOrThrow()
    }

    private fun startToolkit() {
        if (!toolkitStarted.compareAndSet(false, true)) {
            return
        }

        val started = CountDownLatch(1)
        try {
            Platform.startup {
                Platform.setImplicitExit(false)
                started.countDown()
            }
        } catch (_: IllegalStateException) {
            Platform.runLater {
                Platform.setImplicitExit(false)
                started.countDown()
            }
        }
        await(started, "JavaFX toolkit did not start")
    }

    private fun await(
        latch: CountDownLatch,
        failureMessage: String,
    ) {
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw AssertionError(failureMessage)
        }
    }
}
