# 01 — Selesaikan Add Table dan API Risiko Pemda

> Status: **implementasi backend selesai; validasi environment dan migrasi legacy masih terbuka**
> Disusun: 2026-09-23
> Branch kerja: `feature/add-table`
> Acuan frontend: `docs/todo/19-integrasi-risiko-pemda-paritas-opd.md` pada
> repository `manrisk-fraud-ui`

## Status implementasi — 23 September 2026

- [x] Branch backup `codex/feature-add-table-pre-main-merge` dibuat dari
  `f3baf69`.
- [x] `origin/main` digabungkan ke working tree `feature/add-table`; versi
  OPD, auth, exception, dan AI dari `main` dipertahankan.
- [x] Draft rename `Risiko` menjadi `RisikoOpd` dibatalkan sehingga kontrak
  OPD existing tidak berubah.
- [x] Migration V7, entity, repository, DTO mandiri, service CRUD, dan enam
  endpoint `/risiko-pemda` selesai diimplementasikan.
- [x] Pemda memakai `kode_sasaran_pemda`, tidak mempunyai `kode_opd`, dan
  menghasilkan kode stabil `RSK-PEM-*` dari ID database.
- [x] Lima tipe tab tervalidasi dan setiap item membawa `id` serta
  `kode_risiko`.
- [x] Filter token internal dan mode resource-server melindungi route Pemda.
- [x] Context AI mendukung discriminator `scope` untuk OPD/Pemda, menolak
  context campuran, dan memasukkan scope ke hash canonical.
- [x] Focused test CRUD/controller/security/AI/OPD lulus dan `bootJar` berhasil.
- [x] Production terverifikasi baru mencatat V1 dengan checksum `364566838`.
  V1 dipulihkan ke bentuk immutable yang masih memakai kolom `dampat`, lalu
  rename `dampat` menjadi `dampak` dipindahkan ke V2 dan migration lama
  V2–V6 digeser menjadi V3–V7.
- [ ] Konfirmasi environment persisten lain belum pernah menjalankan urutan
  lama V2–V6 sebelum sequence baru dideploy.
- [ ] Jalankan `ManriskApplicationTests.contextLoads()` pada mesin dengan
  Docker aktif. Pada mesin implementasi, 35 dari 36 test lulus dan satu test
  context tersebut gagal karena Testcontainers tidak menemukan Docker.
- [ ] Verifikasi mapping dan migrasikan data Pemda legacy setelah daftar resmi
  `kode_sasaran_pemda` tersedia; record ambigu tidak dimigrasikan otomatis.
- [ ] Jalankan smoke test HTTP terhadap PostgreSQL nyata dan lanjutkan kontrak
  BFF/frontend Plan 19.

## 1. Tujuan

Menyelesaikan implementasi tabel dan API Risiko Pemda yang sudah dimulai pada
commit `f3baf69`, lalu menyediakan kontrak backend yang dapat dipakai BFF/frontend
untuk mencapai paritas dengan Risiko OPD.

Target akhir backend:

- [x] Risiko Pemda disimpan di tabel `risiko_pemda`, terpisah dari tabel Risiko
      OPD `risiko`.
- [x] Referensi record memakai `kode_sasaran_pemda`, bukan
      `kode_sasaran_opd` dan bukan `kode_opd` palsu.
- [x] Tersedia endpoint list, list per sasaran/per tab, detail, create, update,
      dan delete dengan perilaku setara endpoint Risiko OPD.
- [x] Setiap item tab selalu membawa ID database dan `kode_risiko` yang benar.
- [x] Endpoint Pemda dilindungi internal token dan caller ID yang sama dengan
      endpoint Risiko OPD.
- [x] Generate AI menerima context Pemda secara eksplisit tanpa menyamarkan
      field Pemda sebagai field OPD.
- [x] Seluruh perbaikan keamanan, validasi, error envelope, dan test terbaru
      dari `main` tetap dipertahankan.

## 2. Baseline branch saat dokumen dibuat

### 2.1 Status Git

- [x] Branch lokal aktif: `feature/add-table`.
- [x] HEAD branch: `f3baf69` (`fixing errors`).
- [x] Branch berada **1 commit di depan** dan **12 commit di belakang** `main`.
- [x] Working tree bersih sebelum dokumen ini dibuat.
- [x] `compileJava` berhasil.
- [x] Test unit yang tidak membutuhkan Spring context berhasil.
- [ ] Full `test` belum hijau karena `ManriskApplicationTests.contextLoads()`
      membutuhkan Docker/Testcontainers, sedangkan Docker daemon tidak tersedia
      pada audit ini.

### 2.2 Yang sudah dimulai di `f3baf69`

- [x] Draft migration `V6__create_risiko_pemda_table.sql` sudah ada dan
      kemudian digeser menjadi V7 saat urutan migration diperbaiki.
- [x] Draft entity `RisikoPemda` sudah ada.
- [x] Draft request/response DTO Pemda sudah ada.
- [x] Draft `RisikoPemdaRepository` sudah ada.
- [x] Draft `RisikoPemdaService` sudah ada.
- [x] Risiko OPD mulai dipisah namanya menjadi `RisikoOpd`.

Bagian tersebut belum berarti fitur siap dipakai. Controller Pemda belum ada dan
beberapa draft class saling tidak cocok.

## 3. Gap nyata pada implementasi sekarang

### 3.1 Migration masih membuat bentuk OPD

Draft awal `V6__create_risiko_pemda_table.sql` masih mempunyai:

```sql
kode_opd VARCHAR(255),
kode_sasaran_opd VARCHAR(255)
```

Padahal tabel Pemda harus mempunyai `kode_sasaran_pemda` dan tidak membutuhkan
`kode_opd` sebagai owner. Bila migration ini dijalankan apa adanya, masalah
`KODE_OPD` palsu dari frontend hanya berpindah ke tabel baru.

### 3.2 Entity dan repository tidak konsisten

Entity `RisikoPemda` masih mendeklarasikan:

```java
@Column(name = "kode_sasaran_opd")
private String kodeSasaranOpd;
```

Tetapi repository mendeklarasikan:

```java
findByKodeSasaranPemda(String kodeSasaranPemda)
```

Method repository baru akan divalidasi saat Spring membuat bean. Property
`kodeSasaranPemda` belum ada pada entity, sehingga aplikasi berisiko gagal start
meskipun `compileJava` berhasil.

### 3.3 Service Pemda belum merupakan CRUD service

`RisikoPemdaService` baru mempunyai `getAllRisiko()` dan mapper detail. Masalah
lain di class yang sama:

- masih mengimpor `RisikoOpdReqDTO` dan `RisikoOpd`;
- `toEntity()` mengembalikan `RisikoOpd`, bukan `RisikoPemda`;
- masih bergantung pada `ExternalService` lama;
- belum mempunyai get-by-sasaran/type;
- belum mempunyai get-by-kode-risiko;
- belum mempunyai create, update, dan delete;
- belum membuat `kode_risiko`; dan
- belum menerapkan normalisasi, immutable reference, atau `RiskException` dari
  `main`.

### 3.4 DTO Pemda belum setara kontrak OPD terbaru

- Request DTO belum mempunyai Jakarta validation.
- Request DTO belum menolak unknown field.
- Response DTO mengimpor nested type dari `RisikoOpdResDTO`, sehingga kontrak
  Pemda terikat ke implementasi OPD.
- Nested `RisikoItem` Pemda belum mempunyai `id` dan `kode_risiko`.
- Bentuk response belum diuji untuk kelima nilai `type`.
- Belum ada discriminator/metadata `scope=pemda` pada response untuk validasi
  frontend.

### 3.5 Controller Pemda belum ada

Tidak ada `RisikoPemdaController`. Artinya tabel/service yang ditambahkan belum
dapat dipanggil oleh BFF maupun frontend.

### 3.6 Branch tertinggal dari arsitektur keamanan `main`

Branch masih membawa versi sebelum perubahan penting di `main`, antara lain:

- masih memakai `RisikoAiInternalAuthFilter`, bukan `RisikoInternalAuthFilter`;
- masih mempunyai `ExternalService`, `ApiProperties`, dan `RestTemplateConfig`;
- create Risiko OPD masih memanggil Penetapan dari backend;
- kode Risiko OPD masih dibuat acak;
- update masih dapat mengganti reference OPD;
- belum memakai validasi DTO dan `RiskException` terbaru;
- package AI dipindahkan ke lokasi lama; dan
- test Risiko/security terbaru belum ada.

Branch ini tidak boleh diselesaikan dengan menjadikan kode lama tersebut sebagai
baseline. `main` harus menjadi sumber kebenaran untuk semua file existing.

## 4. Keputusan arsitektur

### 4.1 Pemisahan memakai tabel dan endpoint, bukan kolom scope tunggal

Task ini meneruskan maksud branch `add-table`:

```text
Risiko OPD   → tabel risiko       → endpoint /risiko/**
Risiko Pemda → tabel risiko_pemda → endpoint /risiko-pemda/**
```

Isolasi scope dijamin oleh route, service, repository, dan tabel yang berbeda.
Frontend/BFF masih boleh memakai union `scope: "opd" | "pemda"`; BFF menerjemahkan
scope itu menjadi backend target yang eksplisit.

- [ ] Jangan menambahkan `kode_opd` ke tabel Pemda.
- [ ] Jangan menyimpan `kode_sasaran_pemda` di kolom `kode_sasaran_opd`.
- [ ] Jangan query dua tabel lalu menggabungkannya untuk endpoint per scope.
- [ ] Jangan memakai `KODE_OPD` Kesbangpol sebagai identitas Pemda.

### 4.2 Endpoint target

| Method | Endpoint backend | Tujuan |
| --- | --- | --- |
| GET | `/risiko-pemda` | List seluruh Risiko Pemda; admin/debug parity |
| GET | `/risiko-pemda/sasaran/{kodeSasaranPemda}?type={type}` | List tab per sasaran |
| GET | `/risiko-pemda/{kodeRisiko}` | Detail satu Risiko Pemda |
| POST | `/risiko-pemda` | Membuat Risiko Pemda |
| PUT | `/risiko-pemda/{id}` | Memperbarui field mutable |
| DELETE | `/risiko-pemda/{id}` | Menghapus berdasarkan ID |

Nilai `type` yang diperbolehkan:

```text
identifikasi
analisis
pengendalian
pemantauan
hasil-pemantauan
```

### 4.3 Format kode Risiko Pemda

Tabel OPD dan Pemda mempunyai sequence ID masing-masing. Format `RSK-%04d` pada
dua tabel dapat menghasilkan kode sama, misalnya keduanya membuat `RSK-0001`.

Keputusan target:

```text
Risiko OPD existing: RSK-0001
Risiko Pemda baru:   RSK-PEM-0001
```

- [ ] Generate kode setelah `saveAndFlush()` memakai ID database, bukan random.
- [ ] Pertahankan unique constraint `kode_risiko` pada tabel Pemda.
- [ ] Jangan melakukan loop random untuk mencari kode yang belum dipakai.

## 5. Fase 1 — Sinkronkan branch dengan `main`

Karena branch sudah dipublikasikan, gunakan merge biasa agar tidak memerlukan
force-push.

- [ ] Pastikan working tree bersih dan dokumen task sudah tersimpan.
- [ ] Fetch `origin/main` terbaru.
- [ ] Buat branch/tag backup dari `f3baf69` sebelum resolusi konflik.
- [ ] Merge `origin/main` ke `feature/add-table`.
- [ ] Saat konflik, pilih implementasi `main` untuk seluruh file existing terkait
      OPD, auth, security, exception, AI, config, dan tests.
- [ ] Reapply hanya penambahan spesifik Pemda: migration, entity, DTO,
      repository, service, controller, dan test Pemda.
- [ ] Jangan mempertahankan rename `Risiko` → `RisikoOpd` bila rename itu memaksa
      perubahan luas pada API OPD yang sudah stabil.
- [ ] Pertahankan package AI `service/risiko/ai` dan hapus perpindahan package
      yang tidak diperlukan oleh fitur Pemda.
- [ ] Pastikan `ExternalService`, `ApiProperties`, dan `RestTemplateConfig` tidak
      hidup kembali setelah merge.
- [ ] Jalankan `compileJava` segera setelah konflik selesai sebelum melanjutkan
      implementasi Pemda.

Struktur target mengikuti `main` dan menambahkan class Pemda secara paralel:

```text
controller/
├── RisikoController.java                 # OPD existing, jangan diregresikan
└── RisikoPemdaController.java            # baru

dto/
├── Risiko/                               # OPD existing
└── RisikoPemda/                          # baru

entity/
├── Risiko.java                           # OPD existing
└── RisikoPemda.java                      # baru

repository/
├── RisikoRepository.java                 # OPD existing
└── RisikoPemdaRepository.java            # baru

service/risiko/
├── RisikoService.java                    # OPD existing
├── RisikoPemdaService.java               # baru
└── ai/                                   # existing
```

## 6. Fase 2 — Benahi migration `risiko_pemda`

### 6.1 Pulihkan urutan migration yang kompatibel dengan production

- [x] Periksa `flyway_schema_history` production: hanya V1 yang tercatat.
- [x] Pulihkan isi V1 agar checksum kembali `364566838` dan tidak mengubah
      migration yang sudah pernah dijalankan.
- [x] Tambahkan V2 khusus untuk rename kolom `dampat` menjadi `dampak`.
- [x] Geser migration lama V2–V6 menjadi V3–V7 tanpa mengubah urutan operasi.
- [ ] Periksa `flyway_schema_history` pada environment persisten lain sebelum
      deployment; environment yang sudah mencatat V2–V6 lama perlu strategi
      rekonsiliasi tersendiri.

### 6.2 Bentuk tabel target

- [ ] Hapus `kode_opd` dari desain tabel Pemda.
- [ ] Ganti `kode_sasaran_opd` menjadi `kode_sasaran_pemda`.
- [ ] Jadikan `tahun`, `kode_sasaran_pemda`, `pernyataan_risiko`,
      `skala_kemungkinan`, `skala_dampak`, dan
      `rencana_tindak_pengendalian` non-null sesuai kontrak create.
- [ ] Tambahkan check constraint skala 1–5.
- [ ] Pertahankan narrative panjang sebagai `TEXT`.
- [ ] Pertahankan unique constraint `kode_risiko`.
- [ ] Tambahkan index `(kode_sasaran_pemda, id)` untuk query tab terurut.
- [ ] Pastikan `created_at` dan `updated_at` kompatibel dengan `BaseAuditable`.

Bentuk minimum target:

```sql
CREATE TABLE risiko_pemda (
  id BIGSERIAL PRIMARY KEY,
  kode_risiko VARCHAR(128) UNIQUE,
  tahun INTEGER NOT NULL,
  kode_sasaran_pemda VARCHAR(128) NOT NULL,
  -- field risiko umum sama dengan tabel OPD
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risiko_pemda_sasaran_id
  ON risiko_pemda (kode_sasaran_pemda, id);
```

## 7. Fase 3 — Entity, repository, dan DTO Pemda

### 7.1 Entity

- [ ] Ganti property entity menjadi `kodeSasaranPemda`.
- [ ] Map property ke kolom `kode_sasaran_pemda`.
- [ ] Pastikan entity tidak mempunyai `kodeOpd` atau `kodeSasaranOpd`.
- [ ] Samakan panjang/nullable JPA metadata dengan migration bila metadata itu
      dideklarasikan.
- [ ] Pertahankan seluruh field Risiko umum dan `BaseAuditable`.

### 7.2 Repository

- [ ] Pertahankan `findByKodeRisiko()`.
- [ ] Implementasikan query terurut
      `findByKodeSasaranPemdaOrderByIdAsc()`.
- [ ] Jangan memakai `findAll()` lalu filter kode sasaran di service.
- [ ] Tambahkan test repository bila Testcontainers tersedia.

### 7.3 Request DTO

- [ ] Gunakan `@JsonIgnoreProperties(ignoreUnknown = false)`.
- [ ] Tambahkan `@NotNull`, `@NotBlank`, `@Min`, `@Max`, dan `@Size` setara
      `RisikoReqDTO` OPD terbaru.
- [ ] Request hanya menerima `tahun` dan `kode_sasaran_pemda` sebagai reference;
      jangan menerima `kode_opd`/`kode_sasaran_opd`.
- [ ] Pertahankan semua field body umum agar wizard frontend dapat memakai mapper
      yang sama.

### 7.4 Response DTO

- [ ] Buat response Pemda mandiri; jangan mengimpor nested class DTO OPD.
- [ ] Outer wrapper per sasaran memuat `kode_sasaran_pemda`, `tahun`, dan
      `risiko`.
- [ ] Detail memuat seluruh field record termasuk `id`, `kode_risiko`,
      `created_at`, dan `updated_at`.
- [ ] Setiap nested `RisikoItem` wajib memuat `id`, `kode_risiko`, dan `type`
      sebelum field tahap tertentu.
- [ ] Tambahkan field response konstan `scope: "pemda"` bila frontend memakai
      discriminator tersebut untuk validasi lintas menu.
- [ ] Jangan mengisi tujuan/sasaran/indikator dari Penetapan di backend CRUD;
      context tersebut tetap diambil frontend dari API Penetapan Pemda.

## 8. Fase 4 — Implementasi service Pemda lengkap

- [ ] Pindahkan/letakkan service di package `service.risiko` mengikuti `main`.
- [ ] Hapus dependency `ExternalService`.
- [ ] Implementasikan `getAllRisiko()` dengan mapping detail.
- [ ] Implementasikan `getRisikoByKodeSasaranPemda(kode, type)`.
- [ ] Kembalikan wrapper sukses dengan `risiko: []` bila sasaran belum mempunyai
      record; jangan melempar 404 untuk empty list.
- [ ] Sort item berdasarkan ID ascending melalui repository.
- [ ] Implementasikan `getRisikoByKodeRisiko()` dengan `RiskException` 404 dan
      error code stabil.
- [ ] Implementasikan `createRisiko()`:
      1. normalisasi string;
      2. simpan dan flush untuk memperoleh ID;
      3. set `RSK-PEM-{id}`;
      4. simpan ulang; dan
      5. kembalikan detail response.
- [ ] Implementasikan `updateRisiko()` dan jadikan `tahun` serta
      `kode_sasaran_pemda` immutable; perubahan reference mengembalikan 409.
- [ ] Implementasikan delete berdasarkan ID dengan 404 bila tidak ditemukan.
- [ ] Map kelima tipe tab setara `RisikoService` OPD.
- [ ] Untuk `identifikasi`, tetap sertakan identitas item meskipun field tahap
      lanjut disembunyikan.
- [ ] Tolak `type` tidak valid sebelum mapping service dijalankan.
- [ ] Gunakan `@Transactional` pada create/update/delete sesuai kebutuhan.
- [ ] Jangan log body atau isi Risiko yang sensitif.

## 9. Fase 5 — Controller dan keamanan endpoint

### 9.1 Controller

- [ ] Buat `RisikoPemdaController` dengan base path `/risiko-pemda`.
- [ ] Implementasikan enam operasi pada tabel endpoint bagian 4.2.
- [ ] Gunakan `@Valid` pada POST/PUT.
- [ ] Validasi kode route nonblank/panjang aman dan ID integer positif.
- [ ] Batasi query `type` ke lima nilai yang didukung.
- [ ] Gunakan envelope `ApiResponse` dan status HTTP yang sama dengan OPD.
- [ ] Dokumentasikan endpoint melalui annotation OpenAPI tanpa mengekspos
      internal token.

### 9.2 Internal auth

- [ ] Perluas `RisikoInternalAuthFilter` dari `main` agar melindungi
      `/risiko-pemda` dan seluruh subpath-nya.
- [ ] Pertahankan constant-time token comparison.
- [ ] Wajibkan `X-Manrisk-User-Id` untuk endpoint Pemda seperti endpoint OPD.
- [ ] Pastikan mode `security=none` tidak membuat `/risiko-pemda/**` terbuka
      tanpa internal token.
- [ ] Tambahkan test filter untuk GET/POST Pemda tanpa token, token salah, caller
      kosong, dan request valid.
- [ ] Pastikan `GlobalExceptionHandler` memberi envelope Risiko yang stabil untuk
      path `/risiko-pemda/**`.

## 10. Fase 6 — Context Generate AI Pemda

Endpoint AI tetap satu orchestration service, tetapi context harus membedakan
OPD dan Pemda.

- [ ] Tambahkan discriminator `scope` pada context AI dengan nilai `opd` atau
      `pemda`.
- [ ] Pertahankan bentuk OPD existing agar frontend OPD tidak rusak.
- [ ] Untuk Pemda, terima `kode_tujuan_pemda`, `tujuan_pemda`,
      `kode_sasaran_pemda`, `sasaran_pemda`, indikator, target, satuan, tahun,
      dan pemilik risiko.
- [ ] Terapkan validasi conditional: context tidak boleh mencampur field OPD dan
      Pemda.
- [ ] Normalisasi kedua scope ke representasi prompt generik tanpa kehilangan
      kode asli pada canonical context.
- [ ] Hitung `contextHash` dari canonical context lengkap termasuk scope.
- [ ] Jangan membuat AI service menulis ke `risiko_pemda`; generate tetap operasi
      rekomendasi tanpa side effect database.
- [ ] Tambahkan unit test normalisasi context Pemda, field campuran, field wajib
      kosong, dan hash berbeda antar-scope.
- [ ] Pertahankan limiter, deadline, duplicate request guard, output validator,
      dan internal auth dari `main`.

## 11. Fase 7 — Test backend

### 11.1 Unit test `RisikoPemdaService`

- [ ] Empty sasaran menghasilkan wrapper dengan list kosong.
- [ ] Kelima tipe menghasilkan projection field yang benar.
- [ ] Setiap item mempunyai ID dan kode Risiko.
- [ ] Create memakai ID database untuk menghasilkan `RSK-PEM-*`.
- [ ] Dua create berturut-turut tidak menghasilkan kode ganda.
- [ ] Update mempertahankan reference Pemda.
- [ ] Update reference yang berbeda ditolak 409.
- [ ] Detail kode yang tidak ada menghasilkan 404 stabil.
- [ ] Delete memakai ID yang tepat dan 404 untuk ID tidak ada.
- [ ] String dinormalisasi sesuai perilaku OPD.

### 11.2 Controller/validation test

- [ ] POST valid menghasilkan 201.
- [ ] Payload tanpa `kode_sasaran_pemda` ditolak 400.
- [ ] Payload yang membawa `kode_sasaran_opd`/`kode_opd` ditolak sebagai unknown
      field.
- [ ] Skala di luar 1–5 ditolak.
- [ ] Unknown `type` ditolak 400.
- [ ] ID/path code invalid ditolak sebelum repository dipanggil.
- [ ] Empty list tetap 200.

### 11.3 Isolation dan regression

- [ ] Kode sasaran string sama pada tabel OPD dan Pemda tidak mencampur hasil.
- [ ] ID numerik sama pada dua tabel tidak membuat delete/update lintas tabel.
- [ ] Kode Risiko Pemda tidak collision dengan kode Risiko OPD.
- [ ] Seluruh focused test OPD, AI, security, dan exception dari `main` tetap
      lulus.
- [ ] `compileJava`, focused tests, dan `bootJar` lulus tanpa Docker.
- [ ] Full `test` dijalankan ketika Docker/Testcontainers tersedia.

## 12. Kontrak untuk BFF/frontend

### 12.1 Contoh create request backend Pemda

```json
{
  "tahun": 2026,
  "kode_sasaran_pemda": "SAS-PEM-001",
  "permasalahan": "...",
  "sebab_permasalahan": "...",
  "pernyataan_risiko": "...",
  "skala_kemungkinan": 3,
  "skala_dampak": 4,
  "pihak_terkena_risiko": "...",
  "rencana_tindak_pengendalian": "...",
  "metode_pemantauan": "...",
  "penanggungjawab_pemantauan": "...",
  "keterangan": "...",
  "realisasi_tindak_pengendalian": "...",
  "dapat_terkendali": "...",
  "dampak": "...",
  "catatan": "...",
  "perangkat_yang_menangani": "...",
  "kode_perangkat_yang_menangani": "8.01..."
}
```

Request tersebut tidak mempunyai `kode_opd` atau `kode_sasaran_opd`.

### 12.2 Mapping BFF yang diharapkan

```text
Browser scope=opd
  → BFF /api/risiko/**
  → backend /risiko/**

Browser scope=pemda
  → BFF /api/risiko/** (atau route eksplisit /api/risiko-pemda/**)
  → backend /risiko-pemda/**
```

- [ ] Sepakati route browser final dengan frontend sebelum implementasi BFF.
- [ ] Backend tidak menerima scope hanya untuk kemudian memilih repository secara
      dinamis; endpoint backend sudah menentukan tabel yang dipakai.
- [ ] Status/envelope error harus dapat diteruskan BFF tanpa remapping khusus.

## 13. Migrasi data Pemda legacy

Data Pemda yang telanjur disimpan pada tabel `risiko` memakai
`kode_sasaran_opd` dan `kode_opd` palsu. Data tersebut tidak otomatis pindah
hanya karena tabel baru dibuat.

- [ ] Inventaris kode sasaran dan jumlah record pada tabel `risiko`.
- [ ] Dapatkan daftar resmi `kode_sasaran_pemda` dari Penetapan/export.
- [ ] Pisahkan record yang pasti Pemda, pasti OPD, ambigu, dan tidak dikenal.
- [ ] Jangan menebak berdasarkan `KODE_OPD` saja karena nilainya merupakan kode
      OPD valid dan dapat dimiliki record OPD sungguhan.
- [ ] Buat migration/script data terpisah setelah mapping diverifikasi.
- [ ] Copy/move field common ke `risiko_pemda` dan isi
      `kode_sasaran_pemda` dari reference legacy yang tervalidasi.
- [ ] Generate/pertahankan kode Risiko dengan strategi yang tidak collision;
      dokumentasikan mapping kode lama ke kode baru bila diubah.
- [ ] Jangan menghapus record sumber sebelum count dan sample record
      terverifikasi.
- [ ] Simpan daftar record ambigu untuk rekonsiliasi manual.
- [ ] Siapkan backup dan rollback sebelum migrasi production.

## 14. Urutan implementasi

- [ ] Merge `main` ke `feature/add-table` dan selesaikan regression baseline.
- [x] Pulihkan V1 immutable dan geser migration berikutnya menjadi V2–V7.
- [ ] Benahi migration dan entity.
- [ ] Benahi repository dan DTO.
- [ ] Implementasikan service CRUD lengkap.
- [ ] Implementasikan controller.
- [ ] Perluas internal auth dan exception handling.
- [ ] Tambahkan seluruh focused test CRUD/security Pemda.
- [ ] Perluas context AI Pemda dan test-nya.
- [ ] Publikasikan kontrak final ke tim frontend/BFF.
- [ ] Jalankan verifikasi otomatis dan smoke test.
- [ ] Siapkan serta jalankan migrasi data legacy secara terkontrol.

## 15. Checklist smoke test

- [ ] `GET /risiko-pemda/sasaran/{kode}?type=identifikasi` tanpa data → 200 dan
      `risiko: []`.
- [ ] POST valid → 201 dan kode `RSK-PEM-*`.
- [ ] Lima GET tab mengembalikan ID/kode yang sama untuk record yang sama.
- [ ] GET detail berdasarkan kode → record lengkap.
- [ ] PUT field mutable → 200 dan perubahan tersimpan.
- [ ] PUT dengan tahun/kode sasaran berbeda → 409.
- [ ] DELETE berdasarkan ID → 200 lalu detail menjadi 404.
- [ ] Request tanpa internal token → 401/403 sesuai kontrak filter.
- [ ] Request tanpa caller ID → ditolak.
- [ ] Request dengan session backend/BFF valid → berhasil.
- [ ] Generate AI context Pemda untuk lima type → response valid dan tidak
      menulis record.
- [ ] Endpoint OPD existing tetap menghasilkan response yang sama sebelum dan
      sesudah perubahan.

## 16. Batas scope

- [ ] Tidak mengubah API Penetapan.
- [ ] Tidak menghidupkan kembali login/fetch Penetapan dari backend Risiko.
- [ ] Tidak mengimplementasikan UI frontend pada repository ini.
- [ ] Tidak menghubungkan Risiko Operasional.
- [ ] Tidak menambahkan role/ownership authorization baru di luar internal token
      dan caller ID existing.
- [ ] Tidak menghapus/memindahkan data legacy yang ambigu secara otomatis.

## 17. Definition of done

Task selesai bila tabel `risiko_pemda` memakai `kode_sasaran_pemda` yang benar,
seluruh CRUD dan lima projection tab tersedia melalui endpoint terproteksi,
Generate AI menerima context Pemda tanpa alias OPD, perbaikan terbaru `main`
tetap utuh, focused test dan build lulus, serta kontrak siap dikonsumsi Plan 19
frontend.
