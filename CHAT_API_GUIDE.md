# 💬 Chat API Documentation

## 📋 Entity Structure

### **User Entity**

```java
{
  "idUser": "UUID",
  "userName": "string",
  "password": "string (hashed)",
  "accessToken": "string",
  "refreshToken": "string"
}
```

### **Chat Entity**

```java
{
  "idChat": "UUID",
  "idUser": "UUID",
  "title": "string",
  "messages": [Message],
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

### **Message Entity**

```java
{
  "idMessage": "UUID",
  "role": "USER | ASSISTANT",
  "content": "string",
  "idChat": "UUID",
  "createdAt": "LocalDateTime"
}
```

---

## 🔗 Database Relationships

```
┌─────────┐           ┌─────────┐           ┌─────────────┐
│  User   │ 1     ∞   │  Chat   │ 1     ∞   │   Message   │
│─────────│───────────│─────────│───────────│─────────────│
│ idUser  │           │ idChat  │           │ idMessage   │
│ username│           │ idUser  │───┐       │ role        │
│ password│           │ title   │   │       │ content     │
│ tokens  │           │ msgs[]  │   └──────►│ idChat      │
└─────────┘           └─────────┘           │ createdAt   │
                                             └─────────────┘

Relationships:
- User → Chat: OneToMany (1 user có nhiều chats)
- Chat → Message: OneToMany (1 chat có nhiều messages)
- Message → Chat: ManyToOne (nhiều messages thuộc 1 chat)
```

---

## 🚀 API Endpoints

### **1. Tạo Chat Mới**

**POST** `/api/chats?userId={userId}`

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
curl -X POST "http://localhost:8080/api/chats?userId=user-uuid" \
  -H "Content-Type: application/json" \
  -d '{"title":"My First Chat"}'
```

---

### **2. Lấy Tất Cả Chats của User**

**GET** `/api/chats/user/{userId}`

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
curl "http://localhost:8080/api/chats/user/user-uuid"
```

---

### **3. Lấy Chi Tiết Chat (với Messages)**

**GET** `/api/chats/{chatId}?userId={userId}`

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
curl "http://localhost:8080/api/chats/chat-uuid?userId=user-uuid"
```

---

### **4. Thêm Message vào Chat**

**POST** `/api/chats/{chatId}/messages?userId={userId}`

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
curl -X POST "http://localhost:8080/api/chats/chat-uuid/messages?userId=user-uuid" \
  -H "Content-Type: application/json" \
  -d '{"role":"USER","content":"Hello AI!"}'
```

---

### **5. Cập Nhật Title Chat**

**PUT** `/api/chats/{chatId}/title?userId={userId}`

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
curl -X PUT "http://localhost:8080/api/chats/chat-uuid/title?userId=user-uuid" \
  -H "Content-Type: application/json" \
  -d '{"title":"My Updated Chat"}'
```

---

### **6. Xóa Chat**

**DELETE** `/api/chats/{chatId}?userId={userId}`

**Response:**

```json
{
  "message": "Chat deleted successfully"
}
```

**cURL Example:**

```bash
curl -X DELETE "http://localhost:8080/api/chats/chat-uuid?userId=user-uuid"
```

---

### **7. Xóa Tất Cả Chats của User**

**DELETE** `/api/chats/user/{userId}`

**Response:**

```json
{
  "message": "All chats deleted successfully"
}
```

**cURL Example:**

```bash
curl -X DELETE "http://localhost:8080/api/chats/user/user-uuid"
```

---

## 📊 Full Workflow Example

### **Scenario: User tạo chat và gửi messages**

```bash
# 1. User login (nhận userId)
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"123456"}'
# Response: {"userId":"abc-123","username":"john",...}

# 2. Tạo chat mới
curl -X POST "http://localhost:8080/api/chats?userId=abc-123" \
  -H "Content-Type: application/json" \
  -d '{"title":"AI Conversation"}'
# Response: {"idChat":"chat-xyz",...}

# 3. User gửi message
curl -X POST "http://localhost:8080/api/chats/chat-xyz/messages?userId=abc-123" \
  -H "Content-Type: application/json" \
  -d '{"role":"USER","content":"What is Spring Boot?"}'

# 4. AI response (simulate)
curl -X POST "http://localhost:8080/api/chats/chat-xyz/messages?userId=abc-123" \
  -H "Content-Type: application/json" \
  -d '{"role":"ASSISTANT","content":"Spring Boot is..."}'

# 5. Lấy lịch sử chat
curl "http://localhost:8080/api/chats/chat-xyz?userId=abc-123"

# 6. Lấy tất cả chats của user
curl "http://localhost:8080/api/chats/user/abc-123"
```

---

## 🔐 Security Notes

1. **Authorization**: Hiện tại sử dụng `userId` trong query params. Trong production nên:

   - Lấy userId từ JWT token (từ HttpOnly cookie)
   - Validate quyền truy cập chat

2. **Suggested Update** cho ChatController:

```java
@GetMapping("/{chatId}")
public ResponseEntity<?> getChatById(
        @PathVariable UUID chatId,
        @CookieValue("accessToken") String accessToken) {
    // Extract userId from JWT token
    UUID userId = jwtUtil.extractUserId(accessToken);
    ChatResponse chat = chatService.getChatById(chatId, userId);
    return ResponseEntity.ok(chat);
}
```

---

## 💾 Database Schema (SQL)

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

## 🧪 Testing

### **Test tạo chat và messages:**

```bash
# Set user ID
USER_ID="your-user-uuid-here"

# 1. Create chat
CHAT_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/chats?userId=$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Chat"}')

# Extract chatId
CHAT_ID=$(echo $CHAT_RESPONSE | jq -r '.idChat')
echo "Created chat: $CHAT_ID"

# 2. Add user message
curl -X POST "http://localhost:8080/api/chats/$CHAT_ID/messages?userId=$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{"role":"USER","content":"Hello!"}'

# 3. Add assistant message
curl -X POST "http://localhost:8080/api/chats/$CHAT_ID/messages?userId=$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{"role":"ASSISTANT","content":"Hi there! How can I help?"}'

# 4. Get chat with messages
curl "http://localhost:8080/api/chats/$CHAT_ID?userId=$USER_ID" | jq
```

---

## ✅ Features

- ✅ One-to-Many relationship: User → Chats
- ✅ One-to-Many relationship: Chat → Messages
- ✅ Cascade delete: Xóa chat → xóa tất cả messages
- ✅ Auto timestamps: createdAt, updatedAt
- ✅ Message ordering: Messages sắp xếp theo thời gian
- ✅ Access control: Chỉ user owner mới truy cập được chat
- ✅ CRUD complete: Create, Read, Update, Delete

---

## 🚀 Next Steps

1. **Integrate AI/LLM**: Khi user gửi message, gọi OpenAI API để lấy response
2. **WebSocket**: Real-time chat updates
3. **Pagination**: Paginate messages cho chats có nhiều messages
4. **Search**: Tìm kiếm trong chat history
5. **Export**: Export chat to PDF/TXT
6. **Sharing**: Share chat với users khác
