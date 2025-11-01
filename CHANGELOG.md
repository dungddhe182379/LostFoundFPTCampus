# 📝 CHANGELOG

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2025-11-01

### ✨ Added

#### Core Features
- **Authentication System**
  - Login with JWT token
  - Register with email validation (@fpt.edu.vn)
  - Auto-save token to SharedPreferences
  - Auto-logout when token expires

- **Room Database (Offline-First)**
  - User entity with DAO
  - LostItem entity with sync flag
  - Photo, History, KarmaLog, Notification entities
  - Type converters for Date
  - Singleton database pattern

- **RESTful API Integration**
  - Retrofit 2 + OkHttp 3
  - JWT auto-injection via Interceptor
  - Gson converter for JSON
  - HTTP logging interceptor (debug mode)
  - Error handling with ApiResponse wrapper

- **Lost & Found Items**
  - Create new item (title, description, category, status)
  - View all items in RecyclerView
  - Filter by status (all, lost, found)
  - GPS location picker
  - Offline creation with sync
  - Swipe to refresh

- **UI Components**
  - Material Design 3 components
  - Custom RecyclerView adapter with ViewHolder pattern
  - Loading states with ProgressBar
  - Error handling with Toast
  - Toolbar with navigation
  - FloatingActionButton

#### Architecture
- **MVC Pattern**
  - Model: Room entities + API models
  - View: XML layouts
  - Controller: Activities with bindingView/bindingAction pattern

#### Utilities
- SharedPreferencesManager for session management
- PermissionHelper for runtime permissions
- ApiClient singleton with JWT interceptor
- Date formatters and converters

#### Security
- Network Security Config
- HTTPS enforcement (production)
- Cleartext traffic for localhost (dev)
- Certificate pinning support (commented)
- JWT Bearer token authentication

### 🔧 Technical Details

#### Dependencies
- Room 2.6.1 (SQLite ORM)
- Retrofit 2.9.0 (HTTP client)
- OkHttp 3.11.0 (Networking)
- Gson 2.9.0 (JSON parser)
- JWT Decode 2.0.2 (Token parsing)
- OSMDroid 6.1.16 (Maps - not yet implemented)
- ZXing 4.3.0 (QR Scanner - not yet implemented)
- Google Play Services Location 21.0.1
- Material Components (latest)
- SwipeRefreshLayout 1.1.0

#### Database Schema
- 6 tables: users, items, photos, histories, karma_logs, notifications
- Foreign key constraints
- Indexes on frequently queried columns
- Auto-generated IDs

#### API Endpoints
- POST /api/lostfound/auth/register
- POST /api/lostfound/auth/login
- GET /api/lostfound/items
- GET /api/lostfound/items/status/{status}
- POST /api/lostfound/items
- PUT /api/lostfound/items/{id}
- DELETE /api/lostfound/items/{id}

### 📱 Screens Implemented
- LoginActivity
- RegisterActivity
- MainActivity (navigation hub)
- ListItemActivity (with tabs & filter)
- AddItemActivity (with GPS picker)

### 🎨 UI/UX Features
- Dark/Light theme support (Material You)
- Smooth transitions between activities
- Loading indicators for async operations
- Empty state handling
- Error messages with user-friendly text
- Pull-to-refresh on lists

### 📄 Documentation
- README.md (full project overview)
- QUICK_START.md (setup guide)
- MODEL_REFERENCE.md (data structures)
- BUILD_GUIDE.md (deployment guide)
- LOSTFOUND_API_DOCUMENTATION.md (API reference)
- LOSTFOUND_API_URLS.md (endpoint quick reference)
- lostfound_project_summary.md (project context)

---

## [Unreleased] - Planned Features

### 🚧 In Development

#### High Priority
- [ ] MapActivity with OSMDroid
  - Display item locations on map
  - Cluster markers
  - "Near me" feature
  - Route to item location

- [ ] QrScanActivity with ZXing
  - Generate QR code for items
  - Scan QR to confirm return
  - History of transactions

- [ ] DetailItemActivity
  - Full item details
  - Photo gallery
  - Contact finder
  - Mark as found/returned
  - Edit/Delete (owner only)

#### Medium Priority
- [ ] LeaderboardActivity
  - Top users by karma
  - Badge system
  - User ranking
  - Karma history

- [ ] Image Upload
  - Pick from gallery
  - Take photo
  - Multipart upload to server
  - Image compression
  - Multiple photos per item

- [ ] Search & Filter
  - Search by keyword
  - Filter by category
  - Filter by date range
  - Filter by distance

- [ ] Notifications
  - Firebase Cloud Messaging (FCM)
  - Push notifications for nearby items
  - In-app notification center
  - Mark as read functionality

#### Low Priority
- [ ] Profile Management
  - Edit profile
  - Upload avatar
  - View my items
  - View my karma logs

- [ ] Settings
  - Notification preferences
  - Location permissions
  - Theme selection
  - Language selection

- [ ] Social Features
  - Chat with finder
  - Rate users
  - Share items to social media

### 🔧 Technical Improvements
- [ ] Pagination for item lists
- [ ] Image caching with Glide/Picasso
- [ ] Background sync with WorkManager
- [ ] Database encryption with SQLCipher
- [ ] Crash reporting with Firebase Crashlytics
- [ ] Analytics with Firebase Analytics
- [ ] Unit tests with JUnit
- [ ] UI tests with Espresso

### 🐛 Known Issues
- Image upload not implemented (placeholder only)
- Map activity not implemented
- QR scanner not implemented
- No pagination (loads all items at once)
- No image loading library (default placeholders)

---

## Version History Summary

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-01 | Initial release with core features |

---

## Migration Notes

### From 0.x to 1.0.0
- N/A (initial release)

---

## Contributors
- **Developer**: Implementation of MVC architecture, Room DB, Retrofit API
- **API**: Backend RESTful API with JWT authentication
- **Database**: MySQL schema design

---

**Current Version:** 1.0.0  
**Last Updated:** November 1, 2025  
**Status:** ✅ Core features complete, ready for testing

---

## Feedback & Issues
Please report bugs and feature requests in the project repository.

---
