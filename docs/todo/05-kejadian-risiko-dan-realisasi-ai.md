# 05 — Persistensi Kejadian Risiko dan Generate AI Realisasi

> Status: **implemented locally — menunggu deploy dan verifikasi migration**
> Disusun: 2026-09-25
> Repository backend: `/Users/user/Documents/Projects/manrisk terbaru`
> Pasangan plan frontend: `/Users/user/Documents/Projects/manrisk-fraud-ui/docs/todo/23-pemantauan-kejadian-dan-realisasi-ai.md`
> Dependency: implementasikan `04-pengendalian-yang-sudah-ada.md` dan Flyway V10
> terlebih dahulu
> Baseline backend: `main` commit `c02b25f`

## 1. Tujuan

Menambahkan persistensi status kejadian risiko dan tanggal kejadian untuk
Risiko OPD, Pemda, serta Operasional, kemudian menambahkan tipe Generate AI
untuk draft **Realisasi Tindak Pengendalian** berbasis RTP awal.

Response aktual dari endpoint tab Pemda setelah save mengandung
`"keterangan": "-"`. Ini membuktikan masalah tidak hanya berada di rendering
frontend. Source backend lokal memang memetakan `request -> entity -> response`
secara langsung tanpa fallback `"-"`, sehingga plan ini juga harus mengaudit
payload aktual, row database, versi artifact yang terdeploy, dan response
mapper sebelum menetapkan akar masalah.

Pada response contoh, `created_at` dan `updated_at` sama-sama bertanggal
24 September 2026 serta hanya berselisih milidetik. Jika user melakukan save
pada 25 September 2026, endpoint yang diuji tidak memperbarui row itu atau
request save dan GET membaca record/environment berbeda.

## 2. Kontrak canonical

| Makna | JSON | Java | PostgreSQL |
| --- | --- | --- | --- |
| Apakah risiko terjadi | `risiko_terjadi` | `Boolean risikoTerjadi` | `BOOLEAN` |
| Tanggal kejadian | `waktu_terjadi` | `LocalDate waktuTerjadi` | `DATE` |
| Realisasi pengendalian | `realisasi_tindak_pengendalian` | existing `String realisasiTindakPengendalian` | existing `TEXT` |
| Tipe AI | `realisasi-tindak-pengendalian` | string discriminator | tidak disimpan |

Aturan domain:

```text
risiko_terjadi = true  -> waktu_terjadi wajib terisi
risiko_terjadi = false -> waktu_terjadi wajib null
risiko_terjadi = null  -> waktu_terjadi wajib null (record lama/belum dijawab)
waktu_terjadi          -> tidak boleh setelah tanggal hari ini
```

Gunakan `LocalDate`, bukan `LocalDateTime`, karena frontend meminta date picker
dan tidak ada informasi jam/zona waktu yang perlu disimpan.

## 3. Flyway V11

Dependency plan backend 04 menggunakan V10. Migration plan ini harus memakai:

```text
src/main/resources/db/migration/V11__add_risiko_terjadi_dan_waktu_terjadi.sql
```

Target migration:

```sql
ALTER TABLE risiko
    ADD COLUMN risiko_terjadi BOOLEAN,
    ADD COLUMN waktu_terjadi DATE,
    ADD CONSTRAINT ck_risiko_kejadian_consistent
        CHECK (
            (risiko_terjadi IS TRUE AND waktu_terjadi IS NOT NULL)
            OR (risiko_terjadi IS FALSE AND waktu_terjadi IS NULL)
            OR (risiko_terjadi IS NULL AND waktu_terjadi IS NULL)
        );

ALTER TABLE risiko_pemda
    ADD COLUMN risiko_terjadi BOOLEAN,
    ADD COLUMN waktu_terjadi DATE,
    ADD CONSTRAINT ck_risiko_pemda_kejadian_consistent
        CHECK (
            (risiko_terjadi IS TRUE AND waktu_terjadi IS NOT NULL)
            OR (risiko_terjadi IS FALSE AND waktu_terjadi IS NULL)
            OR (risiko_terjadi IS NULL AND waktu_terjadi IS NULL)
        );

ALTER TABLE risiko_operasional
    ADD COLUMN risiko_terjadi BOOLEAN,
    ADD COLUMN waktu_terjadi DATE,
    ADD CONSTRAINT ck_risiko_operasional_kejadian_consistent
        CHECK (
            (risiko_terjadi IS TRUE AND waktu_terjadi IS NOT NULL)
            OR (risiko_terjadi IS FALSE AND waktu_terjadi IS NULL)
            OR (risiko_terjadi IS NULL AND waktu_terjadi IS NULL)
        );
```

Catatan:

- [ ] Jangan mengubah V1–V10 yang sudah direncanakan/dirilis.
- [ ] Kolom nullable menjaga semua row lama valid sebagai `NULL/NULL`.
- [ ] Jangan memakai `CURRENT_DATE` dalam CHECK constraint untuk batas tanggal
      masa depan; validasi tersebut dilakukan pada service agar perilakunya
      dapat diuji dan tidak bergantung pada evaluasi constraint yang berubah
      seiring waktu.
- [ ] Verifikasi migration pada database kosong dan upgrade dari V9 berisi
      data.
- [ ] Rollback aplikasi membiarkan kolom tetap ada; migration tetap
      forward-only.

## 4. Entity, DTO, dan JSON

### 4.1 Entity

Tambahkan ke `Risiko`, `RisikoPemda`, dan `RisikoOperasional`:

```java
@Column(name = "risiko_terjadi")
private Boolean risikoTerjadi;

@Column(name = "waktu_terjadi")
private LocalDate waktuTerjadi;
```

### 4.2 Request DTO

Tambahkan ke ketiga request DTO:

```java
@JsonProperty("risiko_terjadi")
private Boolean risikoTerjadi;

@JsonProperty("waktu_terjadi")
@PastOrPresent
private LocalDate waktuTerjadi;
```

`@PastOrPresent` menangani tanggal masa depan. Konsistensi antar-field tetap
divalidasi pada service/helper domain karena Jakarta field validation tidak
cukup untuk aturan kondisional tersebut.

### 4.3 Response DTO

Tambahkan pada response top-level dan nested `RisikoItem` untuk tiga scope:

```java
@JsonProperty("risiko_terjadi")
private Boolean risikoTerjadi;

@JsonProperty("waktu_terjadi")
private LocalDate waktuTerjadi;
```

Jackson harus menghasilkan tanggal ISO `YYYY-MM-DD`. Jangan mengubahnya menjadi
timestamp atau epoch.

## 5. Service dan validasi domain

Perbarui `RisikoService`, `RisikoPemdaService`, dan
`RisikoOperasionalService`:

- [ ] Map kedua field pada create, update, `toResDTO()`, dan `toRisikoItem()`.
- [ ] Jalankan satu helper validasi yang konsisten sebelum save.
- [ ] Tolak `true/null`, `false/date`, dan `null/date` dengan HTTP 400 serta
      error code stabil, misalnya `RISK_OCCURRENCE_INVALID`.
- [ ] Tolak tanggal masa depan sebagai input invalid.
- [ ] Jangan mengubah atau menebak tanggal secara otomatis di backend.
- [ ] Pastikan update request lama yang tidak membawa kedua field tetap
      menghasilkan `null/null` sesuai kontrak full-replacement DTO existing.
- [ ] Return kedua field minimal pada tab `hasil-pemantauan`; rekomendasi:
      sertakan pada seluruh branch non-identifikasi agar nested DTO konsisten.

## 6. Audit dan perbaikan round-trip Keterangan

Tidak ada migration atau field baru yang diperlukan untuk `keterangan`; kolom
sudah tersedia. Akan tetapi nilai aktual berubah/tersimpan sebagai `"-"`, jadi
jalur runtime harus dibuktikan end-to-end.

Source backend lokal saat ini sudah:

- memiliki kolom `keterangan` pada tiga tabel;
- menerima field pada ketiga request DTO;
- memetakan field pada create/update;
- mengembalikan field pada response top-level dan nested item non-identifikasi.

Tidak ada kode pada source lokal yang secara eksplisit mengubah null/kosong
menjadi `"-"`. Karena itu kemungkinan yang harus diuji adalah:

1. frontend mengirim `"-"` pada POST/PUT;
2. database sudah menyimpan `"-"` dari request/create sebelumnya;
3. artifact backend yang berjalan tidak sama dengan source lokal;
4. ada trigger, import/seed, atau proses lain yang menimpa row; atau
5. endpoint yang diperiksa membaca database/environment yang berbeda.

### 6.1 Prosedur diagnosis

Gunakan marker unik, misalnya `KETERANGAN-E2E-20260925`:

- [ ] Tangkap JSON POST/PUT aktual dan pastikan `keterangan` berisi marker.
- [ ] Periksa response langsung create/update sebelum redirect frontend.
- [ ] Jalankan GET detail `risiko-pemda/{kodeRisiko}` dan catat nilainya.
- [ ] Periksa row database pada environment yang sama:

```sql
SELECT id, kode_risiko, keterangan, updated_at
FROM risiko_pemda
WHERE kode_risiko = 'RSK-PEM-0002';
```

- [ ] Panggil endpoint sasaran dengan `type=pemantauan` dan bandingkan dengan
      GET detail serta row DB.
- [ ] Pastikan `updated_at` berubah setelah PUT pada ID yang sama. Jika tidak,
      cocokkan ID URL update, `kode_risiko`, base URL service, profile aktif,
      datasource, dan environment database.
- [ ] Ulangi satu kasus OPD dan Operasional untuk memastikan masalah tidak
      spesifik Pemda.
- [ ] Cocokkan image/tag/commit backend terdeploy dengan commit source yang
      sedang diaudit.
- [ ] Audit trigger database dan job/import bila payload benar tetapi row DB
      berubah sesudah save.

### 6.2 Keputusan perbaikan berdasarkan titik gagal

| Temuan | Perbaikan |
| --- | --- |
| Payload sudah `-` | Perbaiki frontend; backend tidak boleh menebak teks asli |
| Payload marker, response save/DB `-` | Perbaiki mapping persistence atau proses yang menimpa row |
| DB marker, GET detail/tab `-` | Perbaiki response mapper, cache, atau artifact deployment |
| API marker, UI kosong | Perbaiki `mapPemantauanTab()` frontend |

- [ ] Jangan menambahkan fallback `"-"` pada service atau DTO backend.
- [ ] Nilai kosong seharusnya tetap `null`/kosong dan formatting placeholder
      menjadi tanggung jawab UI.
- [ ] Jangan membuat migration duplikat atau mengganti nama kolom.
- [ ] Tambahkan service/controller test bahwa marker dari request create dan
      update tersimpan serta dikembalikan persis pada response save, detail,
      dan tab `pemantauan` untuk tiga scope.

## 7. Generate AI Realisasi Tindak Pengendalian

### 7.1 Kontrak

Tambahkan tipe:

```text
realisasi-tindak-pengendalian
```

Input yang diperbolehkan dan wajib:

```json
{
  "rencana_tindak_pengendalian": "Melakukan monitoring berkala ..."
}
```

Output tepat tiga proposal unik:

```json
{
  "proposals": [
    {
      "id": "<uuid>",
      "realisasi_tindak_pengendalian": "Monitoring berkala telah dilaksanakan dan didokumentasikan."
    }
  ]
}
```

### 7.2 Guardrail faktual

Realisasi adalah data faktual. Model tidak mengetahui tindakan yang benar-benar
terjadi hanya dari sebuah rencana. Karena itu prompt harus meminta **draft pola
pelaporan untuk diverifikasi**, bukan membuat klaim pelaksanaan.

Prompt harus:

- menggunakan RTP awal sebagai satu-satunya input bisnis;
- tidak mengarang tanggal, jumlah kegiatan, lokasi, dokumen, nomor surat/SOP,
  unit, pejabat, hasil pengukuran, bukti, atau tingkat keberhasilan;
- menggunakan bahasa yang mudah disunting dan tidak mengisi placeholder palsu;
- mengingatkan bahwa hasil memerlukan verifikasi manusia melalui copy UI.

### 7.3 Prompt factory

Perbarui `RisikoAiPromptFactory`:

- [ ] Tambahkan type ke `TYPES`.
- [ ] Izinkan dan wajibkan hanya `rencana_tindak_pengendalian`.
- [ ] Tambahkan task tiga draft realisasi yang relevan dengan RTP awal.
- [ ] Tambahkan response schema string
      `realisasi_tindak_pengendalian` sepanjang 1–1500 karakter.
- [ ] Tambahkan count 3 pada `proposalCount()`.

### 7.4 Output validator

Perbarui `RisikoAiOutputValidator`:

- [ ] Tambahkan branch normalize untuk type baru.
- [ ] Wajibkan tepat tiga proposal.
- [ ] Trim dan validasi string 1–1500 karakter.
- [ ] Tolak proposal duplikat secara case-insensitive.
- [ ] Tambahkan UUID `id` memakai pipeline proposal existing.

Tidak diperlukan perubahan kolom database untuk AI realisasi karena
`realisasi_tindak_pengendalian` sudah persisted sebagai `TEXT`.

## 8. Testing backend

### 8.1 Migration dan persistence

- [ ] V10 berhasil pada database kosong dan database upgrade dari V9.
- [ ] Row lama tetap `risiko_terjadi = NULL` dan `waktu_terjadi = NULL`.
- [ ] Ketiga scope menyimpan/mengembalikan `true/date` dan `false/null`.
- [ ] Database menolak kombinasi yang melanggar CHECK constraint.

### 8.2 Validation/controller

- [ ] JSON boolean diterima; string `"YA"`/`"TIDAK"` ditolak.
- [ ] Tanggal ISO valid diterima dan tanggal invalid ditolak.
- [ ] Tanggal masa depan ditolak.
- [ ] `true/null`, `false/date`, dan `null/date` menghasilkan
      `RISK_OCCURRENCE_INVALID`.
- [ ] Response detail dan nested item menggunakan snake_case dan tanggal
      `YYYY-MM-DD`.

### 8.3 Service/tab regression

- [ ] Kelima type tab tetap dapat dimuat untuk tiga scope.
- [ ] Hasil Pemantauan membawa status/tanggal kejadian dan realisasi.
- [ ] Pemantauan membawa marker `keterangan` yang sama persis dengan payload;
      backend tidak menyintesis `"-"`.

### 8.4 AI

- [ ] Prompt factory menerima type baru dan menolak key tambahan.
- [ ] RTP kosong menghasilkan `AI_INPUT_REQUIRED`.
- [ ] Validator menerima tiga proposal unik yang valid.
- [ ] Validator menolak jumlah salah, kosong, terlalu panjang, dan duplikat.
- [ ] Test prompt mengunci larangan mengarang bukti pelaksanaan.

Jalankan minimal:

```bash
./gradlew test
```

## 9. Urutan deployment

```text
1. Implementasikan/deploy backend plan 04 + Flyway V10
2. Implementasikan/deploy backend plan 05 + Flyway V11
3. Verifikasi CRUD, response tab, dan AI type baru
4. Deploy frontend plan 22
5. Deploy frontend plan 23
6. Smoke test tiga scope dan print preview
```

Frontend 23 tidak boleh dikirim lebih dahulu karena request DTO backend lama
menolak unknown property dan database belum memiliki kolom baru.

## 10. Acceptance criteria

- [ ] V11 menambah `BOOLEAN risiko_terjadi` dan `DATE waktu_terjadi` pada tiga
      tabel tanpa mengubah migration lama.
- [ ] Kombinasi status dan tanggal konsisten pada validation layer dan database.
- [ ] Ketiga scope menerima, menyimpan, serta mengembalikan kedua field.
- [ ] `keterangan` lolos round-trip request → database → response save → detail
      → tab tanpa berubah menjadi `"-"`.
- [ ] Generate AI realisasi menerima RTP awal dan mengembalikan tiga draft unik.
- [ ] Output AI tidak diperlakukan sebagai bukti bahwa tindakan telah benar-
      benar dilaksanakan.
- [ ] Seluruh test lulus sebelum frontend 23 diaktifkan.
