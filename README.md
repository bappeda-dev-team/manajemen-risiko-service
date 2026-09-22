# 🛡️ Manajemen Risiko (ManRisk) API

> REST API untuk pengelolaan data manajemen risiko — **Pejabat Pengelola Informasi dan Dokumentasi (PPID) Kabupaten Mahakam Ulu**.

<p align="left">
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.0.7-6DB33F?logo=springboot&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white">
  <img alt="Flyway" src="https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-build-02303A?logo=gradle&logoColor=white">
  <img alt="OpenAPI" src="https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?logo=swagger&logoColor=black">
</p>

---

## ✨ Fitur

- **CRUD Risiko** — kelola data risiko secara lengkap (create, read, update, delete).
- **Respons API konsisten** — semua endpoint membungkus hasil dalam `ApiResponse` yang seragam.
- **Global exception handling** — penanganan error terpusat dengan tipe exception yang kaya (validation, not-found, forbidden, conflict, dll).
- **Database migrations** — skema dikelola otomatis dengan Flyway.
- **Dokumentasi API interaktif** — Swagger UI / OpenAPI siap pakai.
- **Konfigurasi berbasis environment** — kredensial & URL tidak di-hardcode, dibaca dari file `.env`.
- **Audit otomatis** — kolom `created_at` & `updated_at` melalui `BaseAuditable`.

---

## 🧰 Tech Stack

| Kategori        | Teknologi                                   |
|-----------------|---------------------------------------------|
| Bahasa          | Java 21                                      |
| Framework       | Spring Boot 4.0.7 (Web MVC, Data JPA, Actuator) |
| Database        | PostgreSQL                                   |
| Migrasi DB      | Flyway                                        |
| Dokumentasi     | springdoc-openapi (Swagger UI)              |
| HTTP Client     | Apache HttpClient 5                          |
| Build Tool      | Gradle (wrapper)                             |
| Utilitas        | Lombok, spring-dotenv                       |

---

## 🚀 Memulai

### Prasyarat

- **JDK 21+**
- **PostgreSQL** yang sedang berjalan
- Tidak perlu install Gradle — gunakan wrapper (`./gradlew`)

### 1. Siapkan database

```sql
CREATE DATABASE manajemen_risiko;
```

### 2. Konfigurasi environment

Salin template dan sesuaikan nilainya:

```bash
cp .env.example .env
```

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/manajemen_risiko
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
SERVER_PORT=8080
SWAGGER_SERVER_URL=http://localhost:8080/manrisk/api
```

> ℹ️ File `.env` sudah di-`.gitignore` — jangan pernah commit kredensial asli. Gunakan `.env.example` sebagai referensi.

### 3. Jalankan aplikasi

```bash
./gradlew bootRun
```

Flyway akan otomatis menjalankan migrasi (`V1__create_risiko_table.sql`) saat startup.

Aplikasi berjalan di **`http://localhost:8080/manrisk/api`**.

---

## 📖 Dokumentasi API

Setelah aplikasi berjalan, buka:

- **Swagger UI** → http://localhost:8080/manrisk/api/swagger-ui.html
- **OpenAPI JSON** → http://localhost:8080/manrisk/api/v3/api-docs

Swagger UI dilindungi **Basic Auth** — masukkan username & password saat diminta.

---

## 🔌 Endpoint

Base path: `/manrisk/api/risiko`

| Method   | Endpoint        | Deskripsi                              |
|----------|-----------------|----------------------------------------|
| `GET`    | `/risiko`       | Ambil semua data risiko                |
| `GET`    | `/risiko/{id}`  | Ambil data risiko berdasarkan ID       |
| `POST`   | `/risiko`       | Simpan data risiko baru                |
| `PUT`    | `/risiko/{id}`  | Ubah data risiko berdasarkan ID        |
| `DELETE` | `/risiko/{id}`  | Hapus data risiko berdasarkan ID       |

### Contoh: buat risiko baru

```bash
curl -X POST http://localhost:8080/manrisk/api/risiko \
  -H "Content-Type: application/json" \
  -d '{
    "kodeOpd": "1.01.01",
    "kodeRisiko": "RSK-001",
    "tahun": 2026,
    "pernyataanRisiko": "Keterlambatan penyampaian informasi publik",
    "skalaKemungkinan": 3,
    "skalaDampak": 4
  }'
```

### Format respons

Semua respons mengikuti struktur `ApiResponse` yang seragam:

```json
{
  "success": true,
  "message": "Retrieved 1 data successfully",
  "data": { ... }
}
```

---

## 🗂️ Struktur Proyek

```
src/main/java/cc/kertaskerja/manrisk/
├── ManriskApplication.java        # Entry point
├── common/          # BaseAuditable (created_at / updated_at)
├── config/          # OpenApiConfig, RestTemplateConfig
├── controller/      # RisikoController — REST endpoints
├── dto/             # ApiResponse + Request/Response DTO
├── entity/          # Risiko (JPA entity)
├── exception/       # GlobalExceptionHandler + custom exceptions
├── repository/      # RisikoRepository (Spring Data JPA)
└── service/         # RisikoService — business logic

src/main/resources/
├── application.yml               # Konfigurasi (baca dari .env)
└── db/migration/                 # Skrip migrasi Flyway
```

---

## 🛠️ Perintah Berguna

```bash
./gradlew build          # Build + jalankan test
./gradlew bootRun        # Jalankan aplikasi
./gradlew test           # Jalankan test saja
./gradlew clean          # Bersihkan hasil build
```

---

## 🔒 Catatan Keamanan

- Kredensial database **tidak** di-hardcode — dibaca dari `.env` via `spring-dotenv`.
- File `.env` di-abaikan oleh git; commit hanya `.env.example`.
- Jika kredensial pernah ter-commit sebelumnya, **rotasi password** database Anda.

---

<p align="center"><sub>Dibuat untuk PPID Kabupaten Mahakam Ulu • Kertas Kerja</sub></p>
