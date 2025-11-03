# Database Migration Guide - Version 2 to 3

## 📋 Overview

**Migration Date:** November 3, 2025  
**Version:** 2 → 3  
**Purpose:** Add user role tracking fields to items table

---

## 🔄 Changes

### New Columns Added to `items` Table

| Column Name | Type | Nullable | Description |
|------------|------|----------|-------------|
| `lost_user_id` | INTEGER | YES | ID of user who lost the item (owner) |
| `found_user_id` | INTEGER | YES | ID of user who found the item |
| `returned_user_id` | INTEGER | YES | ID of user who received the item back |

---

## 📝 SQL Migration Script

```sql
ALTER TABLE items ADD COLUMN lost_user_id INTEGER;
ALTER TABLE items ADD COLUMN found_user_id INTEGER;
ALTER TABLE items ADD COLUMN returned_user_id INTEGER;
```

---

## 🔧 Android Room Migration

### AppDatabase.java

**Updated:**
- Version: `2` → `3`
- Added `MIGRATION_2_3` with ALTER TABLE statements
- Added `.addMigrations(MIGRATION_2_3)` to database builder

```java
static final Migration MIGRATION_2_3 = new Migration(2, 3) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("ALTER TABLE items ADD COLUMN lost_user_id INTEGER");
        database.execSQL("ALTER TABLE items ADD COLUMN found_user_id INTEGER");
        database.execSQL("ALTER TABLE items ADD COLUMN returned_user_id INTEGER");
    }
};
```

### LostItem.java

**Added Fields:**
```java
@ColumnInfo(name = "lost_user_id")
@Expose
@SerializedName("lostUserId")
private Long lostUserId;

@ColumnInfo(name = "found_user_id")
@Expose
@SerializedName("foundUserId")
private Long foundUserId;

@ColumnInfo(name = "returned_user_id")
@Expose
@SerializedName("returnedUserId")
private Long returnedUserId;
```

**Added Getters/Setters:**
- `getLostUserId()` / `setLostUserId(Long)`
- `getFoundUserId()` / `setFoundUserId(Long)`
- `getReturnedUserId()` / `setReturnedUserId(Long)`

---

## 📊 Data Flow

### Scenario 1: Lost Item Created

```
User A loses iPhone
↓
POST /api/lostfound/items { status: "lost" }
↓
API Response:
{
  userId: A,
  lostUserId: A,         ← Automatically set
  foundUserId: null,
  returnedUserId: null,
  status: "lost"
}
```

### Scenario 2: Item Found

```
User B finds iPhone
↓
POST /api/lostfound/items { status: "found" }
OR
PUT /api/lostfound/items/{id} { status: "found" }
↓
API Response:
{
  userId: A or B,
  lostUserId: null or A,
  foundUserId: B,        ← Automatically set
  returnedUserId: null,
  status: "found"
}
```

### Scenario 3: Handover Confirmed

```
User A scans QR code to receive iPhone
↓
POST /api/lostfound/items/{id}/confirm-handover
Header: Authorization: Bearer {user_a_token}
Body: { qrToken: "TOKEN_1730678400000" }
↓
API Response:
{
  userId: original_owner,
  lostUserId: A,
  foundUserId: B,
  returnedUserId: A,     ← Set to scanner (receiver)
  status: "returned"
}
```

---

## ⚠️ Migration Notes

### Existing Data
- All existing items will have `NULL` values for the new fields
- Fields will be populated on next API sync
- No data loss occurs

### Backward Compatibility
- Old app versions (DB v2) will crash if trying to read v3 database
- **Solution:** Update app before migration or use fallback migration
- Current implementation: `.fallbackToDestructiveMigration()` (development)

### Production Deployment
1. Remove `.fallbackToDestructiveMigration()` for production
2. Ensure all users update to new version
3. Or provide backward-compatible API responses

---

## 🧪 Testing Migration

### Test Steps:
1. Install app with DB version 2
2. Create some test items
3. Update app to DB version 3
4. Verify:
   - ✅ App doesn't crash
   - ✅ Old items still visible
   - ✅ New fields are NULL or populated
   - ✅ New items have correct user role fields

### Manual Test:
```kotlin
// Check database version
val db = AppDatabase.getInstance(context)
val version = db.openHelper.writableDatabase.version
Log.d("Migration", "Database version: $version") // Should be 3

// Check new columns exist
val items = db.lostItemDao().getAllItems()
items.forEach {
    Log.d("Migration", "Item: ${it.title}")
    Log.d("Migration", "  lostUserId: ${it.lostUserId}")
    Log.d("Migration", "  foundUserId: ${it.foundUserId}")
    Log.d("Migration", "  returnedUserId: ${it.returnedUserId}")
}
```

---

## 🔍 API Integration

### ItemApi.java
- ✅ **No changes required**
- Gson automatically parses new fields with `@SerializedName`
- All endpoints return updated `LostItem` objects

### Affected Endpoints:
- `GET /api/lostfound/items` - Returns all items with new fields
- `GET /api/lostfound/items/{id}` - Returns item with new fields
- `POST /api/lostfound/items` - Creates item with auto-populated fields
- `PUT /api/lostfound/items/{id}` - Updates item (may update fields)
- `POST /api/lostfound/items/{id}/confirm-handover` - Sets returnedUserId

---

## 📱 UI Changes

### Current Implementation:
- ✅ All fragments/activities automatically receive new fields
- ✅ No UI changes required (fields are optional)
- ✅ Can add user role information later if needed

### Future Enhancements:
Add user role display in DetailItemFragment:
```xml
<!-- Lost by: User A -->
<!-- Found by: User B -->
<!-- Returned to: User A -->
```

---

## 📝 Rollback Plan

If migration fails:
1. Uninstall app (clears database)
2. Reinstall previous version
3. Or manually downgrade database:
```sql
ALTER TABLE items DROP COLUMN lost_user_id;
ALTER TABLE items DROP COLUMN found_user_id;
ALTER TABLE items DROP COLUMN returned_user_id;
```

---

## ✅ Checklist

- [x] Update `LostItem.java` with new fields
- [x] Add getters/setters for new fields
- [x] Update `AppDatabase.java` version to 3
- [x] Create `MIGRATION_2_3` with ALTER TABLE
- [x] Add migration to database builder
- [x] Document migration in SQL script
- [x] No UI changes required (optional fields)
- [x] No API changes required (auto-parsed)
- [x] Test compilation - ✅ No errors
- [ ] Test migration on device
- [ ] Verify API sync with new fields

---

## 📞 Support

If migration issues occur:
1. Check logcat for migration errors
2. Verify database version: `adb shell "run-as com.fptcampus.lostfoundfptcampus cat /data/data/com.fptcampus.lostfoundfptcampus/databases/lostfound_fptcampus.db" | sqlite3`
3. Clear app data and resync
4. Or use `.fallbackToDestructiveMigration()` (development only)

---

**Generated:** November 3, 2025  
**Status:** ✅ Complete  
**Next Steps:** Build and test on device
