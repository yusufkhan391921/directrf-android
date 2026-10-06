package com.example.directrf.audio

import com.example.directrf.data.transport.EncodedAudioFrame
import java.util.TreeMap

class JitterBuffer(
    private val startupFrames: Int = 3,
    private val capacityFrames: Int = 8
) {
    private val frames = TreeMap<Long, EncodedAudioFrame>()
    private var nextSequence: Long? = null
    private var gapWaited = false

    init {
        require(startupFrames > 0)
        require(capacityFrames >= startupFrames)
    }

    @Synchronized
    fun offer(frame: EncodedAudioFrame): Boolean {
        if (nextSequence?.let { frame.sequence < it } == true) return false
        if (frames.putIfAbsent(frame.sequence, frame) != null) return false
        while (frames.size > capacityFrames) frames.pollLastEntry()
        return true
    }

    @Synchronized
    fun poll(): EncodedAudioFrame? {
        if (nextSequence == null) {
            if (frames.size < startupFrames) return null
            nextSequence = frames.firstKey()
        }

        val expected = nextSequence ?: return null
        frames.remove(expected)?.let {
            nextSequence = expected + 1
            gapWaited = false
            return it
        }

        while (frames.isNotEmpty() && frames.firstKey() < expected) frames.pollFirstEntry()
        if (frames.isEmpty()) return null
        if (!gapWaited) {
            gapWaited = true
            return null
        }

        val next = frames.pollFirstEntry()?.value ?: return null
        nextSequence = next.sequence + 1
        gapWaited = false
        return next
    }

    @Synchronized
    fun clear() {
        frames.clear()
        nextSequence = null
        gapWaited = false
    }
}
