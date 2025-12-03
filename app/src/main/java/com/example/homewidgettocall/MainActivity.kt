package com.example.homewidgettocall

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.homewidgettocall.model.Friend
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvFriends: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var fabAddFriend: FloatingActionButton
    private lateinit var btnLogout: ImageButton
    private lateinit var friendsAdapter: FriendsAdapter
    private val friendsList = mutableListOf<Friend>()
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "HomeWidgetToCallPrefs"
        private const val KEY_FIRST_FRIEND_FCM_TOKEN = "first_friend_fcm_token"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Initialize views
        rvFriends = findViewById(R.id.rvFriends)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        fabAddFriend = findViewById(R.id.fabAddFriend)
        btnLogout = findViewById(R.id.btnLogout)

        // Setup adapter
        friendsAdapter = FriendsAdapter(friendsList) { friend ->
            onFriendClicked(friend)
        }
        rvFriends.adapter = friendsAdapter

        // FAB click - Add friend
        fabAddFriend.setOnClickListener {
            showAddFriendDialog()
        }

        // Logout button
        btnLogout.setOnClickListener {
            logout()
        }

        // Load friends
        loadFriends()
    }

    private fun loadFriends() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get friends list from Firestore
                    val friendsData = document.get("friends") as? List<Map<String, String>> ?: emptyList()
                    
                    friendsList.clear()
                    friendsData.forEach { friendMap ->
                        val email = friendMap["email"] ?: ""
                        val fcmToken = friendMap["fcmToken"] ?: ""
                        friendsList.add(Friend(email, fcmToken))
                    }
                    
                    // Save first friend's FCM token to SharedPreferences
                    if (friendsList.isNotEmpty()) {
                        val firstFriendToken = friendsList[0].fcmToken
                        saveFirstFriendToken(firstFriendToken)
                        Log.d(TAG, "First friend FCM token saved: ${firstFriendToken.take(20)}...")
                    } else {
                        // Clear token if no friends
                        clearFirstFriendToken()
                        Log.d(TAG, "No friends, cleared FCM token from SharedPreferences")
                    }
                    
                    friendsAdapter.updateFriends(friendsList)
                    updateEmptyState()
                    
                    Log.d(TAG, "Loaded ${friendsList.size} friends")
                } else {
                    Log.w(TAG, "User document does not exist")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading friends", e)
                Toast.makeText(this, "Error loading friends: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveFirstFriendToken(token: String) {
        sharedPreferences.edit().apply {
            putString(KEY_FIRST_FRIEND_FCM_TOKEN, token)
            apply()
        }
    }

    private fun clearFirstFriendToken() {
        sharedPreferences.edit().apply {
            remove(KEY_FIRST_FRIEND_FCM_TOKEN)
            apply()
        }
    }

    fun getFirstFriendToken(): String? {
        return sharedPreferences.getString(KEY_FIRST_FRIEND_FCM_TOKEN, null)
    }

    private fun showAddFriendDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_friend, null)
        val etFriendEmail = dialogView.findViewById<EditText>(R.id.etFriendEmail)

        AlertDialog.Builder(this)
            .setTitle("Add Friend")
            .setMessage("Enter your friend's email address")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val friendEmail = etFriendEmail.text.toString().trim()
                if (friendEmail.isNotEmpty()) {
                    addFriend(friendEmail)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addFriend(friendEmail: String) {
        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(friendEmail).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserEmail = auth.currentUser?.email ?: return

        // Check if trying to add self
        if (friendEmail == currentUserEmail) {
            Toast.makeText(this, "You cannot add yourself as a friend", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if friend already exists
        if (friendsList.any { it.email == friendEmail }) {
            Toast.makeText(this, "Friend already added", Toast.LENGTH_SHORT).show()
            return
        }

        // Search for user in Firestore by email
        firestore.collection("users")
            .whereEqualTo("email", friendEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "User not found with email: $friendEmail", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Get the first matching user
                val friendDocument = documents.documents[0]
                val friendFcmToken = friendDocument.getString("fcmToken") ?: ""

                // Create friend object
                val friendData = hashMapOf(
                    "email" to friendEmail,
                    "fcmToken" to friendFcmToken
                )

                // Add friend to current user's friends list
                firestore.collection("users")
                    .document(currentUserId)
                    .update("friends", FieldValue.arrayUnion(friendData))
                    .addOnSuccessListener {
                        Log.d(TAG, "Friend added successfully")
                        Toast.makeText(this, "Friend added: $friendEmail", Toast.LENGTH_SHORT).show()
                        
                        // Add to local list and update UI
                        friendsList.add(Friend(friendEmail, friendFcmToken))
                        friendsAdapter.updateFriends(friendsList)
                        updateEmptyState()
                        
                        // Update first friend token if this is the first friend
                        if (friendsList.size == 1) {
                            saveFirstFriendToken(friendFcmToken)
                            Log.d(TAG, "First friend added, token saved to SharedPreferences")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error adding friend", e)
                        Toast.makeText(this, "Error adding friend: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error searching for user", e)
                Toast.makeText(this, "Error searching for user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onFriendClicked(friend: Friend) {
        // Show friend details
        AlertDialog.Builder(this)
            .setTitle("Friend Details")
            .setMessage("Email: ${friend.email}\n\nFCM Token: ${friend.fcmToken}")
            .setPositiveButton("OK", null)
            .setNegativeButton("Remove Friend") { _, _ ->
                removeFriend(friend)
            }
            .show()
    }

    private fun removeFriend(friend: Friend) {
        val currentUserId = auth.currentUser?.uid ?: return

        val friendData = hashMapOf(
            "email" to friend.email,
            "fcmToken" to friend.fcmToken
        )

        firestore.collection("users")
            .document(currentUserId)
            .update("friends", FieldValue.arrayRemove(friendData))
            .addOnSuccessListener {
                Log.d(TAG, "Friend removed successfully")
                Toast.makeText(this, "Friend removed: ${friend.email}", Toast.LENGTH_SHORT).show()
                
                // Remove from local list and update UI
                val wasFirstFriend = friendsList.indexOf(friend) == 0
                friendsList.remove(friend)
                friendsAdapter.updateFriends(friendsList)
                updateEmptyState()
                
                // Update SharedPreferences if the removed friend was the first one
                if (wasFirstFriend) {
                    if (friendsList.isNotEmpty()) {
                        // Save new first friend's token
                        saveFirstFriendToken(friendsList[0].fcmToken)
                        Log.d(TAG, "First friend removed, updated with new first friend token")
                    } else {
                        // No friends left, clear token
                        clearFirstFriendToken()
                        Log.d(TAG, "Last friend removed, cleared token from SharedPreferences")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing friend", e)
                Toast.makeText(this, "Error removing friend: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        if (friendsList.isEmpty()) {
            rvFriends.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            rvFriends.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun logout() {
        auth.signOut()
        
        // Clear SharedPreferences on logout
        clearFirstFriendToken()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
