# QR SCAN UPDATE ITEM - TEST GUIDE

## 📝 Vấn đề đã Fix

### Lỗi ban đầu:
```
Toast: "Không thể cập nhật trạng thái vật phẩm"
```

### Nguyên nhân:
- **PUT request gửi toàn bộ LostItem object** với nhiều fields có `@Expose(serialize = false)`
- **API backend chỉ expect các fields cần update**, không cần full object
- **ID field không được serialize** (`@Expose(serialize = false, deserialize = true)`)

### Giải pháp:
✅ Tạo **UpdateItemRequest DTO** - chỉ gửi fields cần update  
✅ Cập nhật **ItemApi.updateItem()** - dùng UpdateItemRequest thay vì LostItem  
✅ Thêm **detailed logging** - track response code và error body  

---

## 🏗️ Files Changed

### 1. **UpdateItemRequest.java** (NEW)
```java
public class UpdateItemRequest {
    @Expose private String title;
    @Expose private String description;
    @Expose private String category;
    @Expose private String status;        // ✅ Chỉ gửi field này khi update QR
    @Expose private Double latitude;
    @Expose private Double longitude;
    @Expose private String imageUrl;
    
    // Builder pattern for flexible construction
}
```

**Tính năng:**
- Chỉ serialize fields được set (non-null)
- Không gửi id, userId, createdAt, updatedAt (server managed)
- Builder pattern cho dễ sử dụng

---

### 2. **ItemApi.java** (UPDATED)
```java
// Before:
@PUT("api/lostfound/items/{itemId}")
Call<ApiResponse<LostItem>> updateItem(
    @Header("Authorization") String token,
    @Path("itemId") long itemId,
    @Body LostItem item  // ❌ Full object
);

// After:
@PUT("api/lostfound/items/{itemId}")
Call<ApiResponse<LostItem>> updateItem(
    @Header("Authorization") String token,
    @Path("itemId") long itemId,
    @Body UpdateItemRequest request  // ✅ DTO only with needed fields
);
```

---

### 3. **QrScanActivity.java** (UPDATED)

#### A. Imports
```java
import com.fptcampus.lostfoundfptcampus.model.dto.UpdateItemRequest;
```

#### B. Update Logic
```java
// Before:
item.setStatus("returned");
ApiClient.getItemApi().updateItem(token, itemId, item).enqueue(...);

// After:
UpdateItemRequest updateRequest = new UpdateItemRequest();
updateRequest.setStatus("returned");  // ✅ Chỉ gửi status field
ApiClient.getItemApi().updateItem(token, itemId, updateRequest).enqueue(...);
```

#### C. Enhanced Logging
```java
@Override
public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
    android.util.Log.d("QrScanActivity", "Update response code: " + response.code());
    
    if (response.isSuccessful() && response.body() != null) {
        android.util.Log.d("QrScanActivity", "Response body: " + response.body().toString());
        android.util.Log.d("QrScanActivity", "Success: " + response.body().isSuccess());
        
        if (response.body().isSuccess()) {
            // ✅ Success flow
        } else {
            String errorMsg = response.body().getError();
            android.util.Log.e("QrScanActivity", "Update failed - Error: " + errorMsg);
            handleError("Không thể cập nhật: " + errorMsg);
        }
    } else {
        android.util.Log.e("QrScanActivity", "Failed to update item - Response unsuccessful or null");
        if (response.errorBody() != null) {
            try {
                String errorBody = response.errorBody().string();
                android.util.Log.e("QrScanActivity", "Error body: " + errorBody);
            } catch (Exception e) {
                android.util.Log.e("QrScanActivity", "Cannot read error body", e);
            }
        }
        handleError("Không thể cập nhật trạng thái vật phẩm");
    }
}
```

---

## 🧪 Testing Checklist

### Test 1: Update Item API (Postman/cURL)
```bash
# Lấy token từ login
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Test PUT update item
curl -X PUT https://vietsuky.com/api/lostfound/items/123 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"returned"}'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Item updated successfully",
  "data": {
    "id": 123,
    "status": "returned",
    ...
  }
}
```

---

### Test 2: QR Scan Flow

#### Step 1: Tạo QR Code
1. Mở QrScanActivity
2. Tab "Tạo mã QR"
3. Search và chọn item (status = "found")
4. Click "Tạo mã QR"
5. **✅ QR hiển thị ngay, không popup**

#### Step 2: Quét QR Code
1. Tab "Quét mã QR"
2. Quét QR code vừa tạo
3. **Check Logcat:**
```
D/QrScanActivity: QR Content: {"itemId":123,"title":"...","token":"..."}
D/QrScanActivity: Processing QR: itemId=123, receiverId=10
D/QrScanActivity: Item details - ID: 123, UserId: 5, Status: found, Title: ...
D/QrScanActivity: Updating item 123 to 'returned' status...
D/QrScanActivity: Update response code: 200
D/QrScanActivity: Response body: ApiResponse{success=true, ...}
D/QrScanActivity: Success: true
D/QrScanActivity: ✅ Item updated to 'returned' status
D/QrScanActivity: ✅ History created successfully
```

4. **✅ Toast: "Hoàn tất trả đồ thành công"**
5. **✅ Scanner resume tự động**

---

### Test 3: Verify Database Changes

#### Check Item Status
```bash
curl -X GET https://vietsuky.com/api/lostfound/items/123 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "status": "returned",  // ✅ Updated
    ...
  }
}
```

#### Check History Record
```bash
curl -X GET https://vietsuky.com/api/lostfound/histories \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "itemId": 123,
      "giverId": 5,
      "receiverId": 10,
      "qrToken": "TOKEN_...",
      "confirmedAt": "2025-11-03T15:30:00"
    }
  ]
}
```

---

## 📊 Log Analysis

### Success Logs
```
D/QrScanActivity: QR Content: {"itemId":123,...}
D/QrScanActivity: Processing QR: itemId=123, receiverId=10
D/QrScanActivity: Item details - ID: 123, UserId: 5, Status: found
D/QrScanActivity: Updating item 123 to 'returned' status...
D/QrScanActivity: Update response code: 200         // ✅ HTTP 200 OK
D/QrScanActivity: Success: true                     // ✅ API success=true
D/QrScanActivity: ✅ Item updated to 'returned' status
D/QrScanActivity: ✅ History created successfully
```

### Error Logs (If Failed)
```
E/QrScanActivity: Update response code: 400         // HTTP error
E/QrScanActivity: Update failed - Error: Invalid status value
// OR
E/QrScanActivity: Failed to update item - Response unsuccessful or null
E/QrScanActivity: Error body: {"error":"..."}
```

---

## 🔍 Common Issues & Solutions

### Issue 1: "Không thể cập nhật trạng thái vật phẩm"
**Nguyên nhân:**
- API response code không phải 200
- Response body null
- success = false

**Debug:**
1. Check Logcat cho "Update response code"
2. Check "Error body" message
3. Verify token chưa expire
4. Verify itemId tồn tại

**Fix:**
- Đã fix bằng UpdateItemRequest DTO ✅
- Thêm detailed error logging ✅

---

### Issue 2: "Không thể tạo lịch sử giao dịch"
**Nguyên nhân:**
- History API endpoint không tồn tại
- Request body không đúng format
- Foreign key constraint fail

**Debug:**
1. Check History API endpoint: `POST /api/lostfound/histories`
2. Verify giverId và receiverId tồn tại trong users table
3. Check History model có đủ fields

**Note:**
⚠️ Endpoint `/api/lostfound/histories` có thể chưa implement trên backend  
→ Cần confirm với backend team

---

### Issue 3: JSON Serialization Error
**Nguyên nhân:**
- LostItem có fields với `@Expose(serialize = false)`
- Gson bỏ qua fields không có `@Expose`

**Fix:**
✅ Dùng UpdateItemRequest với chỉ fields cần thiết  
✅ Tất cả fields trong UpdateItemRequest có `@Expose`

---

## 📝 API Request Examples

### Request Body (UpdateItemRequest)
```json
{
  "status": "returned"
}
```

**Gson serialization:**
- ✅ Chỉ gửi `status` field
- ❌ Không gửi null fields (title, description, etc.)
- ❌ Không gửi id, userId, createdAt (not in DTO)

### Request Body (History)
```json
{
  "itemId": 123,
  "giverId": 5,
  "receiverId": 10,
  "qrToken": "TOKEN_1730678400000",
  "confirmedAt": "2025-11-03T15:30:00"
}
```

---

## 🚀 Next Steps

### If Still Error:
1. **Run app và quét QR**
2. **Copy full Logcat logs** (filter: QrScanActivity)
3. **Share logs** để analyze chi tiết

### Backend API Verification:
```bash
# Test PUT endpoint
curl -X PUT https://vietsuky.com/api/lostfound/items/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"returned"}'

# Test POST history endpoint
curl -X POST https://vietsuky.com/api/lostfound/histories \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": 1,
    "giverId": 5,
    "receiverId": 10,
    "qrToken": "TOKEN_TEST",
    "confirmedAt": "2025-11-03T15:30:00"
  }'
```

---

**Generated:** November 3, 2025  
**Status:** 🔧 Debug in Progress
