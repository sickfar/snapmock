package io.github.sickfar.snapmock.core

import java.util.function.Consumer

/**
 * Post processor should be executed on result if defined.
 * Helps to workaround async or postponed result invocation.
 *
 * For example, Spring StreamingResponseBody may be a lambda expression and will not be snapped outside of [Snap] annotation,
 * because StreamingResponseBody is being processed in different thread.
 * As a workaround, StreamingResponseBody can be called to consume [java.io.OutputStream.nullOutputStream] to trigger
 * all the required dependent invocations to be snapped.
 */
interface SnapResultPostprocessor<T> : Consumer<T> {
    override fun accept(t: T)
}
