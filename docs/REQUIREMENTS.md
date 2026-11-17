# Patungan — Requirement & PoC Specification

> Aplikasi backend untuk mencatat pengeluaran bersama dalam satu grup (liburan, kos, acara kantor),
> menghitung saldo tiap anggota, lalu merekomendasikan **transfer seminimal mungkin** untuk melunasi semuanya.

Dokumen ini adalah "kontrak" pengerjaan: apa yang dibuat, bagaimana bentuknya, dan kapan dianggap selesai.
Kerjakan dari atas ke bawah mengikuti bagian **Milestone** di akhir dokumen.

---

## Daftar Isi
1. [Tujuan & Pemetaan Skill](#1-tujuan--pemetaan-skill)
2. [Scope](#2-scope)
3. [Tech Stack](#3-tech-stack)
4. [Arsitektur](#4-arsitektur)
5. [Struktur Repository](#5-struktur-repository)
6. [Standar Umum (berlaku di semua service)](#6-standar-umum)
7. [user-service](#7-user-service)
8. [expense-service](#8-expense-service)
9. [settlement-service](#9-settlement-service)
10. [api-gateway](#10-api-gateway)
11. [Komunikasi Antar-Service](#11-komunikasi-antar-service)
12. [Containerization](#12-containerization)
13. [Skenario Uji & Expected Result](#13-skenario-uji--expected-result)
14. [Testing](#14-testing)
15. [Milestone & Checklist](#15-milestone--checklist)
16. [Checklist Sebelum Submit](#16-checklist-sebelum-submit)
17. [Bahan Diskusi Saat Interview](#17-bahan-diskusi-saat-interview)

---

## 1. Tujuan & Pemetaan Skill

Soal level Middle meminta 4 kemampuan. Setiap kemampuan **harus bisa ditunjukkan di file yang spesifik**,
supaya saat interview kamu bisa langsung membuka file-nya.

| Skill yang diminta | Diwujudkan sebagai | Lokasi |
|---|---|---|
| **Spring IoC** | Strategy pattern `SplitStrategy` (3 implementasi) di-inject sebagai `List` lalu dipetakan; `@ConfigurationProperties` untuk URL service lain; `@Configuration` + `@Bean` untuk `RestClient`; constructor injection di semua class | expense-service `service/split/`, `config/` |
| **Java Stream** | Algoritma penyederhanaan utang, penggabungan saldo (`toMap` + merge), `partitioningBy`, `groupingBy`, validasi total dengan `reduce` | settlement-service `service/DebtSimplifier`, expense-service `service/split/` |
| **Intermediate Native SQL** | CTE, `FULL OUTER JOIN`, `UNION ALL`, `GROUP BY` + `HAVING`, window function `RANK()` & `SUM() OVER`, pagination native dengan `countQuery`, interface projection | `repository/` di ketiga service |
| **Containerization & Microservices** | 3 service + 1 gateway, database per service, komunikasi REST antar-service dengan timeout & error handling, Dockerfile multi-stage, `docker compose up` satu perintah | root repo, setiap folder service |

---

## 2. Scope

### ✅ In scope (WAJIB)
- Kelola user dan grup beserta anggotanya.
- Catat pengeluaran dengan 3 tipe pembagian: `EQUAL`, `PERCENTAGE`, `EXACT`.
- Hitung saldo bersih tiap anggota di dalam grup.
- Laporan bulanan per kategori dan peringkat konsumsi anggota per kategori.
- Rekomendasi transfer pelunasan dengan jumlah transfer minimal (greedy).
- Catat pelunasan yang sudah dilakukan; rekomendasi ikut ter-update.
- Semua berjalan dengan `docker compose up --build`.

### ⭐ Nice to have (kerjakan jika waktu tersisa)
- Swagger UI per service (springdoc-openapi).
- Settlement-service memperkaya response dengan nama user (memanggil user-service).
- Hapus pengeluaran (`DELETE /api/expenses/{id}`).
- Postman collection dijalankan otomatis dengan Postman CLI.

### ❌ Out of scope (JANGAN dikerjakan — buang waktu)
- Login/JWT/Spring Security.
- Frontend.
- Integrasi pembayaran sungguhan, multi-currency.
- Eureka, Config Server, Kafka, Redis, Elasticsearch (itu materi level Senior).

> Lebih baik **sedikit fitur tapi rapi, teruji, dan bisa dijalankan** daripada banyak fitur setengah jadi.

---

## 3. Tech Stack

| Komponen | Pilihan | Catatan |
|---|---|---|
| Bahasa | **Java 21** (LTS) | |
| Framework | **Spring Boot 4.0.x** (versi stabil terbaru di start.spring.io) | Boleh 3.5.x kalau lebih familiar, tapi konsisten di semua service |
| Gateway | Spring Cloud Gateway | Gunakan release train Spring Cloud yang cocok dengan versi Boot (lihat tabel kompatibilitas di spring.io/projects/spring-cloud). Sejak Spring Cloud 2025.0 starter-nya bernama `spring-cloud-starter-gateway-server-webflux` |
| Database | **PostgreSQL 17** | 1 container, 3 database terpisah |
| Migrasi skema | **Flyway** | `spring.jpa.hibernate.ddl-auto=validate` — skema hanya dari Flyway |
| ORM | Spring Data JPA | Query penting **wajib native** (`nativeQuery = true`) |
| HTTP client | Spring `RestClient` | |
| Validasi | Jakarta Bean Validation (`spring-boot-starter-validation`) | |
| Monitoring | Spring Boot Actuator | endpoint `/actuator/health` |
| Build | Maven | |
| Test | JUnit 5, AssertJ, Mockito, **Testcontainers** (PostgreSQL) | |
| Container | Docker + Docker Compose | |
| Opsional | Lombok, springdoc-openapi | |

---

## 4. Arsitektur

```
                         ┌──────────────────────────────┐
     Client (Postman) ──▶│   api-gateway       :8080    │
                         └──────────────┬───────────────┘
              ┌─────────────────────────┼──────────────────────────┐
              ▼                         ▼                          ▼
   ┌────────────────────┐   ┌──────────────────────┐   ┌──────────────────────────┐
   │ user-service :8081 │◀──│ expense-service :8082│◀──│ settlement-service :8083 │
   │ user, grup, anggota│   │ pengeluaran, saldo,  │   │ rekomendasi transfer,    │
   │                    │   │ laporan              │   │ catatan pelunasan        │
   └─────────┬──────────┘   └──────────┬───────────┘   └────────────┬─────────────┘
             ▼                         ▼                            ▼
         user_db                   expense_db                 settlement_db
             └──────────── 1 container PostgreSQL :5432 ────────────┘
```

**Aturan arsitektur:**
1. Client **hanya** memanggil gateway (port 8080). Port service lain boleh di-expose untuk debugging saja.
2. Setiap service **hanya** mengakses database miliknya. Tidak ada query lintas database.
3. Karena database terpisah, **tidak ada foreign key lintas service** (misalnya `expenses.paid_by` tidak punya FK ke `users`). Validasi dilakukan lewat panggilan API.
4. Panggilan antar-service dilakukan **langsung** (`http://user-service:8081`), tidak lewat gateway.

| Service | Port | Database |
|---|---|---|
| api-gateway | 8080 | – |
| user-service | 8081 | `user_db` |
| expense-service | 8082 | `expense_db` |
| settlement-service | 8083 | `settlement_db` |
| postgres | 5432 | – |

---

## 5. Struktur Repository

```
patungan/
├── README.md
├── docker-compose.yml
├── .gitignore
├── docs/
│   └── REQUIREMENTS.md            ← dokumen ini
├── infra/
│   └── postgres/
│       └── init.sql               ← membuat 3 database
├── postman/
│   └── patungan.postman_collection.json
├── api-gateway/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/...
├── user-service/
├── expense-service/
└── settlement-service/
```

Setiap service adalah project Maven sendiri (punya `pom.xml` dan `Dockerfile` sendiri).

**Struktur package per service** (contoh expense-service; ganti `<namamu>` dengan nama kamu):

```
com.<namamu>.patungan.expense
├── ExpenseServiceApplication.java
├── config/          → RestClientConfig, ClientProperties (@ConfigurationProperties)
├── controller/      → ExpenseController, ReportController
├── dto/             → request/response sebagai Java record
├── entity/          → Expense, ExpenseShare
├── repository/      → ExpenseRepository (native query) + projection interface
├── service/
│   ├── ExpenseService.java
│   └── split/       → SplitStrategy, EqualSplitStrategy, PercentageSplitStrategy,
│                      ExactSplitStrategy, SplitStrategyResolver
├── client/          → UserServiceClient
└── exception/       → GlobalExceptionHandler, NotFoundException, BusinessException, dll.
```

---

## 6. Standar Umum

Berlaku di **semua** service.

### 6.1 Konvensi API
- Prefix semua endpoint: `/api`.
- JSON memakai `camelCase`.
- Tanggal: `YYYY-MM-DD`; timestamp: ISO-8601 (`2026-11-20T10:15:30`).
- Nominal uang: **`BigDecimal` di Java, `NUMERIC(15,2)` di database**. Jangan pakai `double`.
- Pagination: query param `page` (mulai dari 0) dan `size` (default 10, maksimal 50).

### 6.2 Format Error — gunakan `ProblemDetail` (RFC 9457, bawaan Spring 6+)
Semua error dikembalikan dalam format yang sama melalui `@RestControllerAdvice`:

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request tidak valid",
  "instance": "/api/expenses",
  "errorCode": "VALIDATION_ERROR",
  "errors": ["amount: must be greater than 0"]
}
```

| Kondisi | HTTP Status | `errorCode` |
|---|---|---|
| Body/param tidak valid (Bean Validation) | 400 | `VALIDATION_ERROR` |
| Data tidak ditemukan | 404 | `NOT_FOUND` |
| Melanggar aturan bisnis (misal total persen ≠ 100) | 422 | `BUSINESS_RULE_VIOLATION` |
| Data duplikat (email sudah dipakai, user sudah jadi anggota) | 409 | `CONFLICT` |
| Service lain tidak bisa dihubungi / timeout | 503 | `DEPENDENCY_UNAVAILABLE` |
| Error tak terduga | 500 | `INTERNAL_ERROR` |

### 6.3 Konfigurasi
- Semua nilai yang berbeda antar-environment (URL DB, password, URL service lain) dibaca dari **environment variable** dengan default untuk local, contoh:
  `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/expense_db}`
- Actuator: expose hanya `health` dan `info`.

### 6.4 Kode
- **Constructor injection** saja (tidak memakai `@Autowired` di field).
- DTO dipisah dari entity; entity tidak pernah dikembalikan langsung ke client.
- `@Transactional` di service layer, bukan di controller.

---

## 7. user-service

**Tanggung jawab:** menyimpan data user, grup, dan keanggotaan grup.

### 7.1 Skema Database (`V1__init.sql`)

```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE bill_groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    created_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE group_members (
    group_id    BIGINT    NOT NULL REFERENCES bill_groups(id) ON DELETE CASCADE,
    user_id     BIGINT    NOT NULL REFERENCES users(id),
    joined_at   TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);
```

> Tabel grup dinamai `bill_groups` karena `GROUPS` adalah keyword SQL dan bisa bermasalah di beberapa database.

### 7.2 Endpoint

| Method | Path | Deskripsi | Sukses |
|---|---|---|---|
| POST | `/api/users` | Buat user | 201 |
| GET | `/api/users/{id}` | Detail user | 200 |
| GET | `/api/users/{id}/groups` | Grup yang diikuti user + jumlah anggota (**native**) | 200 |
| POST | `/api/groups` | Buat grup; pembuat otomatis jadi anggota | 201 |
| GET | `/api/groups/{id}` | Detail grup + daftar anggota | 200 |
| POST | `/api/groups/{id}/members` | Tambah anggota | 201 |
| GET | `/api/groups/{id}/members` | Daftar anggota (**dipakai expense-service**) | 200 |

**Contoh request & response**

```http
POST /api/users
{ "name": "Budi", "email": "budi@example.com" }

→ 201
{ "id": 1, "name": "Budi", "email": "budi@example.com", "createdAt": "2026-11-20T10:00:00" }
```

```http
POST /api/groups
{ "name": "Liburan Bali", "createdBy": 1 }

→ 201
{ "id": 1, "name": "Liburan Bali", "createdBy": 1, "members": [ { "userId": 1, "name": "Budi" } ] }
```

```http
POST /api/groups/1/members
{ "userId": 2 }

→ 201
{ "groupId": 1, "userId": 2, "joinedAt": "2026-11-20T10:05:00" }
```

```http
GET /api/groups/1/members

→ 200
[
  { "userId": 1, "name": "Budi",  "email": "budi@example.com" },
  { "userId": 2, "name": "Ani",   "email": "ani@example.com" }
]
```

### 7.3 Aturan Bisnis
- Email unik → jika duplikat, 409.
- `createdBy` harus user yang ada → jika tidak, 404.
- Menambah anggota yang sudah ada → 409.
- Grup/user tidak ditemukan → 404.

### 7.4 Native Query

**Q-U1 — Grup yang diikuti user beserta jumlah anggotanya** (`JOIN` ganda + `GROUP BY`)

```sql
SELECT g.id                AS id,
       g.name              AS name,
       g.created_at        AS createdAt,
       COUNT(all_m.user_id) AS memberCount
FROM bill_groups g
JOIN group_members me    ON me.group_id = g.id AND me.user_id = :userId
JOIN group_members all_m ON all_m.group_id = g.id
GROUP BY g.id, g.name, g.created_at
ORDER BY g.created_at DESC
```

Hasil dipetakan ke **interface projection**:

```java
public interface UserGroupSummary {
    Long getId();
    String getName();
    LocalDateTime getCreatedAt();
    Long getMemberCount();
}
```

---

## 8. expense-service

**Tanggung jawab:** mencatat pengeluaran, membaginya ke peserta, menghitung saldo, dan membuat laporan.
**Ini service paling penting** — sebagian besar penilaian ada di sini.

### 8.1 Skema Database (`V1__init.sql`)

```sql
CREATE TABLE expenses (
    id            BIGSERIAL PRIMARY KEY,
    group_id      BIGINT        NOT NULL,
    paid_by       BIGINT        NOT NULL,
    description   VARCHAR(255)  NOT NULL,
    category      VARCHAR(30)   NOT NULL,
    amount        NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    split_type    VARCHAR(20)   NOT NULL,
    expense_date  DATE          NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE expense_shares (
    id            BIGSERIAL PRIMARY KEY,
    expense_id    BIGINT        NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id       BIGINT        NOT NULL,
    share_amount  NUMERIC(15,2) NOT NULL CHECK (share_amount >= 0),
    UNIQUE (expense_id, user_id)
);

CREATE INDEX idx_expenses_group_date ON expenses (group_id, expense_date);
CREATE INDEX idx_shares_user         ON expense_shares (user_id);
```

**Enum:**
- `Category`: `FOOD`, `TRANSPORT`, `ACCOMMODATION`, `ENTERTAINMENT`, `SHOPPING`, `OTHER`
- `SplitType`: `EQUAL`, `PERCENTAGE`, `EXACT`

### 8.2 Endpoint

| Method | Path | Deskripsi | Sukses |
|---|---|---|---|
| POST | `/api/expenses` | Catat pengeluaran + pembagian | 201 |
| GET | `/api/expenses/{id}` | Detail pengeluaran + rincian share | 200 |
| GET | `/api/expenses?groupId=&category=&page=&size=` | Daftar pengeluaran (**native + pagination**) | 200 |
| GET | `/api/balances?groupId=` | Saldo bersih tiap anggota (**native CTE**) | 200 |
| GET | `/api/reports/monthly?groupId=&year=&minTotal=` | Rekap bulanan per kategori (**native GROUP BY + HAVING**) | 200 |
| GET | `/api/reports/top-consumers?groupId=&topN=` | Peringkat konsumsi per kategori (**native window function**) | 200 |

**Contoh request — 3 tipe pembagian memakai bentuk yang sama:**

```http
POST /api/expenses
{
  "groupId": 1,
  "paidBy": 1,
  "description": "Hotel 2 malam",
  "category": "ACCOMMODATION",
  "amount": 3000000,
  "expenseDate": "2026-11-20",
  "splitType": "EQUAL",
  "participants": [ { "userId": 1 }, { "userId": 2 }, { "userId": 3 }, { "userId": 4 } ]
}
```

```json
"splitType": "PERCENTAGE",
"participants": [ { "userId": 1, "value": 40 }, { "userId": 2, "value": 20 },
                  { "userId": 3, "value": 20 }, { "userId": 4, "value": 20 } ]
```

```json
"splitType": "EXACT",
"participants": [ { "userId": 1, "value": 150000 }, { "userId": 3, "value": 150000 },
                  { "userId": 4, "value": 150000 } ]
```

Arti `value`: diabaikan untuk `EQUAL`, persen untuk `PERCENTAGE`, nominal untuk `EXACT`.

**Response:**

```json
{
  "id": 1,
  "groupId": 1,
  "paidBy": 1,
  "description": "Hotel 2 malam",
  "category": "ACCOMMODATION",
  "amount": 3000000.00,
  "splitType": "EQUAL",
  "expenseDate": "2026-11-20",
  "shares": [
    { "userId": 1, "shareAmount": 750000.00 },
    { "userId": 2, "shareAmount": 750000.00 },
    { "userId": 3, "shareAmount": 750000.00 },
    { "userId": 4, "shareAmount": 750000.00 }
  ]
}
```

### 8.3 Aturan Bisnis & Validasi

| # | Aturan | Jika dilanggar |
|---|---|---|
| E1 | `amount` > 0, `participants` minimal 1, `description` tidak kosong | 400 |
| E2 | Tidak boleh ada `userId` duplikat di `participants` | 422 |
| E3 | `paidBy` dan semua peserta **harus anggota grup** (cek ke user-service) | 422 |
| E4 | Grup tidak ditemukan di user-service | 404 |
| E5 | `PERCENTAGE`: total `value` harus tepat 100 | 422 |
| E6 | `EXACT`: total `value` harus tepat sama dengan `amount` | 422 |
| E7 | **Total semua `shareAmount` selalu = `amount`** (invariant, berlaku untuk semua tipe) | bug — harus dicegah oleh logika pembulatan |
| E8 | user-service mati/timeout | 503 |

**Aturan pembulatan** (penting untuk E7):
- **EQUAL**: `base = floor(amount / n)` dengan 2 desimal. Sisa sen dibagikan 1 sen per orang ke peserta pertama (urut `userId` ascending).
  Contoh: 100.000 dibagi 3 → `33.333,34` + `33.333,33` + `33.333,33` = 100.000 ✔
- **PERCENTAGE**: `share = amount × persen / 100`, dibulatkan `HALF_UP` ke 2 desimal. Selisih akibat pembulatan ditambahkan/dikurangkan ke peserta dengan persen terbesar (seri → `userId` terkecil).
- **EXACT**: nominal dipakai apa adanya (sudah divalidasi E6).

### 8.4 Desain Spring IoC — Strategy Pattern

```java
public interface SplitStrategy {
    SplitType type();
    List<ShareResult> split(BigDecimal amount, List<ParticipantRequest> participants);
}

@Component
public class EqualSplitStrategy implements SplitStrategy { ... }

@Component
public class PercentageSplitStrategy implements SplitStrategy { ... }

@Component
public class ExactSplitStrategy implements SplitStrategy { ... }

@Component
public class SplitStrategyResolver {
    private final Map<SplitType, SplitStrategy> strategies;

    // Spring meng-inject SEMUA bean yang mengimplementasikan SplitStrategy
    public SplitStrategyResolver(List<SplitStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(SplitStrategy::type, Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Duplicate strategy: " + a.type()); },
                        () -> new EnumMap<>(SplitType.class)));
    }

    public SplitStrategy resolve(SplitType type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseThrow(() -> new BusinessException("Split type tidak didukung: " + type));
    }
}
```

**Poin yang bisa dijelaskan:** menambah tipe baru (misalnya `SHARES` — dibagi berdasarkan porsi)
cukup dengan membuat class `@Component` baru, **tanpa mengubah kode lain** (Open/Closed Principle).

IoC lain yang wajib ada:
- `ClientProperties` dengan `@ConfigurationProperties(prefix = "clients.user-service")` berisi `baseUrl`, `connectTimeout`, `readTimeout`.
- `RestClientConfig` (`@Configuration`) yang membuat `@Bean RestClient userServiceRestClient(...)`.

### 8.5 Native Query

**Q-E1 — Saldo bersih per anggota** (CTE + `FULL OUTER JOIN`)

```sql
WITH paid AS (
    SELECT paid_by AS user_id, SUM(amount) AS total_paid
    FROM expenses
    WHERE group_id = :groupId
    GROUP BY paid_by
),
owed AS (
    SELECT s.user_id, SUM(s.share_amount) AS total_owed
    FROM expense_shares s
    JOIN expenses e ON e.id = s.expense_id
    WHERE e.group_id = :groupId
    GROUP BY s.user_id
)
SELECT COALESCE(p.user_id, o.user_id)                         AS userId,
       COALESCE(p.total_paid, 0)                              AS totalPaid,
       COALESCE(o.total_owed, 0)                              AS totalOwed,
       COALESCE(p.total_paid, 0) - COALESCE(o.total_owed, 0)  AS netBalance
FROM paid p
FULL OUTER JOIN owed o ON o.user_id = p.user_id
ORDER BY netBalance DESC, userId
```

> `netBalance` positif = orang lain berutang padanya; negatif = dia berutang.
> `FULL OUTER JOIN` dipakai karena ada user yang hanya membayar tanpa ikut menanggung, atau sebaliknya.

**Q-E2 — Rekap bulanan per kategori** (`GROUP BY` + `HAVING` + agregasi)

```sql
SELECT TO_CHAR(DATE_TRUNC('month', e.expense_date), 'YYYY-MM') AS month,
       e.category                                              AS category,
       COUNT(*)                                                AS transactionCount,
       SUM(e.amount)                                           AS totalAmount,
       ROUND(AVG(e.amount), 2)                                 AS averageAmount
FROM expenses e
WHERE e.group_id = :groupId
  AND e.expense_date >= MAKE_DATE(:year, 1, 1)
  AND e.expense_date <  MAKE_DATE(:year + 1, 1, 1)
GROUP BY DATE_TRUNC('month', e.expense_date), e.category
HAVING SUM(e.amount) >= :minTotal
ORDER BY month, totalAmount DESC
```

> Filter tahun memakai rentang tanggal (bukan `EXTRACT(YEAR ...)`) agar index `idx_expenses_group_date` tetap terpakai.

**Q-E3 — Peringkat konsumsi per kategori** (window function `RANK()` + subquery)

```sql
SELECT category, userId, totalConsumed, rankInCategory
FROM (
    SELECT e.category                                                         AS category,
           s.user_id                                                          AS userId,
           SUM(s.share_amount)                                                AS totalConsumed,
           RANK() OVER (PARTITION BY e.category ORDER BY SUM(s.share_amount) DESC) AS rankInCategory
    FROM expense_shares s
    JOIN expenses e ON e.id = s.expense_id
    WHERE e.group_id = :groupId
    GROUP BY e.category, s.user_id
) ranked
WHERE rankInCategory <= :topN
ORDER BY category, rankInCategory, userId
```

> Pakai `RANK()` (bukan `ROW_NUMBER()`) supaya anggota dengan nilai sama mendapat peringkat yang sama.

**Q-E4 — Daftar pengeluaran dengan filter opsional & pagination**

```java
@Query(value = """
        SELECT e.id            AS id,
               e.description   AS description,
               e.category      AS category,
               e.amount        AS amount,
               e.paid_by       AS paidBy,
               e.expense_date  AS expenseDate,
               COUNT(s.id)     AS participantCount
        FROM expenses e
        JOIN expense_shares s ON s.expense_id = e.id
        WHERE e.group_id = :groupId
          AND (CAST(:category AS VARCHAR) IS NULL OR e.category = :category)
        GROUP BY e.id
        ORDER BY e.expense_date DESC, e.id DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM expenses e
        WHERE e.group_id = :groupId
          AND (CAST(:category AS VARCHAR) IS NULL OR e.category = :category)
        """,
        nativeQuery = true)
Page<ExpenseSummary> findByGroup(@Param("groupId") Long groupId,
                                 @Param("category") String category,
                                 Pageable pageable);
```

> `CAST(:category AS VARCHAR)` diperlukan karena PostgreSQL tidak bisa menebak tipe parameter yang bernilai `null`.

### 8.6 Java Stream di expense-service
- `SplitStrategyResolver`: `List` → `EnumMap` dengan `Collectors.toMap`.
- Validasi E2: deteksi duplikat dengan `groupingBy(userId, counting())` lalu `filter(count > 1)`.
- Validasi E5/E6: total dengan `map(...).reduce(BigDecimal.ZERO, BigDecimal::add)`.
- Validasi E3: `participants.stream().map(userId).filter(not(memberIds::contains)).toList()` → kalau tidak kosong, tolak dan sebutkan user mana saja.

---

## 9. settlement-service

**Tanggung jawab:** menghitung rekomendasi transfer dan mencatat pelunasan yang sudah dilakukan.

### 9.1 Skema Database (`V1__init.sql`)

```sql
CREATE TABLE settlements (
    id            BIGSERIAL PRIMARY KEY,
    group_id      BIGINT        NOT NULL,
    from_user_id  BIGINT        NOT NULL,
    to_user_id    BIGINT        NOT NULL,
    amount        NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    note          VARCHAR(255),
    paid_at       TIMESTAMP     NOT NULL DEFAULT now(),
    CHECK (from_user_id <> to_user_id)
);

CREATE INDEX idx_settlements_group ON settlements (group_id, paid_at);
```

### 9.2 Endpoint

| Method | Path | Deskripsi | Sukses |
|---|---|---|---|
| GET | `/api/settlements/suggestions?groupId=` | Rekomendasi transfer minimal | 200 |
| POST | `/api/settlements` | Catat pelunasan yang sudah dilakukan | 201 |
| GET | `/api/settlements?groupId=` | Riwayat pelunasan + total kumulatif (**native window**) | 200 |

```http
GET /api/settlements/suggestions?groupId=1

→ 200
{
  "groupId": 1,
  "finalBalances": [
    { "userId": 1, "balance": 1100000.00 },
    { "userId": 3, "balance": 500000.00 },
    { "userId": 2, "balance": -750000.00 },
    { "userId": 4, "balance": -850000.00 }
  ],
  "transfers": [
    { "fromUserId": 4, "toUserId": 1, "amount": 850000.00 },
    { "fromUserId": 2, "toUserId": 1, "amount": 250000.00 },
    { "fromUserId": 2, "toUserId": 3, "amount": 500000.00 }
  ],
  "transferCount": 3
}
```

```http
POST /api/settlements
{ "groupId": 1, "fromUserId": 4, "toUserId": 1, "amount": 850000, "note": "Transfer BCA" }

→ 201
{ "id": 1, "groupId": 1, "fromUserId": 4, "toUserId": 1, "amount": 850000.00,
  "note": "Transfer BCA", "paidAt": "2026-12-05T09:00:00" }
```

### 9.3 Aturan Bisnis

| # | Aturan | Jika dilanggar |
|---|---|---|
| S1 | `fromUserId` ≠ `toUserId`, `amount` > 0 | 400 |
| S2 | `amount` pelunasan tidak boleh melebihi utang `fromUserId` saat ini | 422 |
| S3 | expense-service mati/timeout | 503 |

### 9.4 Cara Menghitung Saldo Akhir

```
saldo_akhir(user) = netBalance dari expense-service      (Q-E1)
                  + total yang sudah DIA BAYARKAN        (settlement sebagai pengirim)
                  − total yang sudah DIA TERIMA          (settlement sebagai penerima)
```

Contoh: Dodi saldo −850.000 lalu transfer 850.000 ke Budi → Dodi menjadi 0, Budi berkurang 850.000.

**Q-S1 — Total pelunasan bersih per user** (`UNION ALL` + agregasi)

```sql
SELECT user_id AS userId, SUM(delta) AS netSettled
FROM (
    SELECT from_user_id AS user_id,  amount AS delta FROM settlements WHERE group_id = :groupId
    UNION ALL
    SELECT to_user_id   AS user_id, -amount AS delta FROM settlements WHERE group_id = :groupId
) t
GROUP BY user_id
```

**Q-S2 — Riwayat dengan total kumulatif** (window function `SUM() OVER`)

```sql
SELECT id, from_user_id AS fromUserId, to_user_id AS toUserId, amount, note, paid_at AS paidAt,
       SUM(amount) OVER (ORDER BY paid_at, id) AS cumulativeSettled
FROM settlements
WHERE group_id = :groupId
ORDER BY paid_at, id
```

### 9.5 Algoritma Penyederhanaan Utang (Java Stream — inti penilaian)

Class: `DebtSimplifier` — **pure Java, tanpa dependensi Spring**, sehingga mudah di-unit-test.

**Langkah:**
1. **Gabungkan** saldo expense + pelunasan:
   ```java
   Map<Long, BigDecimal> finalBalances = Stream.concat(expenseBalances.stream(), settledBalances.stream())
           .collect(Collectors.toMap(Balance::userId, Balance::amount, BigDecimal::add));
   ```
2. **Sanity check**: total semua saldo harus 0 (`reduce`). Jika tidak, lempar exception — berarti ada bug.
3. **Pisahkan** creditor dan debtor, abaikan saldo 0:
   ```java
   Map<Boolean, List<Balance>> parts = finalBalances.entrySet().stream()
           .filter(e -> e.getValue().signum() != 0)
           .map(e -> new Balance(e.getKey(), e.getValue()))
           .collect(Collectors.partitioningBy(b -> b.amount().signum() > 0));
   ```
4. **Urutkan** keduanya berdasarkan nilai absolut terbesar; jika seri, `userId` terkecil dulu (agar hasil deterministik dan bisa dites).
5. **Greedy matching** (dua pointer): ambil creditor terbesar dan debtor terbesar →
   `transfer = min(sisa piutang, sisa utang)` → catat transfer → kurangi keduanya →
   maju ke orang berikutnya yang sisanya sudah 0. Ulangi sampai habis.

> Langkah 5 boleh memakai loop biasa — tidak semua hal harus dipaksakan menjadi stream.
> Justru bisa jadi poin diskusi: *"Saya pakai stream untuk transformasi data, loop untuk algoritma stateful."*

**Sifat hasil:** jumlah transfer paling banyak `n − 1` untuk `n` orang dengan saldo ≠ 0.
(Mencari jumlah transfer yang benar-benar minimum adalah masalah NP-hard; greedy sudah cukup baik untuk kasus nyata.)

---

## 10. api-gateway

**Tanggung jawab:** satu pintu masuk, meneruskan request ke service yang tepat.

| Route ID | Predicate (Path) | Tujuan |
|---|---|---|
| user-service | `/api/users/**`, `/api/groups/**` | `http://user-service:8081` |
| expense-service | `/api/expenses/**`, `/api/balances/**`, `/api/reports/**` | `http://expense-service:8082` |
| settlement-service | `/api/settlements/**` | `http://settlement-service:8083` |

Contoh `application.yml` (Spring Cloud 2025.x — perhatikan prefix property `server.webflux`):

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: user-service
              uri: ${USER_SERVICE_URL:http://localhost:8081}
              predicates:
                - Path=/api/users/**,/api/groups/**
            - id: expense-service
              uri: ${EXPENSE_SERVICE_URL:http://localhost:8082}
              predicates:
                - Path=/api/expenses/**,/api/balances/**,/api/reports/**
            - id: settlement-service
              uri: ${SETTLEMENT_SERVICE_URL:http://localhost:8083}
              predicates:
                - Path=/api/settlements/**
```

Tambahan wajib:
- Global filter sederhana yang me-log `method`, `path`, `status`, dan durasi tiap request.
- Path yang tidak cocok dengan route mana pun → 404.

---

## 11. Komunikasi Antar-Service

| Pemanggil | Dipanggil | Endpoint | Untuk apa |
|---|---|---|---|
| expense-service | user-service | `GET /api/groups/{id}/members` | Validasi E3 & E4 |
| settlement-service | expense-service | `GET /api/balances?groupId=` | Saldo dasar untuk rekomendasi |
| settlement-service *(nice to have)* | user-service | `GET /api/groups/{id}/members` | Menambahkan nama user di response |

**Ketentuan client:**
- Gunakan `RestClient` yang dibuat sebagai `@Bean`, dengan **connect timeout 2 detik** dan **read timeout 3 detik** (dari `@ConfigurationProperties`).
- Bungkus pemanggilan dalam class `XxxServiceClient` di package `client/`; service layer tidak tahu detail HTTP.
- Pemetaan error:
  - 404 dari service tujuan → teruskan sebagai 404 (`NOT_FOUND`).
  - Connection refused / timeout / 5xx → 503 (`DEPENDENCY_UNAVAILABLE`) dengan pesan yang menyebut nama service.
- **Uji manual wajib:** matikan user-service (`docker compose stop user-service`), lalu `POST /api/expenses` harus mengembalikan 503 dengan pesan jelas, bukan 500 dan bukan hang.

---

## 12. Containerization

### 12.1 Dockerfile (multi-stage) — sama untuk tiap service

```dockerfile
# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> `COPY pom.xml` + `dependency:go-offline` dipisah dari `COPY src` supaya layer dependency ter-cache — build ulang jauh lebih cepat.

### 12.2 `infra/postgres/init.sql`

```sql
CREATE DATABASE user_db;
CREATE DATABASE expense_db;
CREATE DATABASE settlement_db;
```

### 12.3 `docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_USER: patungan
      POSTGRES_PASSWORD: patungan
    ports:
      - "5432:5432"
    volumes:
      - ./infra/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U patungan"]
      interval: 5s
      timeout: 3s
      retries: 10

  user-service:
    build: ./user-service
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/user_db
      DB_USERNAME: patungan
      DB_PASSWORD: patungan
    ports: ["8081:8081"]
    depends_on:
      postgres:
        condition: service_healthy

  expense-service:
    build: ./expense-service
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/expense_db
      DB_USERNAME: patungan
      DB_PASSWORD: patungan
      CLIENTS_USER_SERVICE_BASE_URL: http://user-service:8081
    ports: ["8082:8082"]
    depends_on:
      postgres:
        condition: service_healthy

  settlement-service:
    build: ./settlement-service
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/settlement_db
      DB_USERNAME: patungan
      DB_PASSWORD: patungan
      CLIENTS_EXPENSE_SERVICE_BASE_URL: http://expense-service:8082
    ports: ["8083:8083"]
    depends_on:
      postgres:
        condition: service_healthy

  api-gateway:
    build: ./api-gateway
    environment:
      USER_SERVICE_URL: http://user-service:8081
      EXPENSE_SERVICE_URL: http://expense-service:8082
      SETTLEMENT_SERVICE_URL: http://settlement-service:8083
    ports: ["8080:8080"]
    depends_on:
      - user-service
      - expense-service
      - settlement-service

volumes:
  pgdata:
```

> Script `init.sql` hanya dijalankan saat volume `pgdata` masih kosong. Jika database tidak terbentuk, jalankan `docker compose down -v` lalu naikkan lagi.

**Definition of Done containerization:** di mesin yang bersih, `docker compose up --build` → semua container *running*,
dan skenario di bagian 13 berhasil dijalankan lewat `http://localhost:8080`.

---

## 13. Skenario Uji & Expected Result

Gunakan skenario ini untuk **seed data**, **Postman collection**, dan **integration test**.
Semua angka sudah dihitung — kalau hasil aplikasimu berbeda, berarti ada bug.

### 13.1 Data
| userId | Nama | Email |
|---|---|---|
| 1 | Budi | budi@example.com |
| 2 | Ani | ani@example.com |
| 3 | Citra | citra@example.com |
| 4 | Dodi | dodi@example.com |

Grup **1 — "Liburan Bali"**, dibuat oleh Budi, anggota: 1, 2, 3, 4.

| # | Dibayar oleh | Deskripsi | Kategori | Jumlah | Tanggal | Tipe | Pembagian |
|---|---|---|---|---|---|---|---|
| E1 | Budi (1) | Hotel 2 malam | ACCOMMODATION | 3.000.000 | 2026-11-20 | EQUAL | 1, 2, 3, 4 → @750.000 |
| E2 | Ani (2) | Makan malam seafood | FOOD | 600.000 | 2026-11-20 | EQUAL | 1, 2, 3 → @200.000 |
| E3 | Citra (3) | Sewa mobil | TRANSPORT | 2.000.000 | 2026-12-01 | PERCENTAGE | 1: 40% (800.000), 2/3/4: 20% (@400.000) |
| E4 | Dodi (4) | Snorkeling | ENTERTAINMENT | 450.000 | 2026-12-02 | EXACT | 1, 3, 4 → @150.000 |

### 13.2 Expected: `GET /api/balances?groupId=1`

| userId | totalPaid | totalOwed | netBalance |
|---|---|---|---|
| 1 Budi | 3.000.000 | 1.900.000 | **+1.100.000** |
| 3 Citra | 2.000.000 | 1.500.000 | **+500.000** |
| 2 Ani | 600.000 | 1.350.000 | **−750.000** |
| 4 Dodi | 450.000 | 1.300.000 | **−850.000** |
| **Total** | 6.050.000 | 6.050.000 | **0** |

### 13.3 Expected: `GET /api/settlements/suggestions?groupId=1`

| # | Dari | Ke | Jumlah |
|---|---|---|---|
| 1 | Dodi (4) | Budi (1) | 850.000 |
| 2 | Ani (2) | Budi (1) | 250.000 |
| 3 | Ani (2) | Citra (3) | 500.000 |

Langkah greedy: Budi (1.100.000) vs Dodi (850.000) → Dodi lunas, Budi sisa 250.000 →
Budi (250.000) vs Ani (750.000) → Budi lunas, Ani sisa 500.000 → Citra (500.000) vs Ani (500.000) → selesai.

### 13.4 Expected setelah `POST /api/settlements` (Dodi → Budi 850.000)

Saldo akhir: Budi +250.000, Citra +500.000, Ani −750.000, Dodi 0.

| # | Dari | Ke | Jumlah |
|---|---|---|---|
| 1 | Ani (2) | Citra (3) | 500.000 |
| 2 | Ani (2) | Budi (1) | 250.000 |

### 13.5 Expected: `GET /api/reports/monthly?groupId=1&year=2026&minTotal=0`

| month | category | transactionCount | totalAmount | averageAmount |
|---|---|---|---|---|
| 2026-11 | ACCOMMODATION | 1 | 3.000.000 | 3.000.000 |
| 2026-11 | FOOD | 1 | 600.000 | 600.000 |
| 2026-12 | TRANSPORT | 1 | 2.000.000 | 2.000.000 |
| 2026-12 | ENTERTAINMENT | 1 | 450.000 | 450.000 |

Dengan `minTotal=1000000` → hanya baris ACCOMMODATION dan TRANSPORT (membuktikan `HAVING` bekerja).

### 13.6 Expected: `GET /api/reports/top-consumers?groupId=1&topN=1`

| category | userId | totalConsumed | rank |
|---|---|---|---|
| ACCOMMODATION | 1, 2, 3, 4 | 750.000 | 1 (seri, 4 baris) |
| ENTERTAINMENT | 1, 3, 4 | 150.000 | 1 (seri, 3 baris) |
| FOOD | 1, 2, 3 | 200.000 | 1 (seri, 3 baris) |
| TRANSPORT | 1 | 800.000 | 1 |

> Total 11 baris. Perhatikan efek `RANK()`: nilai seri menghasilkan beberapa baris peringkat 1.
> Dengan `ROW_NUMBER()` hasilnya hanya 4 baris — ini bahan diskusi yang bagus.

### 13.7 Skenario Negatif (wajib dicoba)

| Kasus | Request | Expected |
|---|---|---|
| Persen tidak 100 | PERCENTAGE dengan 50 + 30 | 422 |
| EXACT tidak cocok | amount 450.000, total value 400.000 | 422 |
| Peserta bukan anggota | participant `userId: 99` | 422, pesan menyebut user 99 |
| Peserta duplikat | `userId: 1` dua kali | 422 |
| Grup tidak ada | `groupId: 99` | 404 |
| Amount negatif | `amount: -5000` | 400 |
| User-service mati | `docker compose stop user-service` lalu POST expense | 503 |
| Pelunasan melebihi utang | Ani → Budi 1.000.000 (utang Ani hanya 750.000) | 422 |
| Pembulatan EQUAL | 100.000 dibagi 3 | share 33.333,34 / 33.333,33 / 33.333,33 |

---

## 14. Testing

| Jenis | Yang dites | Minimal |
|---|---|---|
| **Unit test** | `EqualSplitStrategy`, `PercentageSplitStrategy`, `ExactSplitStrategy` (termasuk kasus pembulatan & validasi) | 3 test per strategy |
| **Unit test** | `DebtSimplifier` dengan skenario 13.3 dan 13.4, saldo kosong, satu creditor banyak debtor | 4 test |
| **Unit test** | `SplitStrategyResolver` memetakan semua `SplitType` | 1 test |
| **Integration test** (Testcontainers) | Q-E1 dan Q-E2 di PostgreSQL asli, memakai data skenario 13.1 | 2 test |
| **Integration test** (opsional) | `@WebMvcTest` untuk validasi 400/422 di controller | – |
| **End-to-end** | Postman collection menjalankan skenario 13 lewat gateway, lengkap dengan assertion | 1 collection |

Contoh integration test native query:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ExpenseRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired ExpenseRepository repository;

    @Test
    @Sql("/seed-scenario.sql")
    void shouldCalculateNetBalancePerMember() {
        var balances = repository.findBalancesByGroup(1L);
        assertThat(balances).extracting(BalanceView::getUserId, BalanceView::getNetBalance)
                .containsExactly(
                        tuple(1L, new BigDecimal("1100000.00")),
                        tuple(3L, new BigDecimal("500000.00")),
                        tuple(2L, new BigDecimal("-750000.00")),
                        tuple(4L, new BigDecimal("-850000.00")));
    }
}
```

> Catatan: di Spring Boot 4 beberapa anotasi test dipindah ke modul/package baru (misalnya `spring-boot-data-jpa-test`). Jika import tidak ditemukan, cek dokumentasi versi yang kamu pakai.

Menjalankan Postman collection dari terminal:

```bash
postman collection run postman/patungan.postman_collection.json
```

---

## 15. Milestone & Checklist

Kerjakan berurutan. Commit di setiap checklist yang selesai, dengan pesan yang jelas.

### M0 — Persiapan
- [ ] Buat repo Git (nama netral, misalnya `patungan-microservices`), tambahkan `.gitignore` (Java/Maven/IntelliJ).
- [ ] Salin dokumen ini ke `docs/REQUIREMENTS.md`.
- [ ] Buat `docker-compose.yml` berisi PostgreSQL saja + `infra/postgres/init.sql`; pastikan 3 database terbentuk.

### M1 — user-service
- [ ] Generate project dari start.spring.io (Web, Data JPA, PostgreSQL, Flyway, Validation, Actuator).
- [ ] Flyway `V1__init.sql` (bagian 7.1).
- [ ] Endpoint bagian 7.2 + aturan 7.3.
- [ ] Native query Q-U1 + projection.
- [ ] `GlobalExceptionHandler` dengan `ProblemDetail` (bagian 6.2) — bisa disalin ke service lain.
- [ ] ✅ **Selesai jika:** semua endpoint bisa dicoba via Postman di port 8081, error mengikuti format standar.

### M2 — expense-service *(alokasikan waktu paling banyak di sini)*
- [ ] Flyway `V1__init.sql` (bagian 8.1), enum `Category` & `SplitType`.
- [ ] `SplitStrategy` + 3 implementasi + `SplitStrategyResolver` (bagian 8.4) + unit test.
- [ ] `ClientProperties`, `RestClientConfig`, `UserServiceClient` (bagian 11).
- [ ] `POST /api/expenses` dengan semua aturan 8.3.
- [ ] `GET /api/expenses/{id}` dan list dengan pagination (Q-E4).
- [ ] `GET /api/balances` (Q-E1).
- [ ] `GET /api/reports/monthly` (Q-E2) dan `GET /api/reports/top-consumers` (Q-E3).
- [ ] Integration test Testcontainers untuk Q-E1 & Q-E2.
- [ ] ✅ **Selesai jika:** skenario 13.1 bisa diinput dan hasil 13.2, 13.5, 13.6 cocok persis.

### M3 — settlement-service
- [ ] Flyway `V1__init.sql` (bagian 9.1).
- [ ] `DebtSimplifier` (bagian 9.5) + unit test skenario 13.3 & 13.4 — **tulis test-nya dulu**.
- [ ] `ExpenseServiceClient`.
- [ ] Endpoint bagian 9.2 + aturan 9.3, native query Q-S1 & Q-S2.
- [ ] ✅ **Selesai jika:** hasil 13.3 dan 13.4 cocok persis.

### M4 — api-gateway
- [ ] Project Spring Cloud Gateway + routing (bagian 10) + logging filter.
- [ ] ✅ **Selesai jika:** semua endpoint bisa diakses lewat port 8080.

### M5 — Containerization penuh
- [ ] Dockerfile multi-stage untuk keempat service.
- [ ] `docker-compose.yml` lengkap (bagian 12.3).
- [ ] ✅ **Selesai jika:** `docker compose down -v && docker compose up --build` berhasil dan uji 503 (bagian 11) lolos.

### M6 — Postman & Testing
- [ ] Postman collection: skenario 13.1 → 13.6 berurutan + 13.7, dengan assertion (`pm.test`).
- [ ] Semua unit & integration test hijau (`mvn test` di tiap service).

### M7 — Dokumentasi & Polish
- [ ] `README.md` (lihat bagian 16).
- [ ] Hapus kode tidak terpakai, `System.out.println`, dan komentar TODO.
- [ ] Jalankan checklist submit (bagian 16).

---

## 16. Checklist Sebelum Submit

### README.md wajib berisi
- [ ] Deskripsi singkat aplikasi (2–3 kalimat).
- [ ] Diagram arsitektur (bisa menyalin dari bagian 4).
- [ ] Tabel pemetaan skill → file (bagian 1), **dengan link ke file aslinya** — memudahkan reviewer.
- [ ] Prasyarat (Docker, Java 21, Maven) dan cara menjalankan: `docker compose up --build`.
- [ ] Daftar endpoint + contoh request (curl) untuk skenario utama.
- [ ] Cara menjalankan test dan Postman collection.
- [ ] Keputusan desain & trade-off (ambil dari bagian 17).

### Pemeriksaan akhir
- [ ] Clone ulang repo ke folder lain → `docker compose up --build` → skenario 13 lancar.
- [ ] Repo **public** dan bisa dibuka tanpa login.
- [ ] Tidak ada password asli / file `.env` pribadi di repo.
- [ ] **Nama perusahaan yang dilarang di soal tidak muncul di mana pun** — termasuk nama repo, package, README,
      pesan commit, dan kata berarti "independen" dalam bahasa Indonesia yang ejaannya sama. Cek dengan:
      `git grep -i <kata>` dan `git log --all --format=%B | grep -i <kata>`.

---

## 17. Bahan Diskusi Saat Interview

Siapkan jawaban untuk pertanyaan-pertanyaan ini — kemungkinan besar akan ditanyakan:

| Topik | Poin jawaban |
|---|---|
| Kenapa tidak ada FK antar-service? | Database per service → tiap service bisa di-deploy & diskalakan sendiri. Konsistensi dijaga lewat validasi API. Trade-off: data bisa tidak konsisten jika user dihapus (solusi lanjutan: event/soft delete). |
| Kenapa `BigDecimal`? | `double` tidak presisi untuk uang (`0.1 + 0.2 ≠ 0.3`). Invariant total share = amount dijaga dengan aturan pembulatan yang jelas. |
| Kenapa Strategy pattern? | Open/Closed: tipe baru cukup menambah bean. Spring yang mengumpulkan semua implementasi lewat injection `List<SplitStrategy>`. |
| Kenapa native query, bukan JPQL? | Fitur seperti CTE, window function, `FULL OUTER JOIN`, `DATE_TRUNC` lebih alami/hanya tersedia di SQL native; hasil langsung dipetakan ke projection tanpa memuat entity. |
| `RANK()` vs `DENSE_RANK()` vs `ROW_NUMBER()` | Jelaskan dengan data skenario 13.6. |
| Kenapa filter tanggal pakai rentang? | Fungsi pada kolom (`EXTRACT`) membuat index tidak terpakai (non-sargable). |
| Apakah algoritma greedy optimal? | Tidak selalu minimum global (NP-hard), tapi menjamin ≤ n−1 transfer dan cepat (O(n log n)). |
| Apa yang terjadi jika user-service mati? | Timeout 2–3 detik → 503 dengan pesan jelas. Pengembangan lanjutan: circuit breaker (Resilience4j), retry, cache daftar anggota. |
| Bagaimana jika aplikasi ini dibawa ke production? | Autentikasi (JWT di gateway), service discovery, centralized logging & tracing, database terpisah per instance, CI/CD. |
| Kenapa Dockerfile multi-stage? | Image runtime kecil (hanya JRE + jar), tidak membawa Maven & source code, layer dependency ter-cache, berjalan sebagai non-root. |
