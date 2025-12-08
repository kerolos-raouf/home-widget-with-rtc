package com.example.homewidgettocall

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.homewidgettocall.data.PreferencesHelper
import com.example.homewidgettocall.widget.WidgetProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var tvSignUp: TextView
    private lateinit var loadingOverlay: RelativeLayout
    private var isPasswordVisible = false
    
    // Firebase Auth instance
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        tvSignUp = findViewById(R.id.tvSignUp)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        // Submit button click listener
        btnSubmit.setOnClickListener {
            handleSignIn()
        }

        // Toggle password visibility
        btnTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Navigate to Sign Up screen
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, navigate to MainActivity
            navigateToMain()
        }
    }

    private fun handleSignIn() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate inputs
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        // Show loading
        showLoading(true)

        // Sign in with Firebase
        signInWithFirebase(email, password)
    }

    private fun signInWithFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Hide loading
                showLoading(false)

                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    
                    if (user != null) {
                        // Update FCM token in Firestore
                        updateFcmTokenInFirestore(user.uid)
                    } else {
                        // No user, just proceed to main
                        proceedToMain()
                    }
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    handleSignInError(task.exception)
                }
            }
    }

    private fun handleSignInError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidUserException -> {
                "No account found with this email. Please sign up first."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Invalid email or password. Please try again."
            }
            else -> {
                "Sign in failed: ${exception?.message ?: "Unknown error"}"
            }
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isPasswordVisible = false
        } else {
            // Show password
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            isPasswordVisible = true
        }
        // Move cursor to end of text
        etPassword.setSelection(etPassword.text.length)
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !show
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(this, WidgetProvider::class.java)
        )
        
        val intent = Intent(this, WidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        sendBroadcast(intent)
        
        Log.d(TAG, "Widget update broadcast sent from LoginActivity")
    }

    private fun updateFcmTokenInFirestore(userId: String) {
        // Get current FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val fcmToken = tokenTask.result
                Log.d(TAG, "FCM token retrieved: ${fcmToken.take(20)}...")
                
                // Update token in Firestore
                firestore.collection("users")
                    .document(userId)
                    .update("fcmToken", fcmToken)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token updated in Firestore successfully")
                        // Proceed to main after token update
                        proceedToMain()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating FCM token in Firestore", e)
                        // Still proceed to main even if token update fails
                        proceedToMain()
                    }
            } else {
                Log.w(TAG, "Fetching FCM token failed", tokenTask.exception)
                // Proceed to main even if token fetch fails
                proceedToMain()
            }
        }
    }

    private fun proceedToMain() {
        // Set logged in state
        PreferencesHelper.setLoggedIn(this, true)
        Log.d(TAG, "User logged in state saved")
        
        // Update widget immediately after login to show correct state
        updateWidget()
        Log.d(TAG, "Widget updated after login")
        
        val user = auth.currentUser
        Toast.makeText(
            this,
            "Login Successful!\nWelcome ${user?.email}",
            Toast.LENGTH_SHORT
        ).show()

        navigateToMain()
    }
}
