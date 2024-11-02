package org.snapmock.snap.core

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

    fun start() {
        isRecording.set(true)
    }

    fun stop() {
        isRecording.set(false)
    }

    fun record(snap: InvocationSnap) {
        if (isRecording.get()) {
            invocations.get().add(snap)
        }
    }

    fun record(snap: FactoryInvocationSnap) {
        if (isRecording.get()) {
            factoryInvocations.get().add(snap)
        }
    }

    fun getDependencyInvocations(): List<InvocationSnap> = invocations.get()

    fun getFactoryInvocations(): List<FactoryInvocationSnap> = factoryInvocations.get()

    fun reset() {
        isRecording.set(false)
        invocations.remove()
        factoryInvocations.remove()
    }
}
