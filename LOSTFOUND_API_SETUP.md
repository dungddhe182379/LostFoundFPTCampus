# 🚀 LOST & FOUND FPT CAMPUS - API SETUP GUIDE

## 📋 Overview

RESTful API cho ứng dụng Android Lost & Found FPT Campus. API được tích hợp vào server VietSuKy hiện tại và sử dụng database riêng biệt.

---

## ✅ Prerequisites

- Java 8+
- MySQL Server
- Apache Tomcat 9+
- JJWT Library (io.jsonwebtoken)
- Gson Library (com.google.code.gson)
- BCrypt Library (org.mindrot.jbcrypt)
- Hibernate 5.x

---

## 📦 Required Libraries

Đảm bảo các thư viện sau đã được thêm vào `lib` folder hoặc Maven dependencies:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
</dependency>

<!-- Gson -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- BCrypt -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

---

## 🗄️ Database Setup

### 1. Import Database Schema

Chạy file `DB-lostfond.sql` để tạo database và tables:

```bash
mysql -u root -p < DB-lostfond.sql
```

Hoặc import qua MySQL Workbench/phpMyAdmin.

### 2. Verify Database

```sql
USE lostfound_fptcampus;
SHOW TABLES;
```

Bạn sẽ thấy các tables:
- users
- items
- photos
- histories
- karma_logs
- notifications
- roles
- user_roles

---

## 🔧 Configuration

### 1. Database Connection

File `LostFoundHibernateUtil.java` đã được cấu hình sẵn:

**Local Environment:**
```java
URL: jdbc:mysql://vietsuky.com:3306/lostfound_fptcampus
User: localuser
Password: LocalPass123!
```

**Server Environment (APP_ENV=server):**
```java
URL: jdbc:mysql://localhost:3306/lostfound_fptcampus
User: tomcat
Password: LocalPass123!
```

### 2. Set Environment Variable (Optional)

Để sử dụng server config:
```bash
# Linux/Mac
export APP_ENV=server

# Windows
set APP_ENV=server
```

---

## 📁 Project Structure

```
src/java/
├── controller/
│   └── api/
│       └── lostfound/
│           ├── AuthApiServlet.java       # Authentication APIs
│           ├── ItemApiServlet.java       # Item CRUD APIs
│           ├── UserApiServlet.java       # User Profile APIs
│           └── NotificationApiServlet.java # Notification APIs
├── dao/
│   └── lostfound/
│       ├── UserDAO.java
│       ├── ItemDAO.java
│       └── NotificationDAO.java
├── model/
│   └── lostfound/
│       ├── User.java
│       ├── Item.java
│       ├── Photo.java
│       ├── History.java
│       ├── KarmaLog.java
│       └── Notification.java
└── util/
    ├── LostFoundHibernateUtil.java
    └── api/
        ├── ApiResponse.java              # JSON response utility
        └── JwtUtil.java                  # JWT authentication
```

---

## 🚀 Deployment

### 1. Build Project

```bash
# Sử dụng Ant
ant clean
ant compile
ant dist
```

### 2. Deploy to Tomcat

Copy file WAR hoặc deploy folder build:
```bash
cp build/web/* /path/to/tomcat/webapps/Vietsuky2/
```

### 3. Restart Tomcat

```bash
# Linux
sudo systemctl restart tomcat

# Windows
net stop tomcat9
net start tomcat9
```

---

## ✅ Testing APIs

### 1. Test Authentication

**Register:**
```bash
curl -X POST http://localhost:8080/Vietsuky2/api/lostfound/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@fpt.edu.vn",
    "password": "123456",
    "phone": "0123456789"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUz...",
    "user": {
      "id": 1,
      "name": "Test User",
      "email": "test@fpt.edu.vn",
      ...
    }
  }
}
```

### 2. Test Login

```bash
curl -X POST http://localhost:8080/Vietsuky2/api/lostfound/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@fpt.edu.vn",
    "password": "123456"
  }'
```

### 3. Test Get Items (with token)

```bash
TOKEN="eyJhbGciOiJIUz..."

curl -X GET http://localhost:8080/Vietsuky2/api/lostfound/items \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Test Create Item

```bash
curl -X POST http://localhost:8080/Vietsuky2/api/lostfound/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Lost iPhone 15",
    "description": "Lost near library",
    "category": "electronics",
    "status": "lost",
    "latitude": 21.0285,
    "longitude": 105.8542
  }'
```

---

## 🐛 Troubleshooting

### Problem 1: ClassNotFoundException (JWT)

**Error:** `java.lang.ClassNotFoundException: io.jsonwebtoken.Jwts`

**Solution:** Add JJWT libraries to `lib` folder:
- jjwt-api-0.11.5.jar
- jjwt-impl-0.11.5.jar
- jjwt-jackson-0.11.5.jar

### Problem 2: Database Connection Error

**Error:** `Could not create connection to database`

**Solution:**
1. Check MySQL is running: `systemctl status mysql`
2. Verify database exists: `SHOW DATABASES LIKE 'lostfound%'`
3. Check credentials in `LostFoundHibernateUtil.java`
4. Test connection: `mysql -u localuser -pLocalPass123! lostfound_fptcampus`

### Problem 3: Hibernate SessionFactory Error

**Error:** `Unknown entity: model.lostfound.User`

**Solution:** Make sure all entities are registered in `LostFoundHibernateUtil.java`:
```java
configuration.addAnnotatedClass(User.class);
configuration.addAnnotatedClass(Item.class);
// ... etc
```

### Problem 4: CORS Issues

**Error:** CORS error when calling from Android

**Solution:** Add CORS filter in web.xml or create CorsFilter:
```java
response.setHeader("Access-Control-Allow-Origin", "*");
response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
```

---

## 📊 Database Schema Overview

### users
- id (BIGINT, PK)
- uuid (CHAR(36), UNIQUE)
- name, email, password_hash
- phone, avatar_url
- karma (INT)
- created_at, updated_at

### items
- id (BIGINT, PK)
- uuid (CHAR(36), UNIQUE)
- user_id (FK to users)
- title, description, category
- status (ENUM: lost/found/returned)
- latitude, longitude
- image_url
- created_at, updated_at

### notifications
- id (BIGINT, PK)
- user_id (FK to users)
- title, body
- is_read (BOOLEAN)
- created_at

---

## 🔐 Security Notes

1. **JWT Secret:** Change the secret key in `JwtUtil.java` for production
2. **Password Hashing:** Uses BCrypt with salt (automatic)
3. **Token Expiration:** 7 days (configurable in `JwtUtil.java`)
4. **SQL Injection:** Protected by Hibernate parameterized queries
5. **HTTPS:** Recommend using HTTPS in production

---

## 📱 Android Integration Example

### Retrofit Setup

```java
public interface LostFoundApi {
    @POST("api/lostfound/auth/login")
    Call<ApiResponse<LoginData>> login(@Body LoginRequest request);
    
    @GET("api/lostfound/items")
    Call<ApiResponse<List<Item>>> getItems();
    
    @POST("api/lostfound/items")
    Call<ApiResponse<Item>> createItem(@Body Item item);
}

// Retrofit instance
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("http://vietsuky.com/Vietsuky2/")
    .addConverterFactory(GsonConverterFactory.create())
    .client(createOkHttpClient())
    .build();

// OkHttp client with Authorization interceptor
private OkHttpClient createOkHttpClient() {
    return new OkHttpClient.Builder()
        .addInterceptor(chain -> {
            String token = getTokenFromPreferences();
            Request request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
            return chain.proceed(request);
        })
        .build();
}
```

---

## 📖 Documentation Files

- `LOSTFOUND_API_DOCUMENTATION.md` - Full API documentation with examples
- `LOSTFOUND_API_URLS.md` - Quick reference for all endpoints
- `DB-lostfond.sql` - Database schema

---

## 🎯 Next Steps

1. ✅ Import database schema
2. ✅ Add required libraries
3. ✅ Configure database connection
4. ✅ Deploy to Tomcat
5. ✅ Test APIs with curl/Postman
6. ✅ Integrate with Android app

---

## 📞 Support

If you need help:
1. Check Tomcat logs: `catalina.out`
2. Check MySQL logs: `/var/log/mysql/error.log`
3. Enable Hibernate SQL logging in `LostFoundHibernateUtil.java`:
   ```java
   settings.put(Environment.SHOW_SQL, true);
   ```

---

**Created:** November 1, 2025
**Version:** 1.0
**Database:** lostfound_fptcampus
**Base URL:** http://vietsuky.com/Vietsuky2/api/lostfound
