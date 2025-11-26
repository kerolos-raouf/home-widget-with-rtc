//package com.example.homewidgettocall.repository
//
//import android.content.Context
//import android.util.Log
//import io.socket.client.IO
//import io.socket.client.Socket
//import org.json.JSONObject
//import org.webrtc.*
//import kotlin.collections.get
//
///**
// * Audio-only WebRTC client for background calls and widgets
// * No video, no UI components needed - perfect for home widgets!
// */
//class AudioOnlyWebRTCClient(
//    private val context: Context,
//    private val serverUrl: String,
//    private val listener: AudioCallListener
//) {
//    private var socket: Socket? = null
//    private var peerConnectionFactory: PeerConnectionFactory? = null
//    private var peerConnection: PeerConnection? = null
//    private var localAudioTrack: AudioTrack? = null
//
//    private var currentRoomId: String? = null
//    private var remoteUserId: String? = null
//
//    private var isMuted = false
//
//    interface AudioCallListener {
//        fun onConnectedToServer()
//        fun onDisconnectedFromServer()
//        fun onJoinedRoom(roomId: String)
//        fun onUserJoined(userId: String)
//        fun onUserLeft(userId: String)
//        fun onCallConnected()
//        fun onCallDisconnected()
//        fun onError(error: String)
//    }
//
//    init {
//        initializePeerConnectionFactory()
//    }
//
//    private fun initializePeerConnectionFactory() {
//        // Simpler initialization for audio-only
//        val options = PeerConnectionFactory.InitializationOptions.builder(context)
//            .setEnableInternalTracer(false)
//            .createInitializationOptions()
//        PeerConnectionFactory.initialize(options)
//
//        peerConnectionFactory = PeerConnectionFactory.builder()
//            .setOptions(PeerConnectionFactory.Options().apply {
//                disableEncryption = false
//                disableNetworkMonitor = false
//            })
//            .createPeerConnectionFactory()
//
//        Log.d(TAG, "Audio-only PeerConnectionFactory initialized")
//    }
//
//    fun connectToServer() {
//        try {
//            val options = IO.Options().apply {
//                reconnection = true
//                reconnectionDelay = 1000
//                reconnectionAttempts = 10
//                timeout = 10000
//            }
//
//            socket = IO.socket(serverUrl, options)
//
//            socket?.apply {
//                on(Socket.EVENT_CONNECT) {
//                    Log.d(TAG, "🎙️ Connected to server (audio-only)")
//                    listener.onConnectedToServer()
//                }
//
//                on(Socket.EVENT_DISCONNECT) {
//                    Log.d(TAG, "Disconnected from server")
//                    listener.onDisconnectedFromServer()
//                }
//
//                on(Socket.EVENT_CONNECT_ERROR) { args ->
//                    Log.e(TAG, "Connection error: ${args.firstOrNull()}")
//                    listener.onError("Connection failed")
//                }
//
//                on("user-joined") { args ->
//                    try {
//                        val data = args[0] as JSONObject
//                        val userId = data.getString("userId")
//                        Log.d(TAG, "User joined: $userId")
//                        remoteUserId = userId
//                        listener.onUserJoined(userId)
//
//                        // If we have local audio, create offer
//                        if (localAudioTrack != null) {
//                            createOffer(userId)
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error handling user-joined", e)
//                    }
//                }
//
//                on("user-left") { args ->
//                    try {
//                        val data = args[0] as JSONObject
//                        val userId = data.getString("userId")
//                        Log.d(TAG, "User left: $userId")
//                        listener.onUserLeft(userId)
//                        closePeerConnection()
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error handling user-left", e)
//                    }
//                }
//
//                on("offer") { args ->
//                    try {
//                        val data = args[0] as JSONObject
//                        val senderId = data.getString("senderId")
//                        val offer = data.getJSONObject("offer")
//                        Log.d(TAG, "Received offer from: $senderId")
//                        remoteUserId = senderId
//                        handleOffer(offer, senderId)
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error handling offer", e)
//                    }
//                }
//
//                on("answer") { args ->
//                    try {
//                        val data = args[0] as JSONObject
//                        val answer = data.getJSONObject("answer")
//                        Log.d(TAG, "Received answer")
//                        handleAnswer(answer)
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error handling answer", e)
//                    }
//                }
//
//                on("ice-candidate") { args ->
//                    try {
//                        val data = args[0] as JSONObject
//                        val candidate = data.getJSONObject("candidate")
//                        addIceCandidate(candidate)
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error handling ICE candidate", e)
//                    }
//                }
//
//                connect()
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error connecting to server", e)
//            listener.onError("Connection error: ${e.message}")
//        }
//    }
//
//    fun joinRoom(roomId: String) {
//        currentRoomId = roomId
//
//        val data = JSONObject().apply {
//            put("roomId", roomId)
//            put("userId", socket?.id() ?: "unknown")
//        }
//
//        socket?.emit("join-room", data) { args ->
//            try {
//                val response = args[0] as JSONObject
//                val success = response.getBoolean("success")
//
//                if (success) {
//                    Log.d(TAG, "Joined room: $roomId")
//                    listener.onJoinedRoom(roomId)
//
//                    val participants = response.optJSONArray("participants")
//                    if (participants != null && participants.length() > 0) {
//                        remoteUserId = participants.getString(0)
//                    }
//                } else {
//                    listener.onError("Failed to join room")
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error in join-room callback", e)
//            }
//        }
//    }
//
//    /**
//     * Start audio-only stream - no video, no SurfaceViewRenderer needed!
//     */
//    fun startAudioCall() {
//        try {
//            // Create audio track with echo cancellation and noise suppression
//            val audioConstraints = MediaConstraints().apply {
//                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
//                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
//                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
//            }
//
//            val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
//            localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", audioSource)
//
//            Log.d(TAG, "🎙️ Audio stream started")
//
//            // If remote user exists, create offer
//            remoteUserId?.let { userId ->
//                createOffer(userId)
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error starting audio", e)
//            listener.onError("Failed to start audio: ${e.message}")
//        }
//    }
//
//    private fun createPeerConnection(targetUserId: String) {
//        val iceServers = listOf(
//            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
//            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
//        )
//
//        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
//            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
//            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
//            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
//            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
//            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
//        }
//
//        peerConnection = peerConnectionFactory?.createPeerConnection(
//            rtcConfig,
//            object : PeerConnection.Observer {
//                override fun onIceCandidate(candidate: IceCandidate) {
//                    val data = JSONObject().apply {
//                        put("targetUserId", targetUserId)
//                        put("roomId", currentRoomId)
//                        put("candidate", JSONObject().apply {
//                            put("candidate", candidate.sdp)
//                            put("sdpMLineIndex", candidate.sdpMLineIndex)
//                            put("sdpMid", candidate.sdpMid)
//                        })
//                    }
//                    socket?.emit("ice-candidate", data)
//                }
//
//                override fun onAddStream(stream: MediaStream) {
//                    Log.d(TAG, "Remote audio stream added")
//                    // Audio plays automatically through device speakers/earpiece
//                }
//
//                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
//                    Log.d(TAG, "Connection state: $newState")
//                    when (newState) {
//                        PeerConnection.PeerConnectionState.CONNECTED -> {
//                            listener.onCallConnected()
//                        }
//                        PeerConnection.PeerConnectionState.DISCONNECTED,
//                        PeerConnection.PeerConnectionState.FAILED -> {
//                            listener.onCallDisconnected()
//                        }
//                        else -> {}
//                    }
//                }
//
//                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
//                    Log.d(TAG, "ICE connection state: $newState")
//                }
//
//                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
//                override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
//                override fun onDataChannel(dataChannel: DataChannel) {}
//                override fun onRenegotiationNeeded() {}
//                override fun onRemoveStream(stream: MediaStream) {}
//                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
//                override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {
//                    Log.d(TAG, "Track added: ${receiver.track()?.kind()}")
//                }
//            }
//        )
//
//        // Add local audio track
//        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("local_stream")) }
//
//        Log.d(TAG, "PeerConnection created for: $targetUserId")
//    }
//
//    private fun createOffer(targetUserId: String) {
//        if (peerConnection == null) {
//            createPeerConnection(targetUserId)
//        }
//
//        val constraints = MediaConstraints().apply {
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false")) // Audio only!
//        }
//
//        peerConnection?.createOffer(object : SdpObserver {
//            override fun onCreateSuccess(sessionDescription: SessionDescription) {
//                peerConnection?.setLocalDescription(object : SdpObserver {
//                    override fun onSetSuccess() {
//                        val data = JSONObject().apply {
//                            put("targetUserId", targetUserId)
//                            put("roomId", currentRoomId)
//                            put("offer", JSONObject().apply {
//                                put("type", sessionDescription.type.canonicalForm())
//                                put("sdp", sessionDescription.description)
//                            })
//                        }
//                        socket?.emit("offer", data)
//                    }
//                    override fun onSetFailure(error: String) {
//                        Log.e(TAG, "Set local description failed: $error")
//                    }
//                    override fun onCreateSuccess(p0: SessionDescription) {}
//                    override fun onCreateFailure(error: String) {}
//                }, sessionDescription)
//            }
//            override fun onCreateFailure(error: String) {
//                Log.e(TAG, "Create offer failed: $error")
//            }
//            override fun onSetSuccess() {}
//            override fun onSetFailure(error: String) {}
//        }, constraints)
//    }
//
//    private fun handleOffer(offer: JSONObject, senderId: String) {
//        if (peerConnection == null) {
//            createPeerConnection(senderId)
//        }
//
//        val sdp = SessionDescription(
//            SessionDescription.Type.OFFER,
//            offer.getString("sdp")
//        )
//
//        peerConnection?.setRemoteDescription(object : SdpObserver {
//            override fun onSetSuccess() {
//                createAnswer(senderId)
//            }
//            override fun onSetFailure(error: String) {
//                Log.e(TAG, "Set remote description failed: $error")
//            }
//            override fun onCreateSuccess(p0: SessionDescription) {}
//            override fun onCreateFailure(error: String) {}
//        }, sdp)
//    }
//
//    private fun createAnswer(targetUserId: String) {
//        val constraints = MediaConstraints().apply {
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false")) // Audio only!
//        }
//
//        peerConnection?.createAnswer(object : SdpObserver {
//            override fun onCreateSuccess(sessionDescription: SessionDescription) {
//                peerConnection?.setLocalDescription(object : SdpObserver {
//                    override fun onSetSuccess() {
//                        val data = JSONObject().apply {
//                            put("targetUserId", targetUserId)
//                            put("roomId", currentRoomId)
//                            put("answer", JSONObject().apply {
//                                put("type", sessionDescription.type.canonicalForm())
//                                put("sdp", sessionDescription.description)
//                            })
//                        }
//                        socket?.emit("answer", data)
//                    }
//                    override fun onSetFailure(error: String) {}
//                    override fun onCreateSuccess(p0: SessionDescription) {}
//                    override fun onCreateFailure(error: String) {}
//                }, sessionDescription)
//            }
//            override fun onCreateFailure(error: String) {}
//            override fun onSetSuccess() {}
//            override fun onSetFailure(error: String) {}
//        }, constraints)
//    }
//
//    private fun handleAnswer(answer: JSONObject) {
//        val sdp = SessionDescription(
//            SessionDescription.Type.ANSWER,
//            answer.getString("sdp")
//        )
//
//        peerConnection?.setRemoteDescription(object : SdpObserver {
//            override fun onSetSuccess() {
//                Log.d(TAG, "Remote description set (answer)")
//            }
//            override fun onSetFailure(error: String) {}
//            override fun onCreateSuccess(p0: SessionDescription) {}
//            override fun onCreateFailure(error: String) {}
//        }, sdp)
//    }
//
//    private fun addIceCandidate(candidateJson: JSONObject) {
//        val candidate = IceCandidate(
//            candidateJson.getString("sdpMid"),
//            candidateJson.getInt("sdpMLineIndex"),
//            candidateJson.getString("candidate")
//        )
//        peerConnection?.addIceCandidate(candidate)
//    }
//
//    /**
//     * Mute/unmute microphone
//     */
//    fun setMuted(muted: Boolean) {
//        isMuted = muted
//        localAudioTrack?.setEnabled(!muted)
//        Log.d(TAG, if (muted) "🔇 Muted" else "🔊 Unmuted")
//    }
//
//    fun isMuted(): Boolean = isMuted
//
//    /**
//     * Toggle mute state
//     */
//    fun toggleMute(): Boolean {
//        setMuted(!isMuted)
//        return isMuted
//    }
//
//    private fun closePeerConnection() {
//        peerConnection?.close()
//        peerConnection = null
//    }
//
//    fun leaveRoom() {
//        socket?.emit("leave-room")
//        closePeerConnection()
//        currentRoomId = null
//        remoteUserId = null
//    }
//
//    fun disconnect() {
//        leaveRoom()
//        localAudioTrack?.dispose()
//        socket?.disconnect()
//        socket = null
//        peerConnectionFactory?.dispose()
//        Log.d(TAG, "Disconnected")
//    }
//
//    fun isInCall(): Boolean {
//        return peerConnection?.connectionState() == PeerConnection.PeerConnectionState.CONNECTED
//    }
//
//    fun getCurrentRoomId(): String? = currentRoomId
//
//    companion object {
//        private const val TAG = "AudioOnlyWebRTC"
//    }
//}
