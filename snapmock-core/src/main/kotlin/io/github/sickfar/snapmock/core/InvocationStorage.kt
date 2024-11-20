package io.github.sickfar.snapmock.core

/**
 * Thread-local based storage to keep recorded dependency invocation snapshots
 *
 * @since 1.0.0
 * @author Roman Aksenenko
 */
class InvocationStorage {
    private val invocations = object : ThreadLocal<MutableList<InvocationSnap>>() {
        override fun initialValue() = mutableListOf<InvocationSnap>()
    }

    private val factoryInvocations = object : ThreadLocal<MutableList<FactoryInvocationSnap>>() {
        override fun initialValue() = mutableListOf<FactoryInvocationSnap>()
    }

    private val isRecording = object : ThreadLocal<Boolean>() {
        override fun initialValue() = false
    }

    /**
     * Start recording dependency invocations
     */
    fun start() {
        isRecording.set(true)
    }

    /**
     * Stop recording dependency invocations
     */
    fun stop() {
        isRecording.set(false)
    }

    /**
     * Record a dependency invocation snapshot
     * @param snap Dependency snapshot to record
     */
    fun record(snap: InvocationSnap) {
        if (isRecording.get()) {
            invocations.get().add(snap)
        }
    }

    /**
     * Record a dependency factory bean invocation
     * @param snap Factory bean invocation snapshot to record
     */
    fun record(snap: FactoryInvocationSnap) {
        if (isRecording.get()) {
            factoryInvocations.get().add(snap)
        }
    }

    /**
     * Get recorded dependency invocation snapshots
     * @return Recorded dependency invocation snapshots
     */
    fun getDependencyInvocations(): List<InvocationSnap> = invocations.get()

    /**
     * Get recorded dependency factory bean invocation snapshots
     * @return Recorded dependency factory bean invocation snapshots
     */
    fun getFactoryInvocations(): List<FactoryInvocationSnap> = factoryInvocations.get()

    /**
     * Reset store and get ready for a next recording
     */
    fun reset() {
        isRecording.set(false)
        invocations.remove()
        factoryInvocations.remove()
    }
}
