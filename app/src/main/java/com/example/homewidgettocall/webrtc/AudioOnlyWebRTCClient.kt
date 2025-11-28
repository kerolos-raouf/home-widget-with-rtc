package com.example.homewidgettocall.webrtc

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*

/**
 * Audio-only WebRTC client for background calls
 * Perfect for widgets - no video, no UI components needed!
 */
class AudioOnlyWebRTCClient(
    private val context: Context,
    private val serverUrl: String,
    private val listener: AudioCallListener
) {
    private var socket: Socket? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    
    private var currentRoomId: String? = null
    private var remoteUserId: String? = null
    
    private var isMuted = false
    private var makingOffer = false
    private var isPolite = false  // Will be set based on socket ID comparison

    interface AudioCallListener {
        fun onConnectedToServer()
        fun onDisconnectedFromServer()
        fun onJoinedRoom(roomId: String)
        fun onUserJoined(userId: String)
        fun onUserLeft(userId: String)
        fun onCallConnected()
        fun onCallDisconnected()
        fun onError(error: String)
    }

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            })
            .createPeerConnectionFactory()

        Log.d(TAG, "Audio-only PeerConnectionFactory initialized")
    }

    fun connectToServer() {
        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionDelay = 1000
                reconnectionAttempts = 10
                timeout = 10000
            }
            
            socket = IO.socket(serverUrl, options)
            
            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "🎙️ Connected to server (audio-only)")
                    listener.onConnectedToServer()
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Disconnected from server")
                    listener.onDisconnectedFromServer()
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "Connection error: ${args.firstOrNull()}")
                    listener.onError("Connection failed")
                }

                on("user-joined") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val userId = data.getString("userId")
                        Log.d(TAG, "👤 User joined: $userId")
                        remoteUserId = userId
                        listener.onUserJoined(userId)
                        
                        // DON'T create offer here!
                        // The new user (second person) will create the offer
                        // First person just waits and responds with answer
                        Log.d(TAG, "Waiting for offer from the newly joined user...")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling user-joined", e)
                    }
                }

                on("user-left") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val userId = data.getString("userId")
                        Log.d(TAG, "User left: $userId")
                        listener.onUserLeft(userId)
                        closePeerConnection()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling user-left", e)
                    }
                }

                on("offer") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val senderId = data.getString("senderId")
                        val offer = data.getJSONObject("offer")
                        Log.d(TAG, "Received offer from: $senderId")
                        remoteUserId = senderId
                        handleOffer(offer, senderId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling offer", e)
                    }
                }

                on("answer") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val answer = data.getJSONObject("answer")
                        Log.d(TAG, "Received answer")
                        handleAnswer(answer)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling answer", e)
                    }
                }

                on("ice-candidate") { args ->
                    try {
                        val data = args[0] as JSONObject
                        val candidate = data.getJSONObject("candidate")
                        addIceCandidate(candidate)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling ICE candidate", e)
                    }
                }

                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to server", e)
            listener.onError("Connection error: ${e.message}")
        }
    }

    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        
        val data = JSONObject().apply {
            put("roomId", roomId)
            put("userId", socket?.id() ?: "unknown")
        }

        socket?.emit("join-room", data, io.socket.client.Ack { args ->
            try {
                val response = args[0] as JSONObject
                val success = response.getBoolean("success")
                
                if (success) {
                    Log.d(TAG, "Joined room: $roomId")
                    listener.onJoinedRoom(roomId)
                    
                    // Check if there are existing participants
                    val participants = response.optJSONArray("participants")
                    if (participants != null && participants.length() > 0) {
                        remoteUserId = participants.getString(0)
                        Log.d(TAG, "Found existing participant: $remoteUserId")
                        Log.d(TAG, "👥 I'm the SECOND person - I will create the offer")
                        
                        // Second person creates the offer
                        if (localAudioTrack != null) {
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                remoteUserId?.let { userId ->
                                    Log.d(TAG, "Creating offer as second participant")
                                    createOffer(userId)
                                }
                            }, 500)
                        }
                    } else {
                        Log.d(TAG, "👤 I'm the FIRST person - I will wait for others and respond to their offers")
                    }
                } else {
                    listener.onError("Failed to join room")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in join-room callback", e)
            }
        })
    }

    fun startAudioCall() {
        try {
            // Configure audio constraints with echo cancellation
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }
            
            val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
            localAudioTrack = peerConnectionFactory?.createAudioTrack("local_audio", audioSource)
            
            // IMPORTANT: Enable audio track
            localAudioTrack?.setEnabled(true)
            
            Log.d(TAG, "🎙️ Audio stream started and ready")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio", e)
            listener.onError("Failed to start audio: ${e.message}")
        }
    }

    private fun createPeerConnection(targetUserId: String) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    val data = JSONObject().apply {
                        put("targetUserId", targetUserId)
                        put("roomId", currentRoomId)
                        put("candidate", JSONObject().apply {
                            put("candidate", candidate.sdp)
                            put("sdpMLineIndex", candidate.sdpMLineIndex)
                            put("sdpMid", candidate.sdpMid)
                        })
                    }
                    socket?.emit("ice-candidate", data)
                }

                override fun onAddStream(stream: MediaStream) {
                    Log.d(TAG, "Remote audio stream added")
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    Log.d(TAG, "Connection state: $newState")
                    when (newState) {
                        PeerConnection.PeerConnectionState.CONNECTED -> {
                            listener.onCallConnected()
                        }
                        PeerConnection.PeerConnectionState.DISCONNECTED,
                        PeerConnection.PeerConnectionState.FAILED -> {
                            listener.onCallDisconnected()
                        }
                        else -> {}
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                    Log.d(TAG, "🧊 ICE connection state: $newState")
                    when (newState) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> {
                            Log.d(TAG, "✅ ICE connection established")
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            Log.e(TAG, "❌ ICE connection failed")
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            Log.w(TAG, "⚠️ ICE connection disconnected")
                        }
                        else -> {}
                    }
                }
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                    Log.d(TAG, "🧊 ICE gathering state: $newState")
                }
                override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
                override fun onDataChannel(dataChannel: DataChannel) {}
                override fun onRenegotiationNeeded() {}
                override fun onRemoveStream(stream: MediaStream) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
                override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
            }
        )

        localAudioTrack?.let { track -> 
            peerConnection?.addTrack(track, listOf("local_stream"))
        }
        
        Log.d(TAG, "PeerConnection created for: $targetUserId")
    }

    private fun createOffer(targetUserId: String) {
        if (peerConnection == null) {
            createPeerConnection(targetUserId)
        }

        Log.d(TAG, "📤 Creating offer for: $targetUserId")
        makingOffer = true  // Set flag to detect collisions
        
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "✅ Offer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        makingOffer = false  // Clear flag after offer is set
                        Log.d(TAG, "✅ Local description set, sending offer to $targetUserId")
                        val data = JSONObject().apply {
                            put("targetUserId", targetUserId)
                            put("roomId", currentRoomId)
                            put("offer", JSONObject().apply {
                                put("type", sessionDescription.type.canonicalForm())
                                put("sdp", sessionDescription.description)
                            })
                        }
                        socket?.emit("offer", data)
                    }
                    override fun onSetFailure(error: String) {
                        makingOffer = false  // Clear flag on failure
                        Log.e(TAG, "❌ Set local description failed: $error")
                    }
                    override fun onCreateSuccess(p0: SessionDescription) {}
                    override fun onCreateFailure(error: String) {}
                }, sessionDescription)
            }
            override fun onCreateFailure(error: String) {
                makingOffer = false  // Clear flag on failure
                Log.e(TAG, "❌ Create offer failed: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    private fun handleOffer(offer: JSONObject, senderId: String) {
        Log.d(TAG, "📥 Received offer from: $senderId")
        
        // Make sure we have our local audio track before responding
        if (localAudioTrack == null) {
            Log.e(TAG, "❌ Cannot handle offer: local audio track not initialized!")
            listener.onError("Audio not ready")
            return
        }
        
        // Determine who is polite based on socket IDs (lexicographic comparison)
        if (!isPolite && remoteUserId != null) {
            val myId = socket?.id() ?: ""
            isPolite = myId > remoteUserId!!  // Polite peer has higher ID
            Log.d(TAG, "I am ${if (isPolite) "POLITE" else "IMPOLITE"} peer")
        }
        
        // Perfect negotiation logic
        val offerCollision = makingOffer
        val ignoreOffer = !isPolite && offerCollision
        
        if (ignoreOffer) {
            Log.w(TAG, "⚠️ Ignoring offer due to collision (I'm impolite and making offer)")
            return
        }
        
        if (peerConnection == null) {
            Log.d(TAG, "Creating peer connection for incoming offer")
            createPeerConnection(senderId)
        }

        val sdp = SessionDescription(
            SessionDescription.Type.OFFER,
            offer.getString("sdp")
        )

        Log.d(TAG, "Setting remote description (offer)")
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote description set, creating answer")
                createAnswer(senderId)
            }
            override fun onSetFailure(error: String) {
                Log.e(TAG, "❌ Set remote description failed: $error")
            }
            override fun onCreateSuccess(p0: SessionDescription) {}
            override fun onCreateFailure(error: String) {}
        }, sdp)
    }

    private fun createAnswer(targetUserId: String) {
        Log.d(TAG, "📤 Creating answer for: $targetUserId")
        
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "✅ Answer created successfully")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.d(TAG, "✅ Local description set (answer), sending to $targetUserId")
                        val data = JSONObject().apply {
                            put("targetUserId", targetUserId)
                            put("roomId", currentRoomId)
                            put("answer", JSONObject().apply {
                                put("type", sessionDescription.type.canonicalForm())
                                put("sdp", sessionDescription.description)
                            })
                        }
                        socket?.emit("answer", data)
                    }
                    override fun onSetFailure(error: String) {
                        Log.e(TAG, "❌ Set local description failed (answer): $error")
                    }
                    override fun onCreateSuccess(p0: SessionDescription) {}
                    override fun onCreateFailure(error: String) {}
                }, sessionDescription)
            }
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "❌ Create answer failed: $error")
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
        }, constraints)
    }

    private fun handleAnswer(answer: JSONObject) {
        Log.d(TAG, "📥 Received answer, setting remote description")
        
        val sdp = SessionDescription(
            SessionDescription.Type.ANSWER,
            answer.getString("sdp")
        )

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote description set (answer) - connection should be established now")
            }
            override fun onSetFailure(error: String) {
                Log.e(TAG, "❌ Set remote description failed (answer): $error")
            }
            override fun onCreateSuccess(p0: SessionDescription) {}
            override fun onCreateFailure(error: String) {}
        }, sdp)
    }

    private fun addIceCandidate(candidateJson: JSONObject) {
        try {
            val candidate = IceCandidate(
                candidateJson.getString("sdpMid"),
                candidateJson.getInt("sdpMLineIndex"),
                candidateJson.getString("candidate")
            )
            peerConnection?.addIceCandidate(candidate)
            Log.d(TAG, "✅ ICE candidate added")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add ICE candidate: ${e.message}")
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        localAudioTrack?.setEnabled(!muted)
        Log.d(TAG, if (muted) "🔇 Muted" else "🔊 Unmuted")
    }

    fun isMuted(): Boolean = isMuted

    fun toggleMute(): Boolean {
        setMuted(!isMuted)
        return isMuted
    }

    private fun closePeerConnection() {
        peerConnection?.close()
        peerConnection = null
    }

    fun leaveRoom() {
        socket?.emit("leave-room")
        closePeerConnection()
        currentRoomId = null
        remoteUserId = null
    }

    fun disconnect() {
        leaveRoom()
        localAudioTrack?.dispose()
        socket?.disconnect()
        socket = null
        peerConnectionFactory?.dispose()
        Log.d(TAG, "Disconnected")
    }

    fun isInCall(): Boolean {
        return peerConnection?.connectionState() == PeerConnection.PeerConnectionState.CONNECTED
    }

    fun getCurrentRoomId(): String? = currentRoomId

    companion object {
        private const val TAG = "AudioOnlyWebRTC"
    }
}
