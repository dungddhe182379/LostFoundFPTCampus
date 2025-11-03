# 🔧 BACKEND FIX REQUEST - Karma Update Issue

**Date:** November 4, 2025  
**Priority:** HIGH 🔴  
**Status:** ⏳ Pending Backend Fix

---

## 📋 SUMMARY

Current backend implementation **BLOCKS** karma field updates via `PUT /api/lostfound/user/profile` endpoint. This prevents the QR handover karma reward system from working properly.

---

## ❌ CURRENT PROBLEM

### Issue 1: Karma field is ignored in PUT /user/profile

**Test Results:**
```powershell
# Test với PowerShell
$body = @{name="Test User"; karma=50} | ConvertTo-Json
Invoke-RestMethod -Uri "https://vietsuky.com/api/lostfound/user/profile" `
                  -Method PUT `
                  -Headers @{Authorization="Bearer $token"} `
                  -Body $body `
                  -ContentType "application/json"

# Result:
✅ HTTP 200 OK
✅ name updated to "Test User"
❌ karma still 0 (NOT updated to 50)
```

**Root Cause:**
Backend code filters out `karma` field for security reasons, preventing any updates to karma through profile endpoint.

### Issue 2: confirm-handover API doesn't auto-update karma

**According to API Documentation:**
> ✅ **Karma +10** awarded to both giver and receiver

**Reality:**
```
POST /api/lostfound/items/{itemId}/confirm-handover
✅ Updates item status to "returned"
✅ Sets lostUserId, foundUserId, returnedUserId
❌ Does NOT update karma for giver (foundUserId)
❌ Does NOT update karma for receiver (returnedUserId)
```

---

## 🎯 REQUESTED CHANGES

### Option 1: Allow karma update via PUT /user/profile (RECOMMENDED)

**Endpoint:** `PUT /api/lostfound/user/profile`

**Change Required:**
Allow updating `karma` field in User profile for **ANY user**, not just current authenticated user.

**Backend Code Changes Needed:**

```java
// Current (Blocked):
@PutMapping("/user/profile")
public ResponseEntity<ApiResponse<User>> updateProfile(
    @RequestHeader("Authorization") String token,
    @RequestBody User user
) {
    Long currentUserId = jwtService.getUserIdFromToken(token);
    User currentUser = userRepository.findById(currentUserId).orElseThrow();
    
    // Update allowed fields
    currentUser.setName(user.getName());
    currentUser.setPhone(user.getPhone());
    currentUser.setAvatarUrl(user.getAvatarUrl());
    // ❌ currentUser.setKarma(user.getKarma()); // NOT ALLOWED
    
    userRepository.save(currentUser);
    return ResponseEntity.ok(ApiResponse.success(currentUser));
}
```

```java
// Requested (Allow karma update):
@PutMapping("/user/profile")
public ResponseEntity<ApiResponse<User>> updateProfile(
    @RequestHeader("Authorization") String token,
    @RequestBody User user
) {
    Long currentUserId = jwtService.getUserIdFromToken(token);
    
    // ✅ NEW: Allow updating ANY user if ID is provided
    Long targetUserId = user.getId() != null ? user.getId() : currentUserId;
    User targetUser = userRepository.findById(targetUserId).orElseThrow();
    
    // Update allowed fields
    if (user.getName() != null) targetUser.setName(user.getName());
    if (user.getPhone() != null) targetUser.setPhone(user.getPhone());
    if (user.getAvatarUrl() != null) targetUser.setAvatarUrl(user.getAvatarUrl());
    
    // ✅ NEW: Allow karma updates
    if (user.getKarma() != null) {
        targetUser.setKarma(user.getKarma());
    }
    
    userRepository.save(targetUser);
    return ResponseEntity.ok(ApiResponse.success("Profile updated", targetUser));
}
```

**Why This Approach:**
- ✅ Simple and straightforward
- ✅ Allows frontend to manually control karma updates
- ✅ Flexible for future karma features (penalties, bonuses, etc.)
- ✅ Works with current Android implementation

---

### Option 2: Auto-update karma in confirm-handover API

**Endpoint:** `POST /api/lostfound/items/{itemId}/confirm-handover`

**Change Required:**
Automatically award +10 karma to both giver and receiver when handover is confirmed.

**Backend Code Changes Needed:**

```java
@PostMapping("/items/{itemId}/confirm-handover")
public ResponseEntity<ApiResponse<LostItem>> confirmHandover(
    @RequestHeader("Authorization") String token,
    @PathVariable Long itemId,
    @RequestBody ConfirmHandoverRequest request
) {
    // ... validate token, check expiration, etc ...
    
    // Get item
    LostItem item = itemRepository.findById(itemId).orElseThrow();
    
    // Update item status
    item.setStatus("returned");
    item.setReturnedUserId(scannerId);
    itemRepository.save(item);
    
    // ✅ NEW: Award karma to both users
    Long foundUserId = item.getFoundUserId();  // Giver
    Long returnedUserId = item.getReturnedUserId();  // Receiver
    
    // Award +10 karma to giver
    if (foundUserId != null) {
        User giver = userRepository.findById(foundUserId).orElse(null);
        if (giver != null) {
            giver.setKarma(giver.getKarma() + 10);
            userRepository.save(giver);
            logger.info("✅ Awarded +10 karma to giver (userId={})", foundUserId);
        }
    }
    
    // Award +10 karma to receiver
    if (returnedUserId != null) {
        User receiver = userRepository.findById(returnedUserId).orElse(null);
        if (receiver != null) {
            receiver.setKarma(receiver.getKarma() + 10);
            userRepository.save(receiver);
            logger.info("✅ Awarded +10 karma to receiver (userId={})", returnedUserId);
        }
    }
    
    // Create history record
    History history = new History();
    history.setItemId(itemId);
    history.setGiverId(foundUserId);
    history.setReceiverId(returnedUserId);
    history.setQrToken(request.getQrToken());
    historyRepository.save(history);
    
    return ResponseEntity.ok(ApiResponse.success("Handover confirmed successfully", item));
}
```

**Why This Approach:**
- ✅ Matches API documentation behavior
- ✅ Centralized karma logic in backend
- ✅ Automatic and consistent
- ✅ Frontend doesn't need to worry about karma updates

---

## 📊 COMPARISON

| Feature | Option 1 (Allow PUT) | Option 2 (Auto-update) |
|---------|---------------------|------------------------|
| **Complexity** | Simple | Medium |
| **Flexibility** | High | Low |
| **Security** | Need validation | Better |
| **Maintenance** | Frontend | Backend |
| **Documentation Match** | ❌ No | ✅ Yes |
| **Recommended** | ✅ YES | ⭐ BEST |

---

## 🚀 RECOMMENDED SOLUTION

**Implement BOTH options:**

1. **Option 2 (Auto-update in confirm-handover)** - Primary solution
   - Matches API documentation
   - Automatic karma rewards
   - Less frontend code

2. **Option 1 (Allow PUT karma)** - Backup/Fallback
   - For manual karma adjustments
   - For admin features
   - For edge cases

---

## 🧪 TEST CASES REQUIRED

### Test 1: Auto-update karma in confirm-handover
```
1. User A creates item (status=found, foundUserId=A)
2. User B scans QR code
3. POST /items/{id}/confirm-handover with valid token
4. Expected:
   ✅ Item status = "returned"
   ✅ returnedUserId = B
   ✅ User A karma += 10
   ✅ User B karma += 10
   ✅ History record created
```

### Test 2: Manual karma update via PUT
```
1. Login as User A
2. PUT /user/profile with body: {id: 10, karma: 50}
3. Expected:
   ✅ HTTP 200 OK
   ✅ User 10 karma updated to 50
   ✅ Response includes updated karma value
```

### Test 3: Edge case - Same user giver and receiver
```
1. User A creates item (foundUserId=A)
2. User A scans own QR code
3. Expected:
   ❌ HTTP 400 Bad Request
   ❌ Error: "Cannot scan your own item"
   ❌ No karma update
```

---

## 📝 ANDROID APP CURRENT WORKAROUND

The Android app currently implements a **manual karma update** workaround in `QRFragment.java`:

```java
// After confirm handover success:
private void updateKarmaForBothUsers(LostItem item) {
    // 1. GET /user/{foundUserId} → get current karma
    // 2. karma += 10
    // 3. PUT /user/profile with updated karma
    // 4. Repeat for returnedUserId
}
```

**This workaround FAILS because:**
- ❌ Backend ignores karma field in PUT request
- ❌ Karma always returns 0 in response
- ❌ SharedPreferences stores wrong karma value

**Once backend is fixed, this workaround can be removed.**

---

## ⚡ PRIORITY JUSTIFICATION

**Why this is HIGH priority:**

1. **Core Feature Broken** - QR handover karma rewards don't work
2. **User Experience** - Users don't get rewarded for helping others
3. **Gamification System** - Entire karma/leaderboard system is broken
4. **API Documentation Mismatch** - Backend behavior ≠ documented behavior
5. **Already Deployed** - Android app expects this to work

**Impact:**
- ❌ No karma rewards for handovers
- ❌ Leaderboard shows wrong scores
- ❌ User motivation reduced
- ❌ Trust in app features damaged

---

## 📞 CONTACT

**Android Team:**
- Implemented workaround in `QRFragment.java`
- Waiting for backend fix to enable proper karma system
- Can test once backend is updated

**Backend Team Action Items:**
1. ✅ Review this document
2. ✅ Implement Option 2 (auto-update in confirm-handover)
3. ✅ Implement Option 1 (allow karma in PUT profile)
4. ✅ Update API documentation if needed
5. ✅ Deploy to production
6. ✅ Notify Android team for testing

---

## 🔗 RELATED DOCUMENTS

- `LOSTFOUND_API_DOCUMENTATION.md` - Full API documentation
- `QR_HANDOVER_FLOW.md` - QR handover workflow
- `QRFragment.java` - Android implementation with workaround
- `BACKEND_TODO_CONFIRM_HANDOVER.md` - Previous backend tasks

---

**Status:** ⏳ Waiting for Backend Team Response

**Updated:** November 4, 2025  
**Requested By:** Android Development Team  
**Severity:** HIGH 🔴
