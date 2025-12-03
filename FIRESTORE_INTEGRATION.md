# 🔥 Firestore Integration - User Data Storage

## ✅ What Was Added

### SignUpActivity.kt - UPDATED with Firestore
Now creates a user document in Firestore when signing up:

**Path**: `users/{userId}`

**Data Stored**:
```json
{
  "email": "user@example.com",
  "fcmToken": "dXZ1234..."
}
```

---

## 🗂️ Firestore Structure

```
Firestore Database
└── users (collection)
    └── {userId} (document)
        ├── email: string
        └── fcmToken: string
```

### Example:
```
users/
├── abc123def456 (userId)
│   ├── email: "john@example.com"
│   └── fcmToken: "dXZ1234xyz..."
├── xyz789ghi012 (userId)
│   ├── email: "jane@example.com"
│   └── fcmToken: "aBc567def..."
```

---

## 🔄 Sign Up Flow (Updated)

```
User enters email & password
    ↓
Firebase Auth creates account
    ↓ (Success)
Get Firebase User ID
    ↓
Get FCM Token
    ↓
Create Firestore document:
  users/{userId}
    ├── email
    └── fcmToken
    ↓ (Success)
Navigate to MainActivity
```

---

## 🎯 What Happens on Sign Up

1. **User enters credentials** → Email & Password validated
2. **Firebase Auth creates account** → Returns userId
3. **FCM Token fetched** → Gets device push notification token
4. **Firestore document created** → Saves user data at `users/{userId}`
5. **Success notification** → Shows toast message
6. **Navigate to MainActivity** → User is logged in

---

## 📋 Code Breakdown

### Getting FCM Token
```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
    val fcmToken = if (tokenTask.isSuccessful) {
        tokenTask.result
    } else {
        "" // Empty string if token fetch fails
    }
    // ... use token
}
```

### Writing to Firestore
```kotlin
val userData = hashMapOf(
    "email" to email,
    "fcmToken" to fcmToken
)

firestore.collection("users")
    .document(userId)
    .set(userData)
    .addOnSuccessListener {
        // Success - user data saved
    }
    .addOnFailureListener { e ->
        // Error - but auth still succeeded
    }
```

---

## 🔐 Security

### Firestore Security Rules

Make sure your Firestore has proper security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only read/write their own document
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

This ensures:
- ✅ Only authenticated users can access user documents
- ✅ Users can only read/write their own data
- ✅ Other users cannot see each other's data

---

## 🧪 Testing

### Test Sign Up with Firestore:

1. **Run the app**
2. **Sign up** with new email/password
3. **Check Firebase Console**:
   - Go to: https://console.firebase.google.com/
   - Click "Firestore Database"
   - Navigate to `users` collection
   - ✅ See your userId document with email and fcmToken

### Expected Result in Firestore:
```
users/
└── a1b2c3d4e5f6 (your userId)
    ├── email: "yourtest@example.com"
    └── fcmToken: "dXZ1234567890xyz..."
```

---

## 📱 Use Cases for FCM Token

The FCM token stored in Firestore can be used for:

1. **Push Notifications** - Send notifications to specific users
2. **User Status** - Track active/inactive users
3. **Multi-device Support** - Manage multiple devices per user
4. **Targeted Messaging** - Send personalized notifications

### Example: Send Notification to User
```kotlin
// Get user's FCM token from Firestore
firestore.collection("users")
    .document(targetUserId)
    .get()
    .addOnSuccessListener { document ->
        val fcmToken = document.getString("fcmToken")
        // Use token to send push notification
        sendNotificationToToken(fcmToken)
    }
```

---

## 🚀 Optional Enhancements

### Add More User Fields

Update the userData map to include:

```kotlin
val userData = hashMapOf(
    "email" to email,
    "fcmToken" to fcmToken,
    "createdAt" to FieldValue.serverTimestamp(),
    "lastLogin" to FieldValue.serverTimestamp(),
    "displayName" to "", // Can be updated later
    "photoUrl" to "",    // Can be updated later
    "isOnline" to true
)
```

### Update FCM Token on Login

Add to LoginActivity after successful sign in:

```kotlin
private fun updateFcmToken(userId: String) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val fcmToken = task.result
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", fcmToken)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated")
                }
        }
    }
}
```

### Add User Presence

Track when user is online:

```kotlin
// When user signs in
firestore.collection("users")
    .document(userId)
    .update(mapOf(
        "isOnline" to true,
        "lastSeen" to FieldValue.serverTimestamp()
    ))

// When user signs out
firestore.collection("users")
    .document(userId)
    .update("isOnline", false)
```

---

## ⚠️ Error Handling

### If Firestore write fails:
- ✅ Auth account is still created
- ✅ User can still sign in
- ✅ Shows error message but continues
- ✅ Can retry Firestore write later

### If FCM token fetch fails:
- ✅ Empty string stored for fcmToken
- ✅ Can be updated later when available
- ✅ User signup still succeeds

---

## 🐛 Troubleshooting

**Q: User document not created in Firestore?**
A: Check Logcat for "SignUpActivity" errors, verify Firestore is enabled in Firebase Console

**Q: FCM token is empty?**
A: Normal on first run, token will be generated after app restart

**Q: Permission denied error?**
A: Update Firestore security rules (see Security section above)

**Q: Can't see users collection?**
A: Wait a few seconds after signup and refresh Firebase Console

---

## 📊 Data Flow Diagram

```
SignUpActivity
    ↓
Firebase Auth
    ↓ (userId)
Firebase Messaging
    ↓ (fcmToken)
Firestore
    ↓ (users/{userId})
Success → MainActivity
```

---

## 🎯 Summary

✅ **User document created** in `users/{userId}`
✅ **Email stored** in Firestore
✅ **FCM token stored** for push notifications
✅ **Error handling** for Firestore failures
✅ **Security** through Firestore rules
✅ **Ready for push notifications**

---

## 📚 Related Files

- `SignUpActivity.kt` - Creates user document
- `FIREBASE_AUTH_COMPLETE.md` - Auth documentation
- `FIREBASE_INTEGRATION_SUCCESS.md` - Complete guide

---

Your app now stores user data in Firestore! 🎉

Next Steps:
- [ ] Update FCM token on login
- [ ] Add user profile screen
- [ ] Implement push notifications
- [ ] Add user presence tracking
