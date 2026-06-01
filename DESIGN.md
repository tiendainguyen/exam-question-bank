# Thiết kế hệ thống — Web Ngân hàng câu hỏi (Exam Question-Bank)

> Tài liệu thiết kế độc lập. Sản phẩm này tách biệt với `spring-saas-support-ai`.
> Mục tiêu: portfolio Senior Java + AI integration.

> **Phạm vi MVP:** mindmap do **AI tạo, chỉ đọc** — KHÔNG chỉnh sửa. Các tính năng sửa mindmap / thêm-sửa dạng / tính lại vector được hoãn sang **post-MVP** (đánh dấu 🔵 trong tài liệu). Đa người dùng + làm bài + chấm + tô màu mastery vẫn nằm trong MVP.

---

## 1. Tóm tắt nghiệp vụ

Người dùng import 1 **đề minh họa** + một **folder kho đề (PDF)**. Hệ thống:
1. Trích xuất đề minh họa → JSON.
2. AI phân tích **các dạng bài** trong đề minh họa và **gợi ý dạng tương tự** (tối đa biến thể).
3. Sinh **mindmap**: node gốc = câu hỏi (Câu 1..50), câu cùng dạng gom thành 1 node (vd `2,3`). *(MVP: chỉ đọc; chỉnh sửa là 🔵 post-MVP.)*
4. **Luồng phụ (song song)**: chia thread trích xuất kho đề → gom kết quả → đợi luồng chính (phân loại dạng) → dùng **embedding** gán mỗi câu kho vào dạng → tạo cụm → insert DB.
5. **In đề**: quét mindmap Câu 1→cuối, xác định dạng → random câu từ DB theo dạng → tạo đề mới.
6. **Làm đề** (không giới hạn giờ): chọn đáp án → **AI chấm** (user sửa được kết quả) → xác định dạng nào đã làm đúng.
7. **Báo cáo mindmap**: khi nộp bài, tô màu node theo % đúng (100% / 80%+kèm số đúng-trên-tổng / chưa làm = trắng).
8. 🔵 **(post-MVP) Sửa dạng → tính lại vector**: user được thêm dạng mới / sửa dạng cũ / sửa lại toàn bộ mindmap. Khi submit thay đổi dạng, hệ thống **tính lại embedding/centroid** và phân loại lại các câu kho bị ảnh hưởng.

> **Đa người dùng (bắt buộc):** mindmap, dạng bài, lượt làm và thống kê mastery là **dữ liệu cá nhân hóa theo user** — phải cô lập theo `user_id`. Kho câu hỏi gốc (bank) có thể dùng chung, nhưng mọi tùy biến (mindmap đã sửa, dạng tự thêm, mastery) là riêng của từng người.

---

## 2. Stack công nghệ

| Layer | Công nghệ | Lý do |
|---|---|---|
| Backend | **Java 21 + Spring Boot 3.3+** | Portfolio Java; concurrency mạnh |
| LLM | **Spring AI + Anthropic Claude** | Phân tích dạng, sinh biến thể, chấm bài (structured output JSON) |
| Embeddings | **OpenAI `text-embedding-3-small`** | Phân loại câu kho vào dạng |
| Vector store | **PostgreSQL + PgVector** | Một DB cho cả quan hệ + vector |
| PDF parse | **Apache PDFBox / Tika** (+ **Tesseract/tess4j** cho đề scan) | Lấy text + layout; câu→JSON để LLM lo |
| Song song | **Java 21 Structured Concurrency (`StructuredTaskScope`)** + virtual threads; batch lớn dùng **Spring Batch** | Khớp đúng "chia thread → gom → đợi luồng chính" |
| Frontend | **React + Vite + TypeScript + Zustand + shadcn** | Đồng bộ hệ sinh thái hiện có |
| Mindmap UI | **React Flow (`@xyflow/react`)** | Custom node, tô màu, badge %, kéo-thả, edit |
| Auth / đa người dùng | **Spring Security + JWT (JJWT)** | Cô lập dữ liệu cá nhân hóa theo `user_id` |
| DB migration | Flyway | |

**Phương án ngôn ngữ:**
- **Khuyên dùng: Java-only.** Gọn, một runtime, structured concurrency lý tưởng cho luồng phụ.
- **Hybrid (tùy chọn):** nếu PDF nhiều bảng/cột phức tạp → tách 1 microservice Python (`pdfplumber`/`unstructured`) chỉ cho extraction, backend Java gọi qua REST. Chỉ làm khi PDFBox không đủ.

---

## 3. Mô hình dữ liệu (PostgreSQL)

> **Quy ước cô lập:** cột `user_id` (owner) đứng trên các bảng cá nhân hóa.
> Dữ liệu **dùng chung** (không có `user_id`): `bank_question`, kho đề gốc.
> Dữ liệu **riêng user**: `mindmap`, `question_type` (dạng user tự thêm/sửa), `exam_attempt`, `mastery_stat`, đề sinh ra.

```
app_user              -- người dùng
  id, email(unique), password_hash, display_name, created_at

exam_paper            -- đề thi (minh họa hoặc đề sinh ra)
  id, user_id FK (nullable cho đề mẫu hệ thống), name,
  source_type(ILLUSTRATIVE|GENERATED|BANK), created_at

question              -- câu hỏi thuộc 1 đề
  id, exam_paper_id FK, ordinal(1..N), stem(text), choices(jsonb),
  correct_answer, question_type_id FK (nullable)

question_type         -- dạng bài (có thể do AI sinh hoặc user tự tạo/sửa)
  id, user_id FK, name, description,
  origin(EXTRACTED|SUGGESTED|USER_CREATED|USER_EDITED),
  parent_type_id FK (nullable -- dạng gợi ý xuất phát từ dạng nào),
  centroid vector(1536) (nullable -- vector đại diện dạng, để classify),
  vector_dirty bool (true khi vừa sửa, cần tính lại centroid)

type_suggestion       -- log gợi ý biến thể (cap 5/ dạng gốc)
  id, source_type_id FK, suggested_type_id FK, transform_desc

bank_question         -- câu trong kho đề (DÙNG CHUNG, không user_id)
  id, exam_paper_id FK, stem, choices(jsonb), correct_answer,
  embedding vector(1536)

bank_question_type    -- gán câu kho ↔ dạng, RIÊNG theo user (cá nhân hóa)
  id, user_id FK, bank_question_id FK, question_type_id FK,
  classify_score float

mindmap               -- mindmap của 1 đề minh họa, RIÊNG theo user
  id, user_id FK, exam_paper_id FK, graph(jsonb -- nodes+edges), updated_at

exam_attempt          -- lượt làm 1 đề, RIÊNG theo user
  id, user_id FK, generated_exam_id FK, started_at, submitted_at(nullable)

attempt_answer        -- đáp án từng câu trong lượt làm
  id, attempt_id FK, question_id FK, user_answer,
  ai_grade(CORRECT|WRONG), user_override(nullable), final_grade

mastery_stat          -- thống kê theo dạng (để tô màu mindmap), RIÊNG theo user
  id, user_id FK, mindmap_id FK, question_type_id FK,
  correct_count, total_count, percent, color_band
```

**Lý do tách `bank_question_type` riêng:** câu kho dùng chung, nhưng mỗi user có thể tự định nghĩa/sửa dạng khác nhau → việc "câu này thuộc dạng nào" là **per-user**. Bảng nối này giữ phân loại riêng cho từng người mà không nhân bản kho câu hỏi.

Index vector:
```sql
CREATE INDEX ON bank_question     USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON question_type     USING hnsw (centroid  vector_cosine_ops);
```

---

## 4. Sơ đồ luồng chính + luồng phụ (đồng bộ hóa)

```
[Import đề minh họa + folder kho]
        │
   ┌────┴───────────────────────────────────────────┐
   │ LUỒNG CHÍNH                  │ LUỒNG PHỤ (song song)
   ▼                              ▼
ExtractionService              BankIngestionService
 đề minh họa → JSON             StructuredTaskScope.ShutdownOnFailure
   │                             ├─ fork: parse PDF #1
   ▼                             ├─ fork: parse PDF #2 (chia chunk)
TypeAnalysisService (LLM)       ├─ ...
 → dạng bài + dạng tương tự     └─ join()  → gom toàn bộ bank_question
   │                                  │ (embedding tính sẵn)
   ▼                                  │
MindmapService                        │
 build graph (gom câu cùng dạng)      │
   │                                  │
   └──────────────► [BARRIER] ◄───────┘
              chờ CẢ HAI xong
                     │
                     ▼
          ClassificationService
   với mỗi bank_question:
     cosine(embedding, centroid mỗi dạng) → top-1 ≥ ngưỡng
     gán question_type_id + classify_score
     (tạo cụm/cluster theo dạng)
                     │
                     ▼
            insert/update DB
```

Đồng bộ hóa "đợi luồng chính": dùng `CompletableFuture.allOf(mainFlow, bankFlow).join()` hoặc một `CountDownLatch`; phần classification **chỉ chạy sau** khi có cả danh sách dạng (luồng chính) lẫn embedding kho (luồng phụ).

---

## 5. Mindmap — schema JSON (React Flow)

```jsonc
{
  "nodes": [
    {
      "id": "q1",
      "type": "questionNode",
      "data": {
        "label": "Câu 1",
        "ordinals": [1],            // node có thể gom nhiều câu: [2,3]
        "questionTypeId": "t-comp-superlative",
        "typeName": "So sánh hơn nhất → so sánh hơn",
        "mastery": {                // cập nhật khi nộp bài
          "percent": 80,
          "correct": 4, "total": 5,
          "colorBand": "AMBER"      // GREEN=100% / AMBER=≥80% / RED=<80% / WHITE=chưa làm
        }
      },
      "position": { "x": 0, "y": 0 }
    }
  ],
  "edges": [ { "id": "e-root-q1", "source": "root", "target": "q1" } ]
}
```

**Quy ước tô màu:**
- `WHITE` — dạng chưa làm.
- `GREEN` — đúng 100% (đánh dấu ✓).
- `AMBER`/màu tương ứng — ≥80%, hiển thị badge `correct/total`.
- (gợi ý thêm) `RED` — <80% để dễ nhìn lỗ hổng.

MVP: mindmap chỉ đọc (`mindmap.graph` do AI sinh). 🔵 Post-MVP: cho phép sửa cấu trúc rồi lưu lại `mindmap.graph`.

---

## 6. API Contract (REST, envelope `ApiResponse<T>`)

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/exams/illustrative` (multipart) | Upload đề minh họa PDF |
| POST | `/api/exams/bank` (multipart, folder) | Upload kho đề (nhiều PDF) → trigger luồng phụ |
| POST | `/api/exams/{id}/extract` | Extraction đề → JSON |
| POST | `/api/exams/{id}/analyze` | AI phân tích dạng + gợi ý dạng tương tự |
| GET | `/api/exams/{id}/types` | Danh sách dạng + dạng gợi ý |
| POST | `/api/exams/{id}/mindmap` | Sinh mindmap từ dạng |
| GET | `/api/mindmaps/{id}` | Lấy mindmap (MVP: chỉ đọc) |
| GET | `/api/exams/{id}/ingest-status` | Tiến độ luồng phụ (SSE/polling) |
| POST | `/api/exams/{id}/generate` | In đề: quét mindmap → random câu theo dạng |
| POST | `/api/attempts` | Bắt đầu làm 1 đề sinh ra |
| PATCH | `/api/attempts/{id}/answers` | Lưu/cập nhật đáp án từng câu |
| POST | `/api/attempts/{id}/grade` | AI chấm (trả ai_grade từng câu) |
| PATCH | `/api/attempts/{id}/answers/{qid}/override` | User sửa kết quả AI |
| POST | `/api/attempts/{id}/submit` | Nộp bài → tính mastery → cập nhật màu mindmap |
| GET | `/api/mindmaps/{id}/mastery` | Lấy báo cáo màu/dạng |
| 🔵 PUT | `/api/mindmaps/{id}` | *(post-MVP)* Lưu mindmap đã chỉnh sửa (cả cấu trúc câu + dạng) |
| 🔵 POST | `/api/types` | *(post-MVP)* Tạo dạng mới (user tự thêm) → `vector_dirty` |
| 🔵 PUT | `/api/types/{id}` | *(post-MVP)* Sửa dạng cũ → `vector_dirty=true` |
| 🔵 POST | `/api/mindmaps/{id}/resubmit` | *(post-MVP)* Submit lại dạng → tính lại centroid + phân loại lại |
| 🔵 GET | `/api/mindmaps/{id}/reclassify-status` | *(post-MVP)* Tiến độ tính lại vector |

---

## 7. Chi tiết các service then chốt

### ExtractionService
PDFBox/Tika lấy text → chia theo "Câu N" (regex/heuristic) → đưa block vào LLM với **structured output** (schema: stem, choices[], correctAnswer). Đề scan → OCR trước.

### TypeAnalysisService (LLM)
Prompt: "Phân loại dạng của mỗi câu; với mỗi dạng, đề xuất **tối đa 5** biến thể tương tự và mô tả phép biến đổi." Ví dụ: `so sánh hơn nhất → so sánh hơn`, `so sánh hơn → so sánh nhất`, `so sánh hơn → so sánh bằng`. Trả về `question_type[]` + `type_suggestion[]`.
- **Cap biến thể = 5/ dạng gốc** (hard limit) để đảm bảo độ chính xác, tránh nổ dạng. Nếu LLM trả nhiều hơn → cắt còn 5 (giữ điểm tương đồng cao nhất). Cap được enforce ở service, không phụ thuộc LLM tuân lệnh.

### BankIngestionService (song song)
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    List<Subtask<List<BankQuestion>>> tasks = pdfFiles.stream()
        .map(f -> scope.fork(() -> extractAndEmbed(f)))   // mỗi PDF/chunk 1 task
        .toList();
    scope.join().throwIfFailed();
    var all = tasks.stream().flatMap(t -> t.get().stream()).toList();
}
```
Mỗi câu kho được tính embedding ngay khi parse. Có thể chia nhỏ 1 PDF lớn thành chunk để fan-out sâu hơn.

### ClassificationService (embedding, per-user)
Sau barrier: với mỗi `bank_question`, tính cosine với **centroid** của từng dạng *của user hiện tại* (centroid = trung bình embedding các câu mẫu của dạng, hoặc embedding mô tả dạng). Gán dạng điểm cao nhất ≥ ngưỡng (vd 0.78); dưới ngưỡng → `UNCLASSIFIED` để review. Kết quả ghi vào `bank_question_type (user_id, ...)` — **không** sửa câu kho dùng chung.

### 🔵 TypeEditService + ReclassifyService (post-MVP — sửa dạng → tính lại vector)
> Không thuộc MVP. MVP dùng centroid tính một lần từ dạng AI sinh ra; phần dưới chỉ áp dụng khi mở tính năng chỉnh sửa.

Khi user thêm dạng mới / sửa dạng / sửa lại mindmap rồi **submit lại** (`POST /api/mindmaps/{id}/resubmit`):
1. Đánh dấu các `question_type.vector_dirty = true` cho dạng bị thêm/sửa.
2. Tính lại `centroid` cho từng dạng dirty (re-embed mô tả + câu mẫu mới).
3. **Phân loại lại** câu kho cho riêng user đó: chỉ chạy lại với các dạng dirty + các câu đang `UNCLASSIFIED` hoặc đang thuộc dạng dirty → cập nhật `bank_question_type` của user. Embedding câu kho (`bank_question.embedding`) **không đổi** → không phải re-embed cả kho, chỉ so lại với centroid mới.
4. Chạy nền (`@Async` / structured concurrency); FE theo dõi qua `reclassify-status`.

> Tối ưu: chỉ re-embed *dạng* (rẻ), không re-embed *câu kho* (đã có sẵn vector). Đây là lý do tách `centroid` ở `question_type` và `embedding` ở `bank_question`.

### ExamGenerationService
Duyệt mindmap theo `ordinal` tăng dần → mỗi câu lấy `questionTypeId` của node → chọn từ `bank_question` JOIN `bank_question_type` WHERE `user_id = :me AND question_type_id = X` ORDER BY random() LIMIT 1 (tránh trùng trong cùng đề). Ghép thành `exam_paper(source_type=GENERATED, user_id=:me)`.

### GradingService (LLM) + override
AI chấm từng câu → `ai_grade`. User có thể `override`. `final_grade = override ?? ai_grade`. Không cần làm hết đề — chỉ tổng hợp các câu/dạng đã làm.

### MasteryService
Khi submit: nhóm `attempt_answer` theo `question_type_id`, tính `percent = correct/total`, map sang `colorBand`, ghi `mastery_stat`, cập nhật `mindmap.graph` để FE render màu.

---

## 8. Cô lập đa người dùng

**Mục tiêu:** mindmap, dạng bài, mastery được cá nhân hóa — user A không thấy/sửa dữ liệu user B; kho câu hỏi gốc dùng chung.

**Cách làm (row-level theo `user_id`, dùng lại pattern row-level đã quen):**
- Auth: Spring Security + JWT, claim `sub = userId`. Lấy user hiện tại qua `@AuthenticationPrincipal`.
- Mọi bảng cá nhân hóa có `user_id`; repository **luôn** filter theo user hiện tại.
- Tùy chọn defense-in-depth: dùng Hibernate `@Filter` tự áp `WHERE user_id = :uid` (giống `TenantContext`/`@Filter` trong repo support-ai) để không lệ thuộc dev nhớ thêm điều kiện.
- `bank_question` (kho gốc) **không** có `user_id` → chia sẻ; phân loại riêng nằm ở `bank_question_type(user_id, ...)`.
- Quyền sửa: chỉ owner của `mindmap`/`question_type` được PUT/POST; kiểm tra `resource.userId == currentUserId` trước mọi thao tác ghi.

```
JWT(sub=userId) → SecurityFilter set principal
   → Service đọc currentUserId
      → Repository.findByUserId(currentUserId, ...) / kiểm tra owner trước khi ghi
```

---

## 9. Những điểm cần quyết tiếp (khi triển khai)

- Ngưỡng cosine để gán dạng (tinh chỉnh theo dữ liệu thật).
- Centroid theo embedding câu mẫu hay theo mô tả dạng? (nên thử cả hai).
- Áp Hibernate `@Filter` per-user hay chỉ filter thủ công ở repository (đánh đổi an toàn vs đơn giản).
- SSE hay polling cho tiến độ luồng phụ (SSE mượt hơn).
- 🔵 (post-MVP) Khi mở chỉnh sửa dạng: tính lại vector đồng bộ hay chạy nền.

---

## 10. Ranh giới MVP

**Trong MVP:** import đề + kho → extraction → AI phân tích dạng (cap 5 biến thể) → mindmap AI tạo (chỉ đọc) → luồng phụ embedding + phân loại kho → in đề random → làm bài + AI chấm (user sửa kết quả chấm) → tô màu mastery khi nộp → đa người dùng (cô lập theo `user_id`).

**🔵 Hoãn sang post-MVP:** chỉnh sửa cấu trúc mindmap, thêm/sửa dạng, tính lại centroid/vector khi sửa dạng (`TypeEditService`/`ReclassifyService` + các endpoint `PUT /mindmaps`, `/types`, `/resubmit`).

---

## 11. Thứ tự build MVP

Sắp theo dependency, mỗi bước cố gắng **demo được** để kiểm chứng sớm. Trong-ngoặc là phụ thuộc.

### Bước 0 — Nền tảng *(không phụ thuộc)*
- Scaffold Spring Boot 3.3 + Java 21, React + Vite + TS.
- Postgres + **PgVector** + Flyway; bật extension `vector`.
- Auth: Spring Security + JWT, bảng `app_user`, signup/login.
- Cô lập per-user: lấy `currentUserId`, repository filter theo `user_id`.
- **Demo:** đăng ký/đăng nhập, gọi 1 endpoint bảo vệ.

### Bước 1 — Import + Extraction đề minh họa *(cần B0)*
- Upload PDF đề minh họa; lưu file + `exam_paper(ILLUSTRATIVE)`.
- `ExtractionService`: PDFBox/Tika → tách "Câu N" → LLM structured output → `question[]`.
- **Demo:** upload 1 đề → trả JSON câu hỏi đã cấu trúc.

### Bước 2 — AI phân tích dạng *(cần B1)*
- `TypeAnalysisService`: phân loại dạng + gợi ý biến thể **cap 5**.
- Lưu `question_type[]` + `type_suggestion[]`, gắn `question.question_type_id`.
- Tính `centroid` cho mỗi dạng (re-embed mô tả + câu mẫu).
- **Demo:** xem danh sách dạng + biến thể của đề.

### Bước 3 — Sinh mindmap (chỉ đọc) + render *(cần B2)*
- `MindmapService`: build graph, gom câu cùng dạng thành 1 node (vd `2,3`).
- FE: React Flow render mindmap read-only.
- **Demo:** thấy mindmap dạng bài từ đề minh họa.

### Bước 4 — Luồng phụ: ingest kho + phân loại *(cần B2 cho centroid)*
- `BankIngestionService`: StructuredTaskScope fan-out parse + embed kho PDF.
- Barrier chờ B2 (đã có centroid dạng) → `ClassificationService` gán câu kho vào dạng (per-user) → `bank_question_type`.
- **Demo:** upload kho → status tiến độ → mỗi dạng có N câu kho.

### Bước 5 — In đề random theo dạng *(cần B3 + B4)*
- `ExamGenerationService`: duyệt mindmap theo `ordinal` → random câu theo dạng từ `bank_question_type` của user → tạo `exam_paper(GENERATED)`.
- **Demo:** bấm "In đề" → ra đề mới gồm câu từ kho.

### Bước 6 — Làm bài + AI chấm + override *(cần B5)*
- Trang làm bài (không giới hạn giờ), lưu `attempt_answer`.
- `GradingService` chấm → `ai_grade`; user override → `final_grade`.
- **Demo:** làm vài câu → AI chấm → sửa lại kết quả sai.

### Bước 7 — Mastery + tô màu mindmap *(cần B3 + B6)*
- `MasteryService`: khi submit, gom theo dạng → `percent` → `colorBand` → cập nhật mindmap.
- FE: tô màu node (GREEN/AMBER/WHITE) + badge `correct/total`.
- **Demo:** nộp bài → mindmap đổi màu theo mức thành thạo. ✅ Hết MVP.

> **Đường tới ăn được sớm nhất (happy path):** B0 → B1 → B2 → B3 cho ra "upload đề → thấy mindmap dạng bài" — đã là một demo thuyết phục cho portfolio trước khi làm kho/chấm điểm.

---

## 12. Task breakdown (Epic → Task)

Mỗi `[ ]` là một task nhỏ, làm gọn trong 1 lần. `(BE)`=backend, `(FE)`=frontend, `(DB)`=migration, `(INFRA)`=hạ tầng, `(TEST)`=kiểm thử.

### Epic B0 — Nền tảng
- [ ] (INFRA) Scaffold Spring Boot 3.3 + Java 21 + Maven, thêm deps (web, data-jpa, security, validation, spring-ai-anthropic, spring-ai-openai, pgvector, flyway, jjwt)
- [ ] (INFRA) Scaffold React + Vite + TS + Tailwind/shadcn + Zustand
- [ ] (INFRA) docker-compose: postgres 16 + pgvector; `CREATE EXTENSION vector`
- [ ] (DB) Flyway baseline + bảng `app_user`
- [ ] (BE) `JwtService` (sign/verify, claim `sub=userId`)
- [ ] (BE) `SecurityConfig` + JWT filter, lấy `currentUserId` qua `@AuthenticationPrincipal`
- [ ] (BE) Endpoint `POST /api/auth/signup`, `POST /api/auth/login` + DTO records
- [ ] (BE) Base repository/helper filter theo `user_id`
- [ ] (FE) Trang Login/Signup, lưu token, `ProtectedRoute`
- [ ] (TEST) IT: 2 user không thấy dữ liệu của nhau

### Epic B1 — Import + Extraction đề minh họa
- [ ] (DB) Bảng `exam_paper`, `question` (+ migration)
- [ ] (BE) Endpoint upload PDF `POST /api/exams/illustrative` + lưu file
- [ ] (BE) PDF→text bằng PDFBox/Tika; (tùy) OCR Tesseract cho đề scan
- [ ] (BE) Tách block theo "Câu N" (regex/heuristic)
- [ ] (BE) `ExtractionService`: LLM structured output → `question[]` (stem, choices, correctAnswer)
- [ ] (BE) Persist `question`; endpoint `POST /api/exams/{id}/extract`
- [ ] (FE) Trang upload đề + hiển thị JSON câu hỏi đã trích
- [ ] (TEST) Unit: parse 1 PDF mẫu → đúng số câu

### Epic B2 — AI phân tích dạng
- [ ] (DB) Bảng `question_type` (+ `centroid vector(1536)`, `vector_dirty`), `type_suggestion`
- [ ] (BE) `TypeAnalysisService`: prompt phân loại dạng + gợi ý biến thể (structured output)
- [ ] (BE) Enforce **cap 5** biến thể/dạng ở service (cắt theo điểm tương đồng)
- [ ] (BE) Gắn `question.question_type_id`
- [ ] (BE) Tính `centroid` mỗi dạng (embedding mô tả + câu mẫu)
- [ ] (BE) Endpoint `POST /api/exams/{id}/analyze`, `GET /api/exams/{id}/types`
- [ ] (FE) Hiển thị danh sách dạng + biến thể
- [ ] (TEST) Unit: cap 5 luôn được enforce dù LLM trả nhiều hơn

### Epic B3 — Mindmap (chỉ đọc)
- [ ] (DB) Bảng `mindmap` (`graph jsonb`, `user_id`)
- [ ] (BE) `MindmapService`: build graph, gom câu cùng dạng thành 1 node (`ordinals[]`)
- [ ] (BE) Endpoint `POST /api/exams/{id}/mindmap`, `GET /api/mindmaps/{id}`
- [ ] (FE) Tích hợp React Flow, custom `questionNode` (label, ordinals, typeName)
- [ ] (FE) Render mindmap read-only từ `graph`
- [ ] (TEST) Unit: câu cùng dạng → cùng 1 node

### Epic B4 — Luồng phụ: ingest kho + phân loại
- [ ] (DB) Bảng `bank_question` (+ `embedding`), `bank_question_type`; HNSW index
- [ ] (BE) Endpoint upload kho `POST /api/exams/bank` (nhiều file)
- [ ] (BE) Tích hợp OpenAI embeddings (`text-embedding-3-small`)
- [ ] (BE) `BankIngestionService`: `StructuredTaskScope` fan-out parse + embed
- [ ] (BE) Barrier chờ B2 (centroid sẵn sàng) trước classify
- [ ] (BE) `ClassificationService`: cosine vs centroid, ngưỡng → `bank_question_type` (per-user); dưới ngưỡng = UNCLASSIFIED
- [ ] (BE) Chạy `@Async` + endpoint `GET /api/exams/{id}/ingest-status` (SSE/polling)
- [ ] (FE) Upload kho + thanh tiến độ + số câu/dạng
- [ ] (TEST) IT: kho mẫu → mỗi dạng nhận đúng nhóm câu; phân loại tách theo user

### Epic B5 — In đề random theo dạng
- [ ] (BE) `ExamGenerationService`: duyệt mindmap theo `ordinal`, random câu theo dạng từ `bank_question_type`, tránh trùng
- [ ] (BE) Endpoint `POST /api/exams/{id}/generate` → `exam_paper(GENERATED)`
- [ ] (FE) Nút "In đề" + hiển thị/preview đề mới
- [ ] (TEST) Unit: đề sinh ra phủ đúng các dạng trong mindmap

### Epic B6 — Làm bài + AI chấm + override
- [ ] (DB) Bảng `exam_attempt`, `attempt_answer`
- [ ] (BE) Endpoint `POST /api/attempts` (bắt đầu), `PATCH /api/attempts/{id}/answers`
- [ ] (BE) `GradingService` (LLM) → `ai_grade`; endpoint `POST /api/attempts/{id}/grade`
- [ ] (BE) Endpoint override `PATCH /api/attempts/{id}/answers/{qid}/override` → `final_grade`
- [ ] (FE) Trang làm bài (không giới hạn giờ), chọn đáp án, lưu
- [ ] (FE) Hiển thị AI chấm + cho sửa lại kết quả
- [ ] (TEST) Unit: `final_grade = override ?? ai_grade`

### Epic B7 — Mastery + tô màu mindmap
- [ ] (DB) Bảng `mastery_stat`
- [ ] (BE) `MasteryService`: gom `attempt_answer` theo dạng → `percent` → `colorBand`
- [ ] (BE) Endpoint `POST /api/attempts/{id}/submit` (không cần làm hết đề) + cập nhật `mindmap.graph`
- [ ] (BE) Endpoint `GET /api/mindmaps/{id}/mastery`
- [ ] (FE) Tô màu node (GREEN/AMBER/WHITE) + badge `correct/total`
- [ ] (TEST) Unit: 100%→GREEN, ≥80%→AMBER, chưa làm→WHITE
