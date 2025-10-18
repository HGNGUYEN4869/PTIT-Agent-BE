# 💬 Chat API Documentation (Secured with JWT)

> ⚠️ **IMPORTANT**: Tất cả các API chat endpoint đều **yêu cầu authentication** thông qua **HttpOnly cookie** chứa `accessToken`. Client phải gửi cookie trong mỗi request (sử dụng `credentials: 'include'` trong fetch/axios).

---

## 🔐 Authentication Requirements

### **Cách hoạt động:**

1. User login → Server trả về `accessToken` và `refreshToken` trong **HttpOnly cookies**
2. Browser tự động gửi cookies trong mọi request đến server (cùng domain)
3. Server đọc `accessToken` từ cookie, validate token, extract `userId`
4. Server chỉ cho phép user truy cập chats của chính họ

### **Cookie Configuration:**

```http
Set-Cookie: accessToken=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=900
Set-Cookie: refreshToken=<jwt>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=604800
```

### **Frontend Request (Axios):**

```javascript
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true, // ✅ BẮT BUỘC để gửi cookies
});

// Login
await api.post("/h/auth/login", {
  username: "john",
  password: "123456",
});

// Tạo chat mới (cookies tự động gửi)
const chatRes = await api.post("/h/chats", { title: "My Chat" });
console.log(chatRes.data);
```

### **Frontend Request (Fetch):**

```javascript
// Login
await fetch("http://localhost:8080/h/auth/login", {
  method: "POST",
  credentials: "include", // ✅ BẮT BUỘC
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ username: "john", password: "123456" }),
});

// Tạo chat (cookies tự động gửi)
await fetch("http://localhost:8080/h/chats", {
  method: "POST",
  credentials: "include",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ title: "My Chat" }),
});
```

---

## 🚀 API Endpoints

### **1. Tạo Chat Mới**

**POST** `/h/chats`

**Authentication:** Required (accessToken cookie)

**Request Body:**

```json
{
  "title": "Conversation about AI"
}
```

**Response:**

```json
{
  "idChat": "abc-123-uuid",
  "idUser": "user-uuid",
  "title": "Conversation about AI",
  "messages": [],
  "createdAt": "2025-10-17T10:30:00",
  "updatedAt": "2025-10-17T10:30:00",
  "messageCount": 0
}
```

**cURL Example:**

```bash
curl -X POST "http://localhost:8080/h/chats" \
  -H "Content-Type: application/json" \
  -H "Cookie: accessToken=<your-jwt-token>" \
  -d '{"title":"My First Chat"}'
```

**JavaScript Example:**

```javascript
const response = await api.post("/h/chats", {
  title: "My First Chat",
});
console.log(response.data);
```

---

### **2. Lấy Tất Cả Chats của User**

**GET** `/h/chats`

**Authentication:** Required (accessToken cookie)

**Response:**

```json
[
  {
    "idChat": "chat-1-uuid",
    "idUser": "user-uuid",
    "title": "Chat 1",
    "messages": [
      {
        "idMessage": "msg-1-uuid",
        "role": "USER",
        "content": "Hello",
        "createdAt": "2025-10-17T10:30:00"
      },
      {
        "idMessage": "msg-2-uuid",
        "role": "ASSISTANT",
        "content": "Hi there!",
        "createdAt": "2025-10-17T10:30:05"
      }
    ],
    "createdAt": "2025-10-17T10:30:00",
    "updatedAt": "2025-10-17T10:30:05",
    "messageCount": 2
  }
]
```

**cURL Example:**

```bash
curl "http://localhost:8080/h/chats" \
  -H "Cookie: accessToken=<your-jwt-token>"
```

**JavaScript Example:**

```javascript
const chats = await api.get("/h/chats");
console.log(chats.data);
```

---

### **3. Lấy Chi Tiết Chat (với Messages)**

**GET** `/h/chats/{chatId}`

**Authentication:** Required (accessToken cookie)

**Response:**

```json
{
  "idChat": "chat-1-uuid",
  "idUser": "user-uuid",
  "title": "Chat about Spring Boot",
  "messages": [
    {
      "idMessage": "msg-1-uuid",
      "role": "USER",
      "content": "What is Spring Boot?",
      "createdAt": "2025-10-17T10:30:00"
    },
    {
      "idMessage": "msg-2-uuid",
      "role": "ASSISTANT",
      "content": "Spring Boot is a framework...",
      "createdAt": "2025-10-17T10:30:05"
    }
  ],
  "createdAt": "2025-10-17T10:30:00",
  "updatedAt": "2025-10-17T10:30:05",
  "messageCount": 2
}
```

**cURL Example:**

```bash
curl "http://localhost:8080/h/chats/chat-uuid" \
  -H "Cookie: accessToken=<your-jwt-token>"
```

**JavaScript Example:**

```javascript
const chat = await api.get(`/h/chats/${chatId}`);
console.log(chat.data);
```

---

### **4. Thêm Message vào Chat**

**POST** `/h/chats/{chatId}/messages`

**Authentication:** Required (accessToken cookie)

**Request Body:**

```json
{
  "role": "USER",
  "content": "Can you explain JWT authentication?"
}
```

**Response:** (Trả về toàn bộ chat với message mới)

```json
{
  "idChat": "chat-uuid",
  "idUser": "user-uuid",
  "title": "Chat title",
  "messages": [
    {
      "idMessage": "msg-3-uuid",
      "role": "USER",
      "content": "Can you explain JWT authentication?",
      "createdAt": "2025-10-17T10:35:00"
    }
  ],
  "createdAt": "2025-10-17T10:30:00",
  "updatedAt": "2025-10-17T10:35:00",
  "messageCount": 3
}
```

**cURL Example:**

```bash
curl -X POST "http://localhost:8080/h/chats/chat-uuid/messages" \
  -H "Content-Type: application/json" \
  -H "Cookie: accessToken=<your-jwt-token>" \
  -d '{"role":"USER","content":"Hello AI!"}'
```

**JavaScript Example:**

```javascript
const chat = await api.post(`/h/chats/${chatId}/messages`, {
  role: "USER",
  content: "Hello AI!",
});
console.log(chat.data);
```

---

### **5. Cập Nhật Title Chat**

**PUT** `/h/chats/{chatId}/title`

**Authentication:** Required (accessToken cookie)

**Request Body:**

```json
{
  "title": "New Chat Title"
}
```

**Response:**

```json
{
  "idChat": "chat-uuid",
  "idUser": "user-uuid",
  "title": "New Chat Title",
  "messages": [...],
  "createdAt": "2025-10-17T10:30:00",
  "updatedAt": "2025-10-17T10:40:00",
  "messageCount": 5
}
```

**cURL Example:**

```bash
curl -X PUT "http://localhost:8080/h/chats/chat-uuid/title" \
  -H "Content-Type: application/json" \
  -H "Cookie: accessToken=<your-jwt-token>" \
  -d '{"title":"My Updated Chat"}'
```

**JavaScript Example:**

```javascript
const chat = await api.put(`/h/chats/${chatId}/title`, {
  title: "New Chat Title",
});
console.log(chat.data);
```

---

### **6. Xóa Chat**

**DELETE** `/h/chats/{chatId}`

**Authentication:** Required (accessToken cookie)

**Response:**

```json
{
  "message": "Chat deleted successfully"
}
```

**cURL Example:**

```bash
curl -X DELETE "http://localhost:8080/h/chats/chat-uuid" \
  -H "Cookie: accessToken=<your-jwt-token>"
```

**JavaScript Example:**

```javascript
await api.delete(`/h/chats/${chatId}`);
```

---

### **7. Xóa Tất Cả Chats của User**

**DELETE** `/h/chats`

**Authentication:** Required (accessToken cookie)

**Response:**

```json
{
  "message": "All chats deleted successfully"
}
```

**cURL Example:**

```bash
curl -X DELETE "http://localhost:8080/h/chats" \
  -H "Cookie: accessToken=<your-jwt-token>"
```

**JavaScript Example:**

```javascript
await api.delete("/h/chats");
```

---

## 📊 Full Workflow Example

### **Scenario: User tạo chat và gửi messages (Secured)**

```javascript
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,
});

async function chatWorkflow() {
  // 1. Login (nhận cookies tự động)
  await api.post("/h/auth/login", {
    username: "john",
    password: "123456",
  });
  console.log("✅ Logged in, cookies saved");

  // 2. Tạo chat mới
  const chatRes = await api.post("/h/chats", {
    title: "AI Conversation",
  });
  const chatId = chatRes.data.idChat;
  console.log("✅ Created chat:", chatId);

  // 3. User gửi message
  await api.post(`/h/chats/${chatId}/messages`, {
    role: "USER",
    content: "What is Spring Boot?",
  });
  console.log("✅ User message sent");

  // 4. AI response (simulate)
  await api.post(`/h/chats/${chatId}/messages`, {
    role: "ASSISTANT",
    content: "Spring Boot is a framework...",
  });
  console.log("✅ Assistant message sent");

  // 5. Lấy lịch sử chat
  const chat = await api.get(`/h/chats/${chatId}`);
  console.log("✅ Chat history:", chat.data);

  // 6. Lấy tất cả chats của user
  const allChats = await api.get("/h/chats");
  console.log("✅ All chats:", allChats.data);

  // 7. Logout
  await api.post("/h/auth/logout");
  console.log("✅ Logged out");
}

chatWorkflow();
```

---

## 🔒 Security Features

### ✅ **Implemented Security:**

1. **HttpOnly Cookies**: Tokens không thể đọc từ JavaScript → phòng XSS attacks
2. **JWT Validation**: Mỗi request validate token signature và expiration
3. **User Ownership**: User chỉ truy cập được chats của chính họ (userId từ token)
4. **Token Type Check**: Chỉ accept `access` token cho chat operations
5. **CORS with Credentials**: `allowCredentials=true` cho cross-origin requests

### ⚠️ **Error Responses:**

```json
// Khi không gửi cookie
{
  "error": "Authentication failed: Missing required cookie 'accessToken'"
}

// Khi token hết hạn
{
  "error": "Authentication failed: Invalid or expired token"
}

// Khi user cố truy cập chat của người khác
{
  "error": "Chat not found or you don't have permission"
}
```

---

## 🧪 Testing with cURL

```bash
# 1. Login và lưu cookies vào file
curl -c cookies.txt -X POST "http://localhost:8080/h/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"123456"}'

# 2. Tạo chat (sử dụng cookies)
curl -b cookies.txt -X POST "http://localhost:8080/h/chats" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Chat"}'

# 3. Lấy tất cả chats
curl -b cookies.txt "http://localhost:8080/h/chats"

# 4. Thêm message
curl -b cookies.txt -X POST "http://localhost:8080/h/chats/{chatId}/messages" \
  -H "Content-Type: application/json" \
  -d '{"role":"USER","content":"Hello!"}'

# 5. Logout
curl -b cookies.txt -c cookies.txt -X POST "http://localhost:8080/h/auth/logout"
```

---

## 💾 Database Schema

```sql
CREATE TABLE users (
    id_user CHAR(36) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    access_token TEXT,
    refresh_token TEXT
);

CREATE TABLE chats (
    id_chat CHAR(36) PRIMARY KEY,
    id_user CHAR(36) NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE
);

CREATE TABLE messages (
    id_message CHAR(36) PRIMARY KEY,
    role ENUM('USER', 'ASSISTANT') NOT NULL,
    content TEXT NOT NULL,
    id_chat CHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_chat) REFERENCES chats(id_chat) ON DELETE CASCADE
);

CREATE INDEX idx_chat_user ON chats(id_user);
CREATE INDEX idx_message_chat ON messages(id_chat);
CREATE INDEX idx_chat_updated ON chats(updated_at DESC);
```

---

## 🚀 Next Steps

1. **WebSocket**: Real-time chat updates
2. **Pagination**: Paginate messages cho chats có nhiều messages
3. **Search**: Tìm kiếm trong chat history
4. **Export**: Export chat to PDF/TXT
5. **AI Integration**: Gọi OpenAI API để auto-reply
