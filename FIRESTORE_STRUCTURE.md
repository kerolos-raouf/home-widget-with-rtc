# 📊 Firestore Data Structure - Quick Reference

## 🗂️ Database Structure

```
Firestore Database
│
└── 📁 users (collection)
    │
    ├── 📄 {userId1} (document)
    │   ├── email: "john@example.com"
    │   └── fcmToken: "dXZ1234xyz..."
    │
    ├── 📄 {userId2} (document)
    │   ├── email: "jane@example.com"
    │   └── fcmToken: "aBc567def..."
    │
    └── 📄 {userId3} (document)
        ├── email: "bob@example.com"
        └── fcmToken: "xyz890abc..."
```

---

## 📝 Document Structure

### Path: `users/{userId}`

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| email | string | User's email address | "user@example.com" |
| fcmToken | string | Firebase Cloud Messaging token | "dXZ1234567..." |

---

## 🔄 When Data is Created

```
User Signs Up
    ↓
Firebase Auth Creates Account
    ↓
Get User ID: "abc123def456"
    ↓
Get FCM Token: "dXZ1234..."
    ↓
Firestore Write:
    users/abc123def456
        ├── email: "user@example.com"
        └── fcmToken: "dXZ1234..."
```

---

## 📍 Example: Real Data

### In Firebase Console:

**Collection**: `users`

**Document ID**: `kJ9sD2fH8gT4pL6m`
```json
{
  "email": "test@example.com",
  "fcmToken": "dXZwNjI0MjM0NTY3ODkw..."
}
```

**Document ID**: `mN5hG3kP9sT7qR2x`
```json
{
  "email": "another@example.com",
  "fcmToken": "aBcDeFgHiJkLmNoPqRsT..."
}
```

---

## 🔍 How to Access Data

### Get Current User's Data
```kotlin
val userId = FirebaseAuth.getInstance().currentUser?.uid
firestore.collection("users")
    .document(userId!!)
    .get()
    .addOnSuccessListener { document ->
        val email = document.getString("email")
        val fcmToken = document.getString("fcmToken")
    }
```

### Get Any User's Data (if permitted)
```kotlin
firestore.collection("users")
    .document(targetUserId)
    .get()
    .addOnSuccessListener { document ->
        val email = document.getString("email")
        val fcmToken = document.getString("fcmToken")
    }
```

### Update FCM Token
```kotlin
val userId = FirebaseAuth.getInstance().currentUser?.uid
firestore.collection("users")
    .document(userId!!)
    .update("fcmToken", newToken)
```

---

## 🎯 Use Cases

### 1. Send Push Notification to User
```kotlin
// Step 1: Get user's FCM token from Firestore
firestore.collection("users")
    .document(targetUserId)
    .get()
    .addOnSuccessListener { document ->
        val fcmToken = document.getString("fcmToken")
        
        // Step 2: Send notification using token
        sendPushNotification(fcmToken, title, message)
    }
```

### 2. Get User's Email
```kotlin
firestore.collection("users")
    .document(userId)
    .get()
    .addOnSuccessListener { document ->
        val email = document.getString("email")
        // Display email in profile
    }
```

### 3. List All Users (Admin)
```kotlin
firestore.collection("users")
    .get()
    .addOnSuccessListener { documents ->
        for (document in documents) {
            val email = document.getString("email")
            val fcmToken = document.getString("fcmToken")
            // Process user data
        }
    }
```

---

## 🔐 Security Rules

**Current Setup** (Recommended):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      // Users can only read/write their own data
      allow read, write: if request.auth != null 
                         && request.auth.uid == userId;
    }
  }
}
```

**What This Means**:
- ✅ User can read their own document
- ✅ User can update their own document
- ❌ User CANNOT read other users' documents
- ❌ User CANNOT update other users' documents

---

## 🧪 Testing in Firebase Console

### View Your Data:
1. Go to: https://console.firebase.google.com/
2. Select your project
3. Click **Firestore Database** in left menu
4. You'll see:
   ```
   📁 users
   ├── 📄 abc123... (your userId)
   │   ├── email: "your@email.com"
   │   └── fcmToken: "xyz..."
   ```

### What You'll See After Sign Up:
```
Before Sign Up:
  (Empty database)

After Sign Up:
  📁 users
  └── 📄 kJ9sD2fH8gT4pL6m
      ├── email: "test@example.com"
      └── fcmToken: "dXZwNjI0MjM0..."
```

---

## ✅ Quick Checklist

After user signs up, verify:
- [ ] Document created in `users` collection
- [ ] Document ID matches Firebase Auth userId
- [ ] `email` field contains user's email
- [ ] `fcmToken` field contains valid token (or empty string)

---

## 🎉 Summary

| Item | Value |
|------|-------|
| **Collection** | `users` |
| **Document ID** | Firebase Auth userId |
| **Fields** | `email`, `fcmToken` |
| **Created On** | User sign up |
| **Used For** | Push notifications, user data |

---

Your Firestore is ready! 🚀
