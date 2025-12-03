# 🚀 Quick Fix Guide - gRPC Error

## Problem
App crashes on sign up with:
```
NoClassDefFoundError: Lio/grpc/InternalGlobalInterceptors
```

---

## ✅ Solution (3 Steps)

### Step 1: Sync Gradle
The `build.gradle.kts` and `libs.versions.toml` files have been updated.

**In Android Studio:**
1. Click "Sync Now" banner at the top
   OR
2. Go to: **File → Sync Project with Gradle Files**

### Step 2: Clean Build
```
Build → Clean Project
```
Wait for it to complete.

### Step 3: Rebuild
```
Build → Rebuild Project
```
Wait for it to complete.

---

## 🧪 Test It

1. **Run the app**
2. **Click "Sign Up"**
3. **Enter email:** test123@example.com
4. **Click arrow**
5. **Enter password:** password123
6. **Click arrow**
7. ✅ **Should work now!**

---

## What Was Changed?

1. ✅ **Added MultiDex support** (handles large dependencies)
2. ✅ **Excluded conflicting gRPC dependencies**
3. ✅ **Downgraded Firebase BOM** to stable version (33.5.1)
4. ✅ **Added packaging exclusions** for META-INF files

---

## Still Not Working?

### Try This:
```
File → Invalidate Caches → Invalidate and Restart
```

Then repeat:
1. Sync Gradle
2. Clean Project
3. Rebuild Project
4. Run app

---

## 🎯 Expected Behavior

**Before Fix:**
- ❌ App crashes on sign up
- ❌ gRPC error in logs

**After Fix:**
- ✅ Sign up works
- ✅ Firestore writes succeed
- ✅ No gRPC errors

---

Need more details? See `GRPC_FIX.md`
