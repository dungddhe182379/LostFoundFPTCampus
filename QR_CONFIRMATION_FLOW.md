# QR SCAN CONFIRMATION FLOW - IMPLEMENTATION

## 📋 Overview
Sau khi quét QR code, hiển thị dialog xác nhận với thông tin chi tiết item trước khi cập nhật, thay vì tự động update.

**Ngày:** November 3, 2025  
**Trạng thái:** ✅ Hoàn thành

---

## 🎯 Flow Mới

### 1️⃣ **Quét QR Code**
```
User quét QR → Parse JSON (itemId, title, token)
```

### 2️⃣ **Lấy thông tin chi tiết**
```
GET /api/lostfound/items/{itemId}
→ Lấy full details: title, description, category, status, userId
```

### 3️⃣ **Hiển thị Dialog Xác Nhận**
```
┌────────────────────────────────┐
│  Xác nhận trả đồ              │
├────────────────────────────────┤
│  📦 Tên: Lost iPhone 15       │
│  📝 Mô tả: iPhone màu đen...  │
│  🏷️ Danh mục: electronics     │
│  📍 Trạng thái: found         │
│                                │
│  Bạn có xác nhận đã nhận      │
│  lại đồ này?                  │
├────────────────────────────────┤
│  [Hủy]          [Xác nhận]    │
└────────────────────────────────┘
```

### 4️⃣ **User Click "Xác nhận"**
```
→ Hiển thị ProgressDialog "Đang cập nhật..."
→ PUT /api/lostfound/items/{itemId} với body: {"status":"returned"}
→ POST /api/lostfound/histories với full history object
→ Dismiss ProgressDialog
```

### 5️⃣ **Hiển thị Dialog Kết Quả**

#### ✅ Thành công:
```
┌────────────────────────────────┐
│  ✅ Xác nhận thành công!       │
├────────────────────────────────┤
│  Đã cập nhật trạng thái vật   │
│  phẩm và ghi nhận giao dịch.  │
├────────────────────────────────┤
│              [OK]              │
└────────────────────────────────┘
```

#### ❌ Lỗi:
```
┌────────────────────────────────┐
│  ❌ Không thể cập nhật         │
├────────────────────────────────┤
│  You don't have permission... │
├────────────────────────────────┤
│              [OK]              │
└────────────────────────────────┘
```

### 6️⃣ **Resume Scanner**
```
User click OK → Scanner resume → Sẵn sàng quét tiếp
```

---

## 💻 Code Implementation

### Key Methods:

#### 1. `onQrScanned(String content)`
```java
private void onQrScanned(String content) {
    barcodeScanner.pause();
    
    try {
        JSONObject json = new JSONObject(content);
        long itemId = json.getLong("itemId");
        String qrToken = json.getString("token");
        String itemTitle = json.optString("title", "Unknown Item");
        long receiverId = prefsManager.getUserId();
        
        // Hiển thị dialog xác nhận với thông tin chi tiết
        showItemDetailAndConfirm(itemId, qrToken, itemTitle, receiverId);
        
    } catch (Exception e) {
        Toast.makeText(this, "Mã QR không hợp lệ", Toast.LENGTH_SHORT).show();
        barcodeScanner.resume();
    }
}
```

#### 2. `showItemDetailAndConfirm()`
```java
private void showItemDetailAndConfirm(long itemId, String qrToken, String itemTitle, long receiverId) {
    String token = "Bearer " + prefsManager.getToken();
    
    // GET item details
    ApiClient.getItemApi().getItemById(token, itemId).enqueue(new Callback<>() {
        @Override
        public void onResponse(...) {
            LostItem item = response.body().getData();
            
            // Tạo dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(QrScanActivity.this);
            builder.setTitle("Xác nhận trả đồ");
            
            // Message với emoji
            StringBuilder message = new StringBuilder();
            message.append("📦 Tên: ").append(item.getTitle()).append("\n\n");
            message.append("📝 Mô tả: ").append(item.getDescription()).append("\n\n");
            message.append("🏷️ Danh mục: ").append(item.getCategory()).append("\n\n");
            message.append("📍 Trạng thái: ").append(item.getStatus()).append("\n\n");
            message.append("Bạn có xác nhận đã nhận lại đồ này?");
            
            builder.setMessage(message.toString());
            
            // Buttons
            builder.setPositiveButton("Xác nhận", (dialog, which) -> {
                long giverId = item.getUserId();
                confirmHandoverAndUpdate(itemId, qrToken, giverId, receiverId, item);
            });
            
            builder.setNegativeButton("Hủy", (dialog, which) -> {
                barcodeScanner.resume();
            });
            
            builder.show();
        }
    });
}
```

#### 3. `confirmHandoverAndUpdate()`
```java
private void confirmHandoverAndUpdate(long itemId, String qrToken, 
                                     long giverId, long receiverId, LostItem item) {
    String token = "Bearer " + prefsManager.getToken();
    
    // Show progress
    ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Đang cập nhật...");
    progressDialog.show();
    
    // Update item
    UpdateItemRequest updateRequest = new UpdateItemRequest();
    updateRequest.setStatus("returned");
    
    ApiClient.getItemApi().updateItem(token, itemId, updateRequest).enqueue(new Callback<>() {
        @Override
        public void onResponse(...) {
            if (success) {
                // Create history
                History history = new History();
                history.setItemId(itemId);
                history.setGiverId(giverId);
                history.setReceiverId(receiverId);
                history.setQrToken(qrToken);
                history.setConfirmedAt(new Date());
                
                ApiClient.getHistoryApi().createHistory(token, history).enqueue(...);
                
                progressDialog.dismiss();
                showSuccessDialog("Xác nhận thành công!", "...");
            } else {
                progressDialog.dismiss();
                showErrorDialog("Không thể cập nhật", errorMsg);
            }
        }
    });
}
```

#### 4. `showSuccessDialog()` & `showErrorDialog()`
```java
private void showSuccessDialog(String title, String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("✅ " + title);
    builder.setMessage(message);
    builder.setPositiveButton("OK", (dialog, which) -> {
        barcodeScanner.resume();
    });
    builder.show();
}

private void showErrorDialog(String title, String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("❌ " + title);
    builder.setMessage(message);
    builder.setPositiveButton("OK", (dialog, which) -> {
        barcodeScanner.resume();
    });
    builder.show();
}
```

---

## 🔍 Dialog Types

### 1. Confirmation Dialog (AlertDialog)
- **Title:** "Xác nhận trả đồ"
- **Message:** Item details với emoji
- **Buttons:** "Hủy" | "Xác nhận"
- **Cancelable:** No

### 2. Progress Dialog (ProgressDialog)
- **Message:** "Đang cập nhật..."
- **Cancelable:** No
- **Auto-dismiss:** After API response

### 3. Success Dialog (AlertDialog)
- **Title:** "✅ Xác nhận thành công!"
- **Message:** "Đã cập nhật trạng thái vật phẩm và ghi nhận giao dịch."
- **Button:** "OK"
- **Cancelable:** No

### 4. Error Dialog (AlertDialog)
- **Title:** "❌ [Error Type]"
- **Message:** Error message from API
- **Button:** "OK"
- **Cancelable:** No

---

## 📊 API Test Results

### ✅ Working Endpoints:

#### 1. GET Item Details
```bash
curl -X GET https://vietsuky.com/api/lostfound/items/38 \
  -H "Authorization: Bearer $TOKEN"
```
**Response:** 200 OK ✅

#### 2. PUT Update Item (Owner Only)
```bash
curl -X PUT https://vietsuky.com/api/lostfound/items/38 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"found"}'
```
**Response:** 200 OK ✅
```json
{
  "success": true,
  "message": "Item updated successfully",
  "data": {
    "id": 38,
    "status": "found",
    "updatedAt": "2025-11-03T16:50:22"
  }
}
```

### ⚠️ Permission Issue:

#### PUT Update Item (Non-Owner)
```bash
curl -X PUT https://vietsuky.com/api/lostfound/items/41 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"returned"}'
```
**Response:** 400/403 Error ❌
```json
{
  "success": false,
  "error": "You don't have permission to update this item"
}
```

**Giải thích:**
- Item 41 belongs to userId=10
- Current user is userId=9
- **Backend API checks ownership** before allowing update
- **Receiver (người quét QR) không phải owner** → Cannot update

---

## 🐛 Known Issues & Solutions

### Issue 1: Permission Denied Error
**Vấn đề:** Người quét QR (receiver) không phải owner nên không thể update item.

**Hiện trạng:**
- Giver (userId=10) tạo QR cho item
- Receiver (userId=9) quét QR
- Receiver gọi PUT update → **403 Permission Denied**

**Giải pháp tạm thời:**
1. ✅ **Hiển thị error dialog** với thông báo rõ ràng
2. ✅ **Log chi tiết** để debug
3. ⏳ **Backend cần update logic:** Allow receiver to mark as "returned"

**Giải pháp lâu dài (Backend):**
- Option 1: Tạo endpoint riêng: `POST /api/lostfound/items/{itemId}/confirm-return`
- Option 2: Update logic: Allow receiver (from QR scan) to change status to "returned"
- Option 3: Two-step flow: Receiver confirms → Owner approves

---

## 🧪 Testing Checklist

### Test Case 1: Successful Handover (Same User)
1. ✅ Tạo QR cho item của user A (status="found")
2. ✅ User A quét QR của chính mình
3. ✅ Dialog hiển thị thông tin item
4. ✅ Click "Xác nhận"
5. ✅ ProgressDialog hiển thị
6. ✅ PUT update thành công (200 OK)
7. ✅ POST history thành công (200 OK)
8. ✅ Success dialog hiển thị
9. ✅ Click OK → Scanner resume

### Test Case 2: Permission Denied (Different User)
1. ✅ User A tạo QR cho item
2. ✅ User B quét QR
3. ✅ Dialog hiển thị thông tin item
4. ✅ Click "Xác nhận"
5. ✅ ProgressDialog hiển thị
6. ❌ PUT update failed (403 Permission)
7. ✅ Error dialog hiển thị message rõ ràng
8. ✅ Click OK → Scanner resume

### Test Case 3: Cancel Flow
1. ✅ Quét QR code
2. ✅ Dialog hiển thị
3. ✅ Click "Hủy"
4. ✅ Dialog dismiss
5. ✅ Scanner resume ngay lập tức

### Test Case 4: Invalid QR Code
1. ✅ Quét QR không hợp lệ (không phải JSON)
2. ✅ Toast: "Mã QR không hợp lệ"
3. ✅ Scanner resume

### Test Case 5: Network Error
1. ✅ Quét QR (không có internet)
2. ✅ Error dialog: "Lỗi kết nối: ..."
3. ✅ Click OK → Scanner resume

---

## 📱 User Experience

### Before (Auto Update):
```
Quét QR → Tự động update → Toast ngắn
↓
❌ Không biết item là gì
❌ Không có cơ hội xác nhận lại
❌ Lỗi permission thì không hiểu vì sao
```

### After (Confirmation Flow):
```
Quét QR → Hiển thị detail → Xác nhận → Dialog kết quả
↓
✅ Thấy rõ thông tin item trước khi nhận
✅ Có thể hủy nếu nhận nhầm
✅ Biết rõ kết quả thành công/thất bại
✅ Error message rõ ràng
```

---

## 🎨 UI/UX Improvements

### Dialog Design:
- ✅ **Emoji icons** (📦, 📝, 🏷️, 📍) - dễ nhìn, trực quan
- ✅ **Spacing** (\n\n) - dễ đọc
- ✅ **Clear buttons** - "Xác nhận" vs "Hủy"
- ✅ **ProgressDialog** - feedback trong quá trình xử lý
- ✅ **Result dialog** - thông báo rõ ràng với icon (✅/❌)

### Error Handling:
- ✅ **Meaningful errors** - hiển thị exact error từ API
- ✅ **Fallback messages** - nếu không parse được error
- ✅ **Always resume scanner** - không bị stuck

---

## 📋 Future Enhancements

### Backend Changes Needed:
1. **New endpoint:** `POST /api/lostfound/items/{itemId}/confirm-handover`
   ```json
   {
     "receiverId": 9,
     "qrToken": "TOKEN_xxx"
   }
   ```
   - Validates QR token
   - Creates history record
   - Updates item status
   - Sends notification to giver

2. **Or update PUT logic:**
   - Check if request comes from QR scan
   - Validate qrToken in request
   - Allow receiver to mark as "returned" if token valid

### App Enhancements:
- [ ] Add item image to dialog
- [ ] Show giver information (name, phone)
- [ ] Add map preview for location
- [ ] Vibration feedback on successful scan
- [ ] Sound effect on success/error
- [ ] Rating system after handover

---

## 📚 Related Files

- `QrScanActivity.java` - Main implementation
- `UpdateItemRequest.java` - DTO for PUT request
- `ItemApi.java` - API interface
- `HistoryApi.java` - History API interface
- `QR_UPDATE_DEBUG.md` - Debug guide
- `QR_HANDOVER_FLOW.md` - Original flow doc

---

**Generated:** November 3, 2025  
**Status:** ✅ Working (with permission caveat)  
**Next Step:** Backend update để support cross-user handover
