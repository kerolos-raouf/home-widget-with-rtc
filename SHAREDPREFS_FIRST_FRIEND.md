# 💾 SharedPreferences - First Friend FCM Token

## ✅ What Was Implemented

### Feature: Save First Friend's FCM Token

When friends are loaded, the **first friend's FCM token** is automatically saved to **SharedPreferences**.

---

## 🎯 Why Save to SharedPreferences?

1. **Quick Access** - No need to query Firestore every time
2. **Offline Access** - Available even without internet
3. **Widget Access** - Can be used by home screen widgets
4. **Persistent** - Survives app restarts
5. **Fast** - Instant retrieval

---

## 🔄 When Token is Saved/Updated

### Saved:
1. ✅ **On app start** - When friends list loads
2. ✅ **First friend added** - When adding first friend
3. ✅ **First friend removed** - Updates with new first friend's token

### Cleared:
1. ✅ **No friends** - When last friend is removed
2. ✅ **On logout** - User signs out

---

## 📊 How It Works

### Load Friends:
```kotlin
loadFriends()
    ↓
Fetch from Firestore
    ↓
Parse friends list
    ↓
If friends exist:
    → Save friendsList[0].fcmToken to SharedPreferences ✅
Else:
    → Clear token from SharedPreferences
```

### Add First Friend:
```kotlin
addFriend()
    ↓
Friend added to Firestore
    ↓
If this is first friend (friendsList.size == 1):
    → Save token to SharedPreferences ✅
```

### Remove Friend:
```kotlin
removeFriend()
    ↓
Was this the first friend?
    ↓ YES
    Are there more friends?
        ↓ YES
        → Save new first friend's token ✅
        ↓ NO
        → Clear token from SharedPreferences
    ↓ NO
    → No action needed
```

---

## 🔑 SharedPreferences Keys

| Key | Type | Description |
|-----|------|-------------|
| `first_friend_fcm_token` | String | First friend's FCM token |

**Preferences Name**: `HomeWidgetToCallPrefs`

---

## 📝 Code Implementation

### MainActivity.kt

```kotlin
// Save token
private fun saveFirstFriendToken(token: String) {
    sharedPreferences.edit().apply {
        putString(KEY_FIRST_FRIEND_FCM_TOKEN, token)
        apply()
    }
}

// Get token
fun getFirstFriendToken(): String? {
    return sharedPreferences.getString(KEY_FIRST_FRIEND_FCM_TOKEN, null)
}

// Clear token
private fun clearFirstFriendToken() {
    sharedPreferences.edit().apply {
        remove(KEY_FIRST_FRIEND_FCM_TOKEN)
        apply()
    }
}
```

### PreferencesHelper.kt (New File)

A helper class to access the token from anywhere:

```kotlin
// Save
PreferencesHelper.saveFirstFriendToken(context, token)

// Get
val token = PreferencesHelper.getFirstFriendToken(context)

// Check if exists
if (PreferencesHelper.hasFirstFriendToken(context)) {
    // Token exists
}

// Clear
PreferencesHelper.clearFirstFriendToken(context)
```

---

## 🧪 Testing

### Test 1: Add First Friend
```
1. Login with no friends
2. Add a friend: friend@example.com
3. Check Logcat: "First friend FCM token saved: ..."
4. ✅ Token saved
```

### Test 2: Verify Token in SharedPreferences
```kotlin
// In MainActivity
val token = getFirstFriendToken()
Log.d("TEST", "Stored token: $token")
```

### Test 3: Remove First Friend
```
1. Have 2+ friends
2. Remove first friend
3. Check Logcat: "First friend removed, updated with new first friend token"
4. ✅ New first friend's token saved
```

### Test 4: Remove Last Friend
```
1. Have 1 friend
2. Remove that friend
3. Check Logcat: "Last friend removed, cleared token from SharedPreferences"
4. ✅ Token cleared
```

### Test 5: Logout
```
1. Have friends
2. Click logout
3. Token cleared from SharedPreferences
4. ✅ Token removed
```

### Test 6: App Restart
```
1. Add friends
2. Close app completely
3. Reopen app
4. Check token still exists
5. ✅ Token persists
```

---

## 📱 Usage Examples

### Example 1: In MainActivity
```kotlin
val token = getFirstFriendToken()
if (token != null) {
    Log.d(TAG, "First friend token: $token")
    // Use token for push notifications
} else {
    Log.d(TAG, "No first friend token available")
}
```

### Example 2: In Widget
```kotlin
class MyWidget : AppWidgetProvider() {
    override fun onUpdate(...) {
        val token = PreferencesHelper.getFirstFriendToken(context)
        if (token != null) {
            // Send notification to first friend
            sendNotification(token)
        }
    }
}
```

### Example 3: In Service
```kotlin
class VoiceRecorderService : Service() {
    private fun sendToFirstFriend() {
        val token = PreferencesHelper.getFirstFriendToken(this)
        if (token != null) {
            // Send voice message to first friend
            sendVoiceMessage(token)
        }
    }
}
```

### Example 4: Check Before Sending
```kotlin
fun callFirstFriend() {
    if (PreferencesHelper.hasFirstFriendToken(this)) {
        val token = PreferencesHelper.getFirstFriendToken(this)
        // Make call using token
        initiateCall(token!!)
    } else {
        Toast.makeText(this, "Please add a friend first", Toast.LENGTH_SHORT).show()
    }
}
```

---

## 🔐 Security Notes

### ✅ Safe:
- FCM tokens are **public identifiers**
- Can be stored in SharedPreferences
- Used to send push notifications
- Not sensitive data

### ⚠️ Not for Storing:
- Passwords
- Private keys
- Personal information
- Payment data

---

## 📊 Data Flow

```
User Opens App
    ↓
MainActivity.onCreate()
    ↓
loadFriends()
    ↓
Firestore returns friends list
    ↓
Parse friends
    ↓
If friends.isNotEmpty():
    fcmToken = friendsList[0].fcmToken
    SharedPreferences.save(fcmToken) ✅
    ↓
Widget can now access token via:
PreferencesHelper.getFirstFriendToken()
```

---

## 🎯 Use Cases

### 1. Home Widget
Display first friend and allow quick call/message via widget

### 2. Quick Actions
Send notification to first friend from notification shade

### 3. Voice Commands
"Call my first friend" using stored token

### 4. Background Service
Send automated messages to first friend

### 5. Wear OS
Access first friend from smartwatch

---

## 🐛 Troubleshooting

**Q: Token is null?**
A: Check if user has added any friends

**Q: Token not updating?**
A: Verify `saveFirstFriendToken()` is called after adding/removing friends

**Q: Token cleared on app restart?**
A: SharedPreferences persist across restarts - check if logout was called

**Q: How to debug?**
A: Add logs:
```kotlin
Log.d(TAG, "Saved token: ${PreferencesHelper.getFirstFriendToken(this)}")
```

---

## 📝 Important Notes

1. **First Friend Only** - Only the first friend's token is saved
2. **Automatic Updates** - Token automatically updates when:
   - Friends list loads
   - First friend is added
   - First friend is removed
3. **Cleared on Logout** - Token is cleared when user signs out
4. **Persistent** - Survives app restarts
5. **Fast Access** - No Firestore query needed

---

## ✅ Summary

| Event | Action |
|-------|--------|
| **Load friends** | Save first friend's token |
| **Add first friend** | Save token |
| **Remove first friend** | Update with new first friend's token |
| **Remove last friend** | Clear token |
| **Logout** | Clear token |
| **App restart** | Token persists |

---

## 🚀 Files Created/Modified

### New Files:
1. `PreferencesHelper.kt` - Helper class for easy access

### Modified Files:
1. `MainActivity.kt` - Added SharedPreferences logic

---

Your first friend's FCM token is now saved to SharedPreferences! 🎉

Use `PreferencesHelper.getFirstFriendToken(context)` anywhere in your app to access it!
