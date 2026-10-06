package com.example.directrf

import com.example.directrf.domain.model.CallEvent
import com.example.directrf.domain.model.CallState
import com.example.directrf.domain.model.CallStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class CallStateMachineTest {
    @Test
    fun outgoingCallRequiresPeerAcceptanceAndAudioStart() {
        var state = CallState.CONNECTED
        state = CallStateMachine.transition(state, CallEvent.CALL_SENT)
        assertEquals(CallState.OUTGOING_CALL, state)
        state = CallStateMachine.transition(state, CallEvent.REMOTE_ACCEPTED)
        assertEquals(CallState.AUDIO_CONNECTING, state)
        state = CallStateMachine.transition(state, CallEvent.AUDIO_STARTED)
        assertEquals(CallState.IN_CALL, state)
    }

    @Test
    fun incomingCallCanBeDeclinedWithoutStartingAudio() {
        var state = CallState.CONNECTED
        state = CallStateMachine.transition(state, CallEvent.CALL_RECEIVED)
        assertEquals(CallState.INCOMING_CALL, state)
        state = CallStateMachine.transition(state, CallEvent.CALL_DECLINED)
        assertEquals(CallState.CONNECTED, state)
    }

    @Test
    fun peerLossDuringCallRequiresRecovery() {
        val next = CallStateMachine.transition(CallState.IN_CALL, CallEvent.PEER_LOST)
        assertEquals(CallState.RECONNECTING, next)
    }

    @Test
    fun invalidEventsDoNotInventSuccessfulCalls() {
        val next = CallStateMachine.transition(CallState.IDLE, CallEvent.AUDIO_STARTED)
        assertEquals(CallState.IDLE, next)
    }
}
