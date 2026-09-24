# 02 — Tambah Tabel dan API Risiko Operasional

> Status: **implementasi inti selesai; migrasi production dan policy akses lintas OPD masih perlu divalidasi**  
> Disusun: 2026-09-24  
> Repository backend: `/Users/user/Documents/Projects/manrisk terbaru`  
> Baseline branch: `feature/add-table`  
> Baseline commit: `e143bfa`  
> Branch implementasi wajib: `feature/risiko-operasional`  
> Acuan frontend: menu `/kinerja/operasional/[kodeRekin]` pada repository
> `manrisk-fraud-ui`

## Status implementasi — 24 September 2026

- [x] Branch `feature/risiko-operasional` dibuat dari `feature/add-table`.
- [x] Migration V8, entity, repository, DTO, service CRUD, controller, dan
      test Risiko Operasional ditambahkan.
- [x] Kode risiko memakai format stabil `RSK-OPR-*` dari ID database.
- [x] Route Operasional dilindungi dengan dual-auth: `X-Session-Id` ke Auth
      Service atau fallback token internal untuk BFF lama.
- [x] Swagger mendokumentasikan `X-Session-Id`, bukan Basic Auth.
- [x] Scope Generate AI `operasional` beserta `kode_rekin`, `pegawai_id`, dan
      `rekin` ditambahkan tanpa menyamar sebagai sasaran OPD.
- [x] Focused test CRUD, controller, security, dan AI serta `bootJar` lulus.
- [ ] Full test masih terhalang test `contextLoads()` karena Docker/Testcontainers
      tidak tersedia pada mesin ini; 54 test lain lulus.
- [ ] Flyway V8 belum dijalankan terhadap database production.
- [ ] Rule akses lintas OPD/pegawai belum diaktifkan karena role resmi dan
      kewenangan memilih pegawai belum ditetapkan.

## 1. Tujuan

Menambahkan penyimpanan dan API khusus Risiko Operasional dengan pola yang
setara Risiko Pemda, tetapi menggunakan rencana kinerja individu sebagai
identitas induk.

Target akhirnya:

- [ ] Risiko Operasional disimpan dalam tabel `risiko_operasional`.
- [ ] Risiko Operasional tidak disimpan di tabel OPD `risiko` dan tidak
      disamarkan sebagai `kode_sasaran_opd`.
- [ ] Tersedia API list, list per rencana kinerja/per tab, detail, create,
      update, dan delete.
- [ ] Lima tab risiko memiliki kontrak yang sama dengan Risiko OPD dan Pemda.
- [ ] Generate AI menerima context `scope=operasional` secara eksplisit.
- [ ] Route baru mengikuti mekanisme autentikasi direct-service yang ditetapkan
      pada Plan 20 frontend/backend.
- [ ] Implementasi dan nama existing Risiko OPD tetap `Risiko`, `risiko`, dan
      `/risiko` selama pekerjaan ini.

### 1.1 Branch kerja wajib

Sebelum mengubah migration atau source code, buat branch khusus:

```bash
git switch feature/add-table
git switch -c feature/risiko-operasional
```

Ketentuan branch:

- [ ] Pastikan working tree bersih sebelum branch dibuat.
- [ ] Buat `feature/risiko-operasional` dari commit terbaru
      `feature/add-table` yang sudah memuat implementasi Risiko Pemda.
- [ ] Bila Risiko Pemda sudah lebih dahulu digabung ke `main`, gunakan `main`
      terbaru sebagai titik awal dan tetap beri nama branch
      `feature/risiko-operasional`.
- [ ] Seluruh migration, entity, DTO, repository, service, controller, security,
      AI, test, dan dokumentasi implementasi Operasional dikerjakan di branch
      `feature/risiko-operasional`.
- [ ] Jangan mengimplementasikan Risiko Operasional langsung di
      `feature/add-table` atau `main`.
- [ ] Sebelum commit pertama, verifikasi `git branch --show-current`
      menghasilkan `feature/risiko-operasional`.
- [ ] Push branch tersebut dan ajukan merge terpisah setelah seluruh verifikasi
      selesai.

## 2. Keputusan penamaan dan batas perubahan

Struktur target:

```text
Risiko OPD          → tabel risiko              → endpoint /risiko/**
Risiko Pemda        → tabel risiko_pemda        → endpoint /risiko-pemda/**
Risiko Operasional  → tabel risiko_operasional  → endpoint /risiko-operasional/**
```

Keputusan penting:

- [ ] Jangan rename entity `Risiko` menjadi `RisikoOpd` pada task ini.
- [ ] Jangan rename `RisikoController`, `RisikoService`, `RisikoRepository`,
      DTO `Risiko`, tabel `risiko`, atau endpoint `/risiko` pada task ini.
- [ ] Jangan menambahkan kolom scope ke tabel `risiko` untuk menampung seluruh
      jenis risiko.
- [ ] Rename OPD dikerjakan sekali pada fase akhir melalui task terpisah setelah
      Pemda dan Operasional stabil.
- [ ] Jangan memakai nama `kode_sasaran_opd` untuk kode rencana kinerja.

## 3. Identitas Risiko Operasional

Halaman operasional mengambil rencana kinerja dari respons Penetapan:

```json
{
  "pegawai_id": "<NIP>",
  "kode_opd": "...",
  "tahun_aktif": 2026,
  "rekins": [
    {
      "id": 123,
      "kode_pk": "...",
      "rekin": "...",
      "nama_pemilik_pk": "..."
    }
  ]
}
```

Mapping canonical yang dipakai oleh Manrisk Service:

| Sumber Penetapan/frontend | Field Manrisk | Keterangan |
| --- | --- | --- |
| `rekins[].kode_pk` | `kode_rekin` | Identitas induk Risiko Operasional |
| `pegawai_id` | `pegawai_id` | Berisi NIP sebagai string, bukan ID numerik |
| `kode_opd` | `kode_opd` | Batas organisasi pemilik data |
| `tahun_aktif` | `tahun` | Tahun data risiko |

`kodeRekin` pada path frontend adalah nilai `kode_pk`. Nama `kode_rekin` dipakai
di kontrak Manrisk agar maksud bisnisnya jelas; nilainya tidak boleh diambil dari
`rekins[].id`.

Field identitas berikut immutable setelah record dibuat:

```text
tahun
kode_rekin
kode_opd
pegawai_id
```

Perubahan salah satu field tersebut dilakukan dengan membuat record baru, bukan
melalui endpoint update.

## 4. Migration database

### 4.1 Nomor migration

Migration production saat ini berakhir di V7. Tambahkan file baru:

```text
src/main/resources/db/migration/V8__create_risiko_operasional_table.sql
```

- [ ] Jangan mengedit V1 sampai V7 karena migration tersebut sudah menjadi
      riwayat immutable.
- [ ] Jangan menggeser nomor migration lama lagi.
- [ ] Pastikan `flyway_schema_history` production mencatat V1–V7 sukses sebelum
      V8 dideploy.

### 4.2 Struktur tabel target

```sql
CREATE TABLE IF NOT EXISTS risiko_operasional
(
    id                              BIGSERIAL PRIMARY KEY,
    kode_risiko                     VARCHAR(255),
    tahun                           INTEGER NOT NULL,
    kode_rekin                      VARCHAR(128) NOT NULL,
    kode_opd                        VARCHAR(128) NOT NULL,
    pegawai_id                      VARCHAR(128) NOT NULL,
    permasalahan                    TEXT,
    sebab_permasalahan              TEXT,
    pernyataan_risiko               TEXT NOT NULL,
    skala_kemungkinan               INTEGER NOT NULL,
    skala_dampak                    INTEGER NOT NULL,
    pihak_terkena_risiko            TEXT,
    rencana_tindak_pengendalian     TEXT NOT NULL,
    metode_pemantauan               TEXT,
    penanggungjawab_pemantauan      TEXT,
    keterangan                      TEXT,
    realisasi_tindak_pengendalian   TEXT,
    dapat_terkendali                VARCHAR(255),
    dampak                          TEXT,
    catatan                         TEXT,
    perangkat_yang_menangani        TEXT,
    kode_perangkat_yang_menangani   VARCHAR(128) NOT NULL,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_risiko_operasional_kode_risiko UNIQUE (kode_risiko),
    CONSTRAINT ck_risiko_operasional_skala_kemungkinan
        CHECK (skala_kemungkinan BETWEEN 1 AND 5),
    CONSTRAINT ck_risiko_operasional_skala_dampak
        CHECK (skala_dampak BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_risiko_operasional_rekin_id
    ON risiko_operasional (kode_rekin, id);

CREATE INDEX IF NOT EXISTS idx_risiko_operasional_owner
    ON risiko_operasional (kode_opd, pegawai_id, tahun);
```

Catatan:

- `kode_rekin` menyimpan referensi, bukan foreign key database, karena data
  rencana kinerja berada di layanan lain.
- Nama rencana kinerja dan nama pegawai tidak disalin ke tabel. Tampilan tetap
  mengambil data mutakhir dari Penetapan/Kepegawaian.
- Data mock frontend tidak di-backfill ke production.
- Bila panjang nyata `kode_pk` melebihi 128 karakter, ukuran kolom dan validasi
  diubah bersama sebelum migration dijalankan.

## 5. Entity dan repository

Tambahkan:

```text
entity/RisikoOperasional.java
repository/RisikoOperasionalRepository.java
```

### 5.1 Entity

- [ ] Map entity ke tabel `risiko_operasional`.
- [ ] Extend `BaseAuditable` seperti `RisikoPemda`.
- [ ] Gunakan `GenerationType.IDENTITY` untuk `id`.
- [ ] Tandai `tahun`, `kodeRekin`, `kodeOpd`, `pegawaiId`,
      `pernyataanRisiko`, `skalaKemungkinan`, `skalaDampak`,
      `rencanaTindakPengendalian`, dan `kodePerangkatYangMenangani` sebagai
      non-null sesuai migration.
- [ ] Gunakan `TEXT` untuk field naratif panjang.

### 5.2 Repository

Method minimum:

```java
Optional<RisikoOperasional> findByKodeRisiko(String kodeRisiko);

List<RisikoOperasional> findByKodeRekinOrderByIdAsc(String kodeRekin);
```

Jika pengecekan owner diterapkan langsung pada query, tambahkan method dengan
`kodeRekin`, `kodeOpd`, `pegawaiId`, dan `tahun`; jangan mengambil seluruh data
lalu memfilter di Java.

## 6. DTO API

Tambahkan package mandiri:

```text
dto/RisikoOperasional/
├── RisikoOperasionalReqDTO.java
└── RisikoOperasionalResDTO.java
```

### 6.1 Request DTO

Field reference:

```json
{
  "tahun": 2026,
  "kode_rekin": "<nilai kode_pk>",
  "kode_opd": "...",
  "pegawai_id": "<NIP>"
}
```

Field risiko lainnya mengikuti `RisikoPemdaReqDTO`:

```text
permasalahan
sebab_permasalahan
pernyataan_risiko
skala_kemungkinan
skala_dampak
pihak_terkena_risiko
rencana_tindak_pengendalian
metode_pemantauan
penanggungjawab_pemantauan
keterangan
realisasi_tindak_pengendalian
dapat_terkendali
dampak
catatan
perangkat_yang_menangani
kode_perangkat_yang_menangani
```

- [ ] Terapkan Jakarta validation setara DTO Pemda.
- [ ] `pegawai_id` harus string non-kosong agar NIP dengan format non-numerik
      atau leading zero tidak rusak.
- [ ] Tolak unknown JSON property.
- [ ] Tolak field Pemda seperti `kode_sasaran_pemda`.
- [ ] Tolak penyamaran `kode_rekin` sebagai `kode_sasaran_opd`.

### 6.2 Response DTO

Wrapper list per rencana kinerja minimal:

```json
{
  "scope": "operasional",
  "tahun": 2026,
  "kode_rekin": "...",
  "kode_opd": "...",
  "pegawai_id": "...",
  "risiko": []
}
```

Setiap `risiko[]` minimal selalu membawa:

```json
{
  "id": 1,
  "kode_risiko": "RSK-OPR-0001",
  "type": "identifikasi"
}
```

Nested type dibuat di DTO Operasional sendiri. Jangan mengimpor nested type dari
DTO OPD atau Pemda.

## 7. Service Risiko Operasional

Tambahkan:

```text
service/risiko/RisikoOperasionalService.java
```

Perilaku minimum:

- [ ] `getAllRisiko()` untuk parity/debug.
- [ ] `getRisikoByKodeRekin(kodeRekin, type)` mengembalikan wrapper dengan list
      kosong bila belum ada risiko.
- [ ] `getRisikoByKodeRisiko(kodeRisiko)` mengembalikan satu detail atau 404.
- [ ] `createRisiko(request)` membuat record dan kode stabil.
- [ ] `updateRisiko(id, request)` hanya mengubah field mutable.
- [ ] `deleteRisiko(id)` menghapus berdasarkan ID.
- [ ] Normalisasi string dengan `trim()` tanpa mengubah kapitalisasi kode.
- [ ] Gunakan `RiskException` dan error code stabil, bukan exception mentah.

Nilai `type` yang diterima:

```text
identifikasi
analisis
pengendalian
pemantauan
hasil-pemantauan
```

### 7.1 Format kode risiko

Gunakan namespace tersendiri agar tidak bentrok secara konseptual dengan tabel
lain:

```text
RSK-OPR-0001
RSK-OPR-0002
```

Alur create:

1. simpan dan flush record tanpa `kode_risiko`;
2. ambil ID database;
3. format `RSK-OPR-%04d`;
4. simpan kembali;
5. kembalikan response lengkap.

Jangan menggunakan random code atau melakukan query-loop untuk mencari kode
yang belum dipakai.

### 7.2 Field immutable

Pada update, bandingkan nilai existing dengan request setelah normalisasi.
Perubahan `tahun`, `kode_rekin`, `kode_opd`, atau `pegawai_id` menghasilkan:

```text
HTTP 409
code: RISK_REFERENCE_IMMUTABLE
```

## 8. Controller dan endpoint

Tambahkan:

```text
controller/RisikoOperasionalController.java
```

Kontrak endpoint:

| Method | Endpoint | Tujuan |
| --- | --- | --- |
| GET | `/risiko-operasional` | List seluruh Risiko Operasional |
| GET | `/risiko-operasional/rekin/{kodeRekin}?type={type}` | List tab per rencana kinerja |
| GET | `/risiko-operasional/{kodeRisiko}` | Detail berdasarkan kode risiko |
| POST | `/risiko-operasional` | Membuat Risiko Operasional |
| PUT | `/risiko-operasional/{id}` | Memperbarui field mutable |
| DELETE | `/risiko-operasional/{id}` | Menghapus berdasarkan ID |

- [ ] Validasi path `kodeRekin` dan `kodeRisiko`: tidak kosong, maksimal 128.
- [ ] Validasi `id >= 1`.
- [ ] Gunakan `@Valid` pada create/update.
- [ ] Pertahankan envelope `ApiResponse` yang sama dengan endpoint existing.
- [ ] Dokumentasikan tag Swagger sebagai `Manajemen Risiko Operasional`.

## 9. Autentikasi dan otorisasi

Plan 20 memindahkan frontend ke pemanggilan Manrisk Service secara langsung.
Karena itu route Operasional harus masuk ke mekanisme session validation yang
sama dengan route OPD dan Pemda.

- [ ] Lindungi `/risiko-operasional` dan seluruh subpath-nya.
- [ ] Browser mengirim `X-Session-Id`; jangan pernah mengirim
      `RISIKO_INTERNAL_TOKEN` ke browser.
- [ ] Backend memvalidasi session ke endpoint Auth Service `/user-info`.
- [ ] Session kosong/tidak valid menghasilkan 401.
- [ ] Auth Service timeout/tidak tersedia menghasilkan 503.
- [ ] CORS mengizinkan origin frontend production yang tepat.
- [ ] Mode `security=none` tidak boleh berarti route risiko bebas tanpa
      validasi caller/session.

Aturan kepemilikan minimum:

- [ ] `kode_opd` request harus cocok dengan `kode_opd` principal untuk user
      non-admin/non-pusat.
- [ ] Akses lintas OPD hanya boleh diberikan oleh role yang disepakati.
- [ ] Kebijakan akses berdasarkan `pegawai_id` harus dikonfirmasi sebelum
      production: user mengelola dirinya sendiri atau pejabat OPD dapat memilih
      pegawai lain.
- [ ] Jangan menganggap NIP dari request sudah tepercaya hanya karena session
      user valid.

Jika implementasi Plan 20 belum masuk saat task ini mulai, buat fitur
Operasional di belakang abstraction/filter auth yang sama; jangan membuka route
sementara dan jangan menanam shared secret di frontend.

## 10. Integrasi Generate AI

Perluas context AI menjadi tiga scope:

```text
opd | pemda | operasional
```

Tambahkan field context Operasional:

```json
{
  "scope": "operasional",
  "kode_opd": "...",
  "pegawai_id": "...",
  "tahun": 2026,
  "kode_rekin": "...",
  "rekin": "...",
  "kode_indikator": "...",
  "indikator": "...",
  "target": 100,
  "satuan": "%",
  "pagu": 1000000,
  "pemilik_risiko": "..."
}
```

- [ ] Update regex/validation `GenerateAiReqDTO.Context` untuk
      `scope=operasional`.
- [ ] Tambahkan field `kode_rekin`, `rekin`, dan `pegawai_id` tanpa memakai
      field sasaran OPD sebagai alias.
- [ ] `RisikoAiContextService` menolak context campuran antar-scope.
- [ ] Canonical hash memasukkan scope dan identitas Operasional.
- [ ] `RisikoAiPromptFactory` memakai subjek `kinerja operasional/individu`,
      bukan `OPD` atau `Pemerintah Daerah`.
- [ ] Snapshot context dari frontend tetap dianggap data tidak tepercaya dan
      hanya digunakan untuk prompt, bukan untuk keputusan otorisasi.

## 11. Error contract

Gunakan code existing bila maknanya sama:

| HTTP | Code | Kondisi |
| --- | --- | --- |
| 400 | `RISK_INVALID_INPUT` | Body/path/ID tidak valid |
| 400 | `RISK_TYPE_INVALID` | Nilai tab tidak dikenal |
| 401 | `RISK_CALLER_UNAUTHORIZED` atau code session pengganti Plan 20 | Session/caller tidak valid |
| 403 | `RISK_ACCESS_FORBIDDEN` | Principal tidak berhak pada owner data |
| 404 | `RISK_NOT_FOUND` | Risiko Operasional tidak ditemukan |
| 409 | `RISK_REFERENCE_IMMUTABLE` | Reference owner/rekin diubah |
| 503 | code auth-service Plan 20 | Validasi session tidak dapat dilakukan |

Jangan mengembalikan stack trace, SQL, credential, shared token, atau alamat
internal service pada response.

## 12. Pengujian

Tambahkan minimal:

```text
src/test/java/cc/kertaskerja/manrisk/
├── controller/RisikoOperasionalControllerTest.java
└── service/risiko/RisikoOperasionalServiceTest.java
```

Perluas test security dan AI existing.

### 12.1 Service test

- [ ] Create menghasilkan `RSK-OPR-*` dari ID database.
- [ ] List per `kode_rekin` terurut berdasarkan ID.
- [ ] Empty list tetap 200 dengan `risiko: []` dan `scope=operasional`.
- [ ] Kelima tab menghasilkan shape yang benar.
- [ ] Detail kode tidak ditemukan menghasilkan 404.
- [ ] Update reference immutable menghasilkan 409.
- [ ] Update field mutable berhasil.
- [ ] Delete ID tidak ditemukan menghasilkan 404.

### 12.2 Controller/validation test

- [ ] Create valid menghasilkan 201.
- [ ] `kode_rekin`, `kode_opd`, `pegawai_id`, atau field wajib kosong
      menghasilkan 400.
- [ ] Skala di luar 1–5 menghasilkan 400.
- [ ] Unknown property dan field scope lain menghasilkan 400.
- [ ] Unknown tab type menghasilkan `RISK_TYPE_INVALID`.

### 12.3 Security test

- [ ] Route Operasional tanpa session/caller ditolak.
- [ ] Session valid diteruskan.
- [ ] OPD principal yang tidak cocok ditolak 403.
- [ ] CORS preflight dari frontend production berhasil.
- [ ] Shared internal token tidak diperlukan oleh browser.

### 12.4 AI test

- [ ] Context Operasional valid dinormalisasi.
- [ ] Context campuran Operasional+OPD/Pemda ditolak.
- [ ] Hash berbeda bila `kode_rekin`, `pegawai_id`, atau scope berubah.
- [ ] Prompt menyebut konteks kinerja operasional/individu.

### 12.5 Migration dan regression

- [ ] Jalankan migration V1–V8 pada database kosong.
- [ ] Jalankan migration V8 pada snapshot schema production V1–V7.
- [ ] Pastikan V8 hanya menambah tabel/index baru.
- [ ] Jalankan seluruh test OPD dan Pemda untuk membuktikan tidak ada regresi.
- [ ] Jalankan `compileJava`, test terfokus, full `test`, dan `bootJar`.

## 13. Urutan implementasi

1. Pastikan working tree bersih dan baseline sudah memuat Risiko Pemda.
2. Buat lalu checkout branch `feature/risiko-operasional`.
3. Verifikasi branch aktif sebelum menyentuh source code.
4. Pastikan baseline migration production benar dan V1–V7 sukses.
5. Tambahkan V8 tanpa mengubah migration lama.
6. Tambahkan entity dan repository Operasional.
7. Tambahkan request/response DTO mandiri.
8. Tambahkan service beserta unit test.
9. Tambahkan controller beserta validation test.
10. Masukkan route Operasional ke auth/session dan CORS.
11. Tambahkan scope Operasional pada Generate AI.
12. Jalankan migration test dan regression test OPD/Pemda.
13. Dokumentasikan kontrak final untuk implementasi frontend.

## 14. Rollout production

- [ ] Backup PostgreSQL sebelum deploy migration V8.
- [ ] Deploy backend yang berisi V8 dan kode Operasional dalam satu artifact.
- [ ] Pantau startup sampai Flyway mencatat V8 sukses.
- [ ] Cek tabel dan index `risiko_operasional` terbentuk.
- [ ] Smoke test session, empty list, create, detail, update, dan delete dengan
      record uji yang disepakati.
- [ ] Baru setelah backend stabil, frontend mengganti mock menjadi API.

Rollback aplikasi dapat memakai image backend sebelumnya. Karena V8 hanya
menambah tabel, tabel dapat dibiarkan saat rollback aplikasi. Jangan drop tabel
production sebagai bagian rollback otomatis; penghapusan data memerlukan
persetujuan terpisah.

## 15. Handoff kontrak frontend

Frontend nantinya perlu:

- service/type khusus `risiko-operasional`;
- memakai `rekin.kode_pk` sebagai `kode_rekin`;
- mengambil list berdasarkan `/risiko-operasional/rekin/{kodeRekin}`;
- mengganti `risiko-operasional.mock.ts`;
- mengaktifkan create/edit/delete pada lima tab;
- memakai `scope=operasional` untuk Generate AI; dan
- mengirim session user sesuai Plan 20, bukan internal token.

Implementasi frontend bukan bagian dari plan backend ini.

## 16. Definition of Done

- [ ] Seluruh implementasi berada di branch `feature/risiko-operasional`, bukan
      langsung di `feature/add-table` atau `main`.
- [ ] V8 berjalan pada database kosong dan database production-like V1–V7.
- [ ] CRUD Risiko Operasional tersedia melalui endpoint khusus.
- [ ] Record terikat ke `kode_rekin` dari `kode_pk`, bukan ID numerik dan bukan
      `kode_sasaran_opd`.
- [ ] Kode risiko stabil memakai prefix `RSK-OPR-`.
- [ ] Lima tab mengembalikan data dengan identitas row yang benar.
- [ ] Route terlindungi session/caller dan aturan OPD.
- [ ] Generate AI memahami scope Operasional tanpa context campuran.
- [ ] Test Operasional, OPD, Pemda, security, AI, migration, dan build hijau.
- [ ] Tidak ada rename atau regresi terhadap implementasi Risiko OPD existing.
- [ ] Kontrak API final siap dipakai frontend untuk mengganti mock.

## 17. Di luar scope

- Rename `Risiko` existing menjadi `RisikoOpd`.
- Rename tabel `risiko` menjadi `risiko_opd`.
- Rename endpoint `/risiko` menjadi `/risiko-opd`.
- Migrasi data mock frontend ke production.
- Perubahan API Penetapan atau Kepegawaian.
- Implementasi UI/frontend Risiko Operasional.
- Pembuatan API gateway/proxy final.
