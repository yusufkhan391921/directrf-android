package com.example.directrf.domain.model

enum class CallEvent {
    START_DISCOVERY,
    PEER_FOUND,
    CONNECT_PEER,
    PAIRING_REQUESTED,
    PAIRING_REJECTED,
    CONNECTION_ESTABLISHED,
    CALL_SENT,
    CALL_RECEIVED,
    ACCEPT_CALL,
    REMOTE_ACCEPTED,
    CALL_DECLINED,
    END_CALL,
    AUDIO_STARTED,
    PEER_LOST,
    FAILURE
}

object CallStateMachine {
    fun transition(current: CallState, event: CallEvent): CallState =
        transitions[current]?.get(event) ?: current

    private val transitions = mapOf(
        CallState.IDLE to mapOf(
            CallEvent.START_DISCOVERY to CallState.DISCOVERING,
            CallEvent.PAIRING_REQUESTED to CallState.PAIRING
        ),
        CallState.DISCOVERING to mapOf(
            CallEvent.PEER_FOUND to CallState.PEER_AVAILABLE,
            CallEvent.PAIRING_REQUESTED to CallState.PAIRING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.PEER_AVAILABLE to mapOf(
            CallEvent.START_DISCOVERY to CallState.DISCOVERING,
            CallEvent.CONNECT_PEER to CallState.CONNECTING,
            CallEvent.PAIRING_REQUESTED to CallState.PAIRING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.CONNECTING to mapOf(
            CallEvent.PAIRING_REQUESTED to CallState.PAIRING,
            CallEvent.CONNECTION_ESTABLISHED to CallState.CONNECTED,
            CallEvent.FAILURE to CallState.ERROR,
            CallEvent.PEER_LOST to CallState.RECONNECTING
        ),
        CallState.PAIRING to mapOf(
            CallEvent.PAIRING_REJECTED to CallState.PEER_AVAILABLE,
            CallEvent.CONNECTION_ESTABLISHED to CallState.CONNECTED,
            CallEvent.FAILURE to CallState.ERROR,
            CallEvent.PEER_LOST to CallState.RECONNECTING
        ),
        CallState.CONNECTED to mapOf(
            CallEvent.START_DISCOVERY to CallState.DISCOVERING,
            CallEvent.CALL_SENT to CallState.OUTGOING_CALL,
            CallEvent.CALL_RECEIVED to CallState.INCOMING_CALL,
            CallEvent.PEER_LOST to CallState.RECONNECTING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.OUTGOING_CALL to mapOf(
            CallEvent.REMOTE_ACCEPTED to CallState.AUDIO_CONNECTING,
            CallEvent.CALL_DECLINED to CallState.CONNECTED,
            CallEvent.END_CALL to CallState.CONNECTED,
            CallEvent.PEER_LOST to CallState.RECONNECTING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.INCOMING_CALL to mapOf(
            CallEvent.ACCEPT_CALL to CallState.AUDIO_CONNECTING,
            CallEvent.CALL_DECLINED to CallState.CONNECTED,
            CallEvent.END_CALL to CallState.CONNECTED,
            CallEvent.PEER_LOST to CallState.RECONNECTING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.AUDIO_CONNECTING to mapOf(
            CallEvent.AUDIO_STARTED to CallState.IN_CALL,
            CallEvent.END_CALL to CallState.CONNECTED,
            CallEvent.PEER_LOST to CallState.RECONNECTING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.IN_CALL to mapOf(
            CallEvent.END_CALL to CallState.CONNECTED,
            CallEvent.PEER_LOST to CallState.RECONNECTING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.RECONNECTING to mapOf(
            CallEvent.START_DISCOVERY to CallState.DISCOVERING,
            CallEvent.CONNECTION_ESTABLISHED to CallState.CONNECTED,
            CallEvent.PEER_LOST to CallState.RECONNECTING,
            CallEvent.FAILURE to CallState.ERROR
        ),
        CallState.ERROR to mapOf(
            CallEvent.START_DISCOVERY to CallState.DISCOVERING,
            CallEvent.PAIRING_REQUESTED to CallState.PAIRING
        )
    )
}
