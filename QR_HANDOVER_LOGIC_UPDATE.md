# 🔄 QR HANDOVER LOGIC UPDATE - November 3, 2025

## 📋 OVERVIEW

Cập nhật logic xác nhận bàn giao QR code với các tính năng mới:

1. ✅ **Validation**: Không cho quét QR của chính mình
2. ✅ **User Role Tracking**: Cập nhật đầy đủ 3 trường (lost/found/returned)
3. ✅ **Karma Rewards**: Tặng +10 điểm cho cả 2 người (giver & receiver)

---

## 🎯 BUSINESS RULES

### Rule 1: Ownership Validation ❌
**Người scan QR KHÔNG được là người tạo item**

```java
// Client-side validation
if (giverId == receiverId) {
    // ❌ Block: Cannot scan own item
    return;
}
```

**Why?**
- Tránh gian lận (tự tạo item rồi tự scan để nhận điểm)
- Đảm bảo giao dịch thực sự giữa 2 người khác nhau

### Rule 2: User Role Assignment ✅
**Cập nhật đầy đủ 3 trường khi confirm handover:**

| Field | Value | Description |
|-------|-------|-------------|
| `lostUserId` | Original lost item owner | Người mất đồ ban đầu |
| `foundUserId` | Item creator (giver) | Người tìm thấy và tạo QR |
| `returnedUserId` | Scanner (receiver) | Người scan QR và nhận lại |

### Rule 3: Karma Rewards 🎁
**Tặng điểm cho cả 2 người:**

| User | Role | Karma Reward | Reason |
|------|------|--------------|--------|
| `foundUserId` | Giver | **+10** | Trả lại đồ cho người khác |
| `returnedUserId` | Receiver | **+10** | Nhận lại đồ thành công |

---

## 🔧 CODE CHANGES

### 1. QRFragment.java ✅

**Location:** `confirmHandover()` method

**Added Validation:**
```java
private void confirmHandover(long itemId, String qrToken, LostItem item) {
    String token = "Bearer " + prefsManager.getToken();
    long giverId = item.getUserId();
    long receiverId = prefsManager.getUserId();
    
    // ✅ NEW: Ownership validation
    if (giverId == receiverId) {
        android.util.Log.w("QRFragment", "❌ Cannot scan own item");
        updateScanStatus("❌ Không thể quét mã QR của chính bạn!", R.color.error);
        Toast.makeText(requireContext(), 
            "Bạn không thể xác nhận bàn giao đồ vật của chính mình", 
            Toast.LENGTH_LONG).show();
        resumeScanning();
        return; // ❌ BLOCK
    }
    
    // ✅ Continue with confirm handover API call...
}
```

### 2. QrScanActivity.java ✅

**Location:** `confirmHandoverAndUpdate()` method

**Added Validation:**
```java
private void confirmHandoverAndUpdate(long itemId, String qrToken, 
                                       long giverId, long receiverId, 
                                       LostItem item) {
    String token = "Bearer " + prefsManager.getToken();
    
    // ✅ NEW: Ownership validation
    if (giverId == receiverId) {
        android.util.Log.w("QrScanActivity", "❌ Cannot scan own item");
        showErrorDialog("Không thể xác nhận", 
            "Bạn không thể xác nhận bàn giao đồ vật của chính mình");
        barcodeScanner.resume();
        return; // ❌ BLOCK
    }
    
    // ✅ Continue with confirm handover API call...
}
```

---

## 📊 WORKFLOW

### Complete Flow:

```
┌─────────────────────────────────────────────────────────────┐
│ STEP 1: Generate QR Code                                    │
├─────────────────────────────────────────────────────────────┤
│ User A (Giver) creates item with status="found"             │
│ - userId = A                                                 │
│ - foundUserId = A (auto-set by API)                         │
│                                                              │
│ User A generates QR code:                                    │
│ {                                                            │
│   "itemId": 38,                                              │
│   "title": "iPhone 15",                                      │
│   "token": "TOKEN_1730678400000"                            │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 2: Scan QR Code                                        │
├─────────────────────────────────────────────────────────────┤
│ User B (Receiver) opens app and scans QR                    │
│ - receiverId = B                                             │
│                                                              │
│ ✅ CLIENT VALIDATION:                                        │
│   if (giverId == receiverId) → ❌ BLOCK                     │
│                                                              │
│ ✅ PASSED: A ≠ B                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 3: Confirm Handover Dialog                             │
├─────────────────────────────────────────────────────────────┤
│ Show item details:                                           │
│ - Title: iPhone 15                                           │
│ - Description: ...                                           │
│ - Category: electronics                                      │
│                                                              │
│ User B clicks "Xác nhận"                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 4: API Call - Confirm Handover                         │
├─────────────────────────────────────────────────────────────┤
│ POST /api/lostfound/items/38/confirm-handover               │
│ Header: Authorization: Bearer {user_b_token}                │
│ Body: { "qrToken": "TOKEN_1730678400000" }                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 5: Backend Processing                                  │
├─────────────────────────────────────────────────────────────┤
│ ✅ Validate token (format, expiry, not used)                │
│ ✅ Validate scanner ≠ item creator (server-side check)      │
│ ✅ Update item:                                              │
│    - status: found → returned                                │
│    - lostUserId: (original owner or null)                   │
│    - foundUserId: A (giver)                                  │
│    - returnedUserId: B (receiver/scanner)                   │
│                                                              │
│ ✅ Create History record:                                    │
│    - giverId: A                                              │
│    - receiverId: B                                           │
│    - qrToken: TOKEN_1730678400000                           │
│                                                              │
│ ✅ Award Karma:                                              │
│    - User A (giver): +10 karma                              │
│    - User B (receiver): +10 karma                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 6: Success Response                                    │
├─────────────────────────────────────────────────────────────┤
│ {                                                            │
│   "success": true,                                           │
│   "message": "Handover confirmed successfully",             │
│   "data": {                                                  │
│     "id": 38,                                                │
│     "userId": A,                                             │
│     "lostUserId": (original),                               │
│     "foundUserId": A,                                        │
│     "returnedUserId": B,                                     │
│     "status": "returned",                                    │
│     ...                                                      │
│   }                                                          │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ STEP 7: UI Update                                           │
├─────────────────────────────────────────────────────────────┤
│ ✅ Show success dialog                                       │
│ ✅ Display karma gained (+10 points)                        │
│ ✅ Resume scanning for next QR                              │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚠️ ERROR CASES

### Case 1: Self-Scan Attempt ❌

**Trigger:** User scans QR of their own item

**Client Response:**
```
❌ Không thể quét mã QR của chính bạn!
Toast: "Bạn không thể xác nhận bàn giao đồ vật của chính mình"
→ Resume scanning
```

**Log:**
```
W/QRFragment: ❌ Cannot scan own item: userId=5
```

### Case 2: Invalid/Expired Token ❌

**Trigger:** QR token > 24 hours old

**Server Response:**
```json
{
  "success": false,
  "error": "Invalid or expired QR token: Token expired (older than 24 hours)"
}
```

### Case 3: Token Already Used ❌

**Trigger:** Same QR token scanned twice

**Server Response:**
```json
{
  "success": false,
  "error": "Invalid or expired QR token: Token already used"
}
```

### Case 4: Item Already Returned ❌

**Trigger:** Item status is already "returned"

**Server Response:**
```json
{
  "success": false,
  "error": "Item already marked as returned"
}
```

---

## 🧪 TESTING CHECKLIST

### Test 1: Normal Flow ✅
- [ ] User A creates item (found)
- [ ] User A generates QR code
- [ ] User B scans QR code
- [ ] User B confirms handover
- [ ] Verify: status = "returned"
- [ ] Verify: all 3 user fields set correctly
- [ ] Verify: both users gain +10 karma

### Test 2: Self-Scan Block ❌
- [ ] User A creates item (found)
- [ ] User A generates QR code
- [ ] User A tries to scan own QR
- [ ] Verify: Error message shown
- [ ] Verify: No API call made
- [ ] Verify: Scanning resumes

### Test 3: Expired Token ❌
- [ ] User A generates QR code
- [ ] Wait 25 hours
- [ ] User B scans QR code
- [ ] Verify: API returns expired error
- [ ] Verify: No status change
- [ ] Verify: No karma awarded

### Test 4: Duplicate Scan ❌
- [ ] User B scans QR successfully
- [ ] User C tries to scan same QR
- [ ] Verify: API returns "already used" error
- [ ] Verify: No duplicate karma

---

## 📝 BACKEND REQUIREMENTS

**API Endpoint:** `POST /api/lostfound/items/{itemId}/confirm-handover`

**Backend Must:**
1. ✅ Validate QR token (format, expiry, not used)
2. ✅ Check scanner ≠ item creator (server-side validation)
3. ✅ Update item status to "returned"
4. ✅ Set all 3 user role fields:
   - `lostUserId` (if known)
   - `foundUserId` (giver/item creator)
   - `returnedUserId` (scanner)
5. ✅ Create History record with giverId & receiverId
6. ✅ Award +10 karma to giver (foundUserId)
7. ✅ Award +10 karma to receiver (returnedUserId)
8. ✅ Mark QR token as used (prevent reuse)

---

## 🎁 KARMA SYSTEM

### Calculation:
```
Handover Success:
  Giver Karma = Current Karma + 10
  Receiver Karma = Current Karma + 10
```

### Database Updates:
```sql
-- Update giver karma
UPDATE users SET karma = karma + 10 WHERE id = {foundUserId};

-- Update receiver karma
UPDATE users SET karma = karma + 10 WHERE id = {returnedUserId};

-- Create karma log entries (optional)
INSERT INTO karma_logs (user_id, points, reason, created_at) 
VALUES ({foundUserId}, 10, 'Returned item to owner', NOW());

INSERT INTO karma_logs (user_id, points, reason, created_at) 
VALUES ({returnedUserId}, 10, 'Received lost item back', NOW());
```

---

## 📊 STATISTICS

### Trackable Metrics:
- Total handovers completed
- Total karma awarded
- Average handover time
- Success rate (successful scans / total scans)
- User reputation (karma ranking)

### Query Examples:
```sql
-- Top givers (most items returned)
SELECT user_id, COUNT(*) as items_returned 
FROM items 
WHERE found_user_id = user_id AND status = 'returned'
GROUP BY user_id 
ORDER BY items_returned DESC;

-- Top receivers (most items recovered)
SELECT returned_user_id, COUNT(*) as items_recovered
FROM items
WHERE status = 'returned'
GROUP BY returned_user_id
ORDER BY items_recovered DESC;
```

---

## ✅ COMPLETION CHECKLIST

- [x] QRFragment.java - Added ownership validation
- [x] QrScanActivity.java - Added ownership validation
- [x] LOSTFOUND_API_DOCUMENTATION.md - Updated with new rules
- [x] API_UPDATE_SUMMARY.md - Updated workflow
- [x] Created QR_HANDOVER_LOGIC_UPDATE.md (this file)
- [ ] Backend implements all 3 field updates
- [ ] Backend implements karma rewards
- [ ] Test on device with 2 different users
- [ ] Verify karma updates in database
- [ ] Monitor for edge cases

---

## 🔍 DEBUGGING

### Client Logs:
```
✅ Success:
D/QRFragment: Processing QR: itemId=38, title=iPhone 15
D/QRFragment: Confirming handover: itemId=38, giverId=5, receiverId=10
D/QRFragment: ✅ Handover confirmed successfully

❌ Self-scan blocked:
W/QRFragment: ❌ Cannot scan own item: userId=5
```

### Server Logs (Expected):
```
POST /api/lostfound/items/38/confirm-handover
→ Validate token: ✅
→ Check scanner ≠ creator: ✅ (5 ≠ 10)
→ Update item: ✅ status=returned
→ Set user roles: ✅ lost=null, found=5, returned=10
→ Create history: ✅ giver=5, receiver=10
→ Award karma: ✅ user_5 +10, user_10 +10
→ Response: 200 OK
```

---

## 📞 SUPPORT

**Documentation:**
- `LOSTFOUND_API_DOCUMENTATION.md` - Complete API reference
- `API_UPDATE_SUMMARY.md` - Database migration guide
- `QR_HANDOVER_FINAL_SUMMARY.md` - Original QR implementation

**Contact:**
- Check logcat for error details
- Verify backend API is updated with new logic
- Test with at least 2 different user accounts

---

**Status:** ✅ **CLIENT-SIDE COMPLETE**  
**Next:** Verify backend implements karma rewards and 3-field updates

**Generated:** November 3, 2025  
**Version:** 2.0 - Ownership validation + Karma rewards
