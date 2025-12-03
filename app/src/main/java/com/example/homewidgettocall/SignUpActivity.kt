package com.example.homewidgettocall

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SignUpActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etSignUpEmail: EditText
    private lateinit var etSignUpPassword: EditText
    private lateinit var btnNextEmail: ImageButton
    private lateinit var btnCompleteSignUp: ImageButton
    private lateinit var pageEmail: LinearLayout
    private lateinit var pagePassword: LinearLayout
    private var email: String = ""
    
    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        etSignUpEmail = findViewById(R.id.etSignUpEmail)
        etSignUpPassword = findViewById(R.id.etSignUpPassword)
        btnNextEmail = findViewById(R.id.btnNextEmail)
        btnCompleteSignUp = findViewById(R.id.btnCompleteSignUp)
        pageEmail = findViewById(R.id.pageEmail)
        pagePassword = findViewById(R.id.pagePassword)

        // Back button - goes back to previous screen
        btnBack.setOnClickListener {
            if (pagePassword.visibility == View.VISIBLE) {
                // If on password page, go back to email page
                showEmailPage()
            } else {
                // If on email page, go back to LoginActivity
                finish()
            }
        }

        // Next button on email page
        btnNextEmail.setOnClickListener {
            handleEmailNext()
        }

        // Complete sign up button on password page
        btnCompleteSignUp.setOnClickListener {
            handleSignUpComplete()
        }
    }

    private fun handleEmailNext() {
        email = etSignUpEmail.text.toString().trim()

        if (email.isEmpty()) {
            etSignUpEmail.error = "Email is required"
            etSignUpEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etSignUpEmail.error = "Please enter a valid email"
            etSignUpEmail.requestFocus()
            return
        }

        // Show password page
        showPasswordPage()
    }

    private fun handleSignUpComplete() {
        val password = etSignUpPassword.text.toString().trim()

        if (password.isEmpty()) {
            etSignUpPassword.error = "Password is required"
            etSignUpPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etSignUpPassword.error = "Password must be at least 6 characters"
            etSignUpPassword.requestFocus()
            return
        }

        // Disable button to prevent multiple clicks
        btnCompleteSignUp.isEnabled = false
        
        // Create account with Firebase
        createFirebaseAccount(email, password)
    }

    private fun createFirebaseAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    
                    if (user != null) {
                        // Create user document in Firestore
                        createUserInFirestore(user.uid, email)
                    } else {
                        btnCompleteSignUp.isEnabled = true
                        Toast.makeText(this, "Error: User is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Sign up failed
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    btnCompleteSignUp.isEnabled = true
                    handleSignUpError(task.exception)
                }
            }
    }

    private fun createUserInFirestore(userId: String, email: String) {
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
            val fcmToken = if (tokenTask.isSuccessful) {
                tokenTask.result
            } else {
                Log.w(TAG, "Fetching FCM token failed", tokenTask.exception)
                "" // Empty string if token fetch fails
            }

            // Create user data map with empty friends list
            val userData = hashMapOf(
                "email" to email,
                "fcmToken" to fcmToken,
                "friends" to emptyList<Map<String, String>>() // Initialize empty friends list
            )

            // Write to Firestore: users/{userId}
            firestore.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(TAG, "User document created successfully in Firestore")
                    
                    // Re-enable button
                    btnCompleteSignUp.isEnabled = true
                    
                    Toast.makeText(
                        this,
                        "Account created successfully!\nEmail: $email",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navigate to main screen
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error creating user document in Firestore", e)
                    
                    // Re-enable button
                    btnCompleteSignUp.isEnabled = true
                    
                    Toast.makeText(
                        this,
                        "Account created but error saving data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Still navigate to main screen as auth succeeded
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
        }
    }

    private fun handleSignUpError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException -> {
                "Password is too weak. Please use a stronger password."
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Invalid email format. Please check your email."
            }
            is FirebaseAuthUserCollisionException -> {
                "An account with this email already exists. Please sign in instead."
            }
            else -> {
                "Sign up failed: ${exception?.message ?: "Unknown error"}"
            }
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun showEmailPage() {
        pageEmail.visibility = View.VISIBLE
        pagePassword.visibility = View.GONE
    }

    private fun showPasswordPage() {
        pageEmail.visibility = View.GONE
        pagePassword.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        if (pagePassword.visibility == View.VISIBLE) {
            showEmailPage()
        } else {
            super.onBackPressed()
        }
    }
}
