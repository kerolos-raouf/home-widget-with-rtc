# ⚡ First Friend Token - Quick Reference

## 🎯 What It Does

Automatically saves the **first friend's FCM token** to SharedPreferences when loading friends.

---

## 🔑 How to Access

### From MainActivity:
```kotlin
val token = getFirstFriendToken()
```

### From Anywhere Else:
```kotlin
val token = PreferencesHelper.getFirstFriendToken(context)
```

### Check if Exists:
```kotlin
if (PreferencesHelper.hasFirstFriendToken(context)) {
    // Token exists
}
```

---

## 🔄 When Token is Saved

✅ **On app start** (friends load)
✅ **When adding first friend**
✅ **When removing first friend** (updates to new first)

## 🗑️ When Token is Cleared

❌ **When last friend is removed**
❌ **On logout**

---

## 📊 Data Stored

**Key**: `first_friend_fcm_token`
**Value**: FCM token string (e.g., "dXZ123...")
**Location**: SharedPreferences (`HomeWidgetToCallPrefs`)

---

## 🧪 Quick Test

```kotlin
// Log the token
Log.d("TEST", "Token: ${PreferencesHelper.getFirstFriendToken(this)}")
```

---

## 💡 Common Uses

1. **Widget** - Quick call/message first friend
2. **Notifications** - Send to first friend
3. **Voice Commands** - "Call my first friend"
4. **Background Services** - Auto-send messages

---

## 📝 Code Examples

### Send Notification to First Friend:
```kotlin
fun notifyFirstFriend() {
    val token = PreferencesHelper.getFirstFriendToken(this)
    if (token != null) {
        sendPushNotification(token, "Hello!")
    } else {
        Toast.makeText(this, "Add a friend first", LENGTH_SHORT).show()
    }
}
```

### Check Before Action:
```kotlin
if (PreferencesHelper.hasFirstFriendToken(this)) {
    // Proceed with action
} else {
    // Show "add friend" prompt
}
```

---

## ✅ Benefits

- ⚡ **Fast** - No Firestore query needed
- 💾 **Persistent** - Survives app restarts
- 🔌 **Offline** - Works without internet
- 🎯 **Easy Access** - One line of code

---

See `SHAREDPREFS_FIRST_FRIEND.md` for detailed documentation.
