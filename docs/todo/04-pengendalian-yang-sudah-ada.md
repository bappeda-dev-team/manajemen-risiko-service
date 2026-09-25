# 04 — Persistensi dan AI Pengendalian yang Sudah Ada

> Status: **implemented locally — menunggu deploy dan verifikasi migration**
> Disusun: 2026-09-25
> Repository backend: `/Users/user/Documents/Projects/manrisk terbaru`
> Pasangan plan frontend: `/Users/user/Documents/Projects/manrisk-fraud-ui/docs/todo/22-pengendalian-yang-sudah-ada.md`
> Baseline backend: `main` commit `c02b25f`

## 1. Tujuan

Menambahkan field persisted **Pengendalian yang Sudah Ada** untuk Risiko OPD,
Risiko Pemda, dan Risiko Operasional, mengekspos field pada CRUD serta seluruh
response tab, dan menyediakan Generate AI berbasis `pernyataan_risiko`.

Kontrak canonical:

| Lapisan | Nama |
| --- | --- |
| JSON API | `pengendalian_yang_sudah_ada` |
| Java | `pengendalianYangSudahAda` |
| PostgreSQL | `pengendalian_yang_sudah_ada` |
| Generate AI type | `pengendalian-yang-sudah-ada` |

## 2. Keputusan kompatibilitas

- Kolom baru bertipe `TEXT` dan nullable pada rollout pertama.
- Tidak ada default teks buatan untuk record lama.
- Request DTO menerima field kosong/null dengan batas maksimum 2000 karakter;
  field belum `@NotBlank` agar frontend lama dan record lama tetap kompatibel.
- Response detail dan response item tab mengembalikan field bila tersedia.
- Jangan mengubah migration `V1`–`V9` yang sudah pernah dirilis.
- AI menghasilkan usulan yang harus diverifikasi user, bukan klaim faktual
  bahwa kontrol tersebut benar-benar sudah ada.

## 3. Flyway V10

Buat migration baru:

```text
src/main/resources/db/migration/V10__add_pengendalian_yang_sudah_ada.sql
```

Isi target:

```sql
ALTER TABLE risiko
    ADD COLUMN pengendalian_yang_sudah_ada TEXT;

ALTER TABLE risiko_pemda
    ADD COLUMN pengendalian_yang_sudah_ada TEXT;

ALTER TABLE risiko_operasional
    ADD COLUMN pengendalian_yang_sudah_ada TEXT;
```

- [ ] Pastikan urutan V10 berada setelah migration V9 multi-OPD, pembuatan `risiko_pemda` (V7), dan
      `risiko_operasional` (V8).
- [ ] Jangan memakai `NOT NULL` atau placeholder/backfill yang mengarang nilai
      existing.
- [ ] Jangan menambahkan index untuk field naratif yang tidak dipakai sebagai
      predicate repository.
- [ ] Verifikasi startup pada database kosong menjalankan V1–V10.
- [ ] Verifikasi upgrade database berisi data menjalankan V10 dan menjaga
      seluruh row existing.
- [ ] Rollback aplikasi tidak menghapus kolom; migration Flyway tetap
      forward-only dan kolom nullable aman diabaikan versi lama.

## 4. Entity dan DTO

### 4.1 Entity

Tambahkan field pada:

```text
entity/Risiko.java
entity/RisikoPemda.java
entity/RisikoOperasional.java
```

Mapping target:

```java
@Column(name = "pengendalian_yang_sudah_ada", columnDefinition = "TEXT")
private String pengendalianYangSudahAda;
```

`columnDefinition = "TEXT"` dipakai konsisten pada ketiga entity untuk field
naratif baru, meskipun beberapa field lama di `Risiko` belum menuliskannya.

### 4.2 Request DTO

Tambahkan ke:

```text
dto/Risiko/RisikoReqDTO.java
dto/RisikoPemda/RisikoPemdaReqDTO.java
dto/RisikoOperasional/RisikoOperasionalReqDTO.java
```

Kontrak target:

```java
@JsonProperty("pengendalian_yang_sudah_ada")
@Size(max = 2000)
private String pengendalianYangSudahAda;
```

### 4.3 Response DTO

Tambahkan field pada response top-level dan nested `RisikoItem` di:

```text
dto/Risiko/RisikoResDTO.java
dto/RisikoPemda/RisikoPemdaResDTO.java
dto/RisikoOperasional/RisikoOperasionalResDTO.java
```

Field nested wajib tersedia untuk kelima type tab; frontend tidak boleh perlu
memanggil endpoint detail per row.

## 5. Service CRUD dan mapping tab

Perbarui:

```text
service/risiko/RisikoService.java
service/risiko/RisikoPemdaService.java
service/risiko/RisikoOperasionalService.java
```

- [ ] Map request ke entity pada create.
- [ ] Terapkan normalisasi/`trim` yang sama pada update.
- [ ] Sertakan field pada `toResDTO()` untuk endpoint detail/list.
- [ ] Sertakan field pada `toRisikoItem()` sebelum conditional tab atau pada
      seluruh branch, sehingga field muncul pada `identifikasi`, `analisis`,
      `pengendalian`, `pemantauan`, dan `hasil-pemantauan`.
- [ ] Pastikan update field tidak mengubah kode risiko, scope key, atau field
      lain.
- [ ] Jangan menurunkan nilai existing menjadi `NULL` akibat mapper update yang
      lupa menerima field baru.

Contoh fragmen response tab:

```json
{
  "id": 12,
  "kode_risiko": "RSK-PEM-0012",
  "type": "pengendalian",
  "pernyataan_risiko": "Pelayanan publik terganggu karena ...",
  "pengendalian_yang_sudah_ada": "Tersedia SOP pelayanan dan reviu berkala.",
  "rencana_tindak_pengendalian": "Memperkuat monitoring kepatuhan SOP ..."
}
```

## 6. Generate AI

### 6.1 Kontrak

Tipe baru:

```text
pengendalian-yang-sudah-ada
```

Input yang diperbolehkan dan diwajibkan:

```json
{
  "pernyataan_risiko": "Pelayanan publik terganggu karena ..."
}
```

Output berisi tepat tiga proposal unik:

```json
{
  "proposals": [
    {
      "id": "<uuid>",
      "pengendalian_yang_sudah_ada": "Tersedia SOP pelayanan dan reviu berkala."
    }
  ]
}
```

### 6.2 Prompt factory

Perbarui `RisikoAiPromptFactory`:

- [ ] Tambahkan type baru ke `TYPES`.
- [ ] Izinkan hanya `pernyataan_risiko` pada `validateInput()` dan jadikan
      field tersebut required.
- [ ] Tambahkan task yang meminta tiga **usulan pengendalian existing yang
      mungkin relevan untuk diverifikasi**, bukan realisasi faktual.
- [ ] Tegaskan larangan mengarang nama dokumen, nomor SOP, regulasi, tanggal,
      unit, pejabat, hasil audit, atau klaim implementasi yang tidak ada pada
      context.
- [ ] Tambahkan response schema proposal dengan satu field string
      `pengendalian_yang_sudah_ada`, panjang 1–1500 karakter.
- [ ] Set `proposalCount()` menjadi 3 untuk type baru.

Contoh maksud prompt, bukan teks final wajib:

```text
Buat tepat tiga draft pengendalian yang lazim dan relevan terhadap pernyataan
risiko. Tulis sebagai usulan untuk diverifikasi pengguna. Jangan menyatakan
bahwa kontrol benar-benar tersedia atau telah dilaksanakan jika context tidak
menyediakannya.
```

### 6.3 Output validator

Perbarui `RisikoAiOutputValidator`:

- [ ] Tambahkan branch type baru pada `normalize()`.
- [ ] Wajibkan tepat tiga proposal.
- [ ] Normalisasi field `pengendalian_yang_sudah_ada` dan batasi 1500 karakter.
- [ ] Tolak string kosong dan proposal duplikat secara case-insensitive.
- [ ] Tambahkan UUID `id` menggunakan mekanisme proposal existing.

Komponen `RekomendasiRisikoService`, rate limit, request ID, context hash,
OpenRouter client, dan envelope response tetap dipakai tanpa jalur khusus.

## 7. Testing backend

### 7.1 Flyway/integration

- [ ] Database kosong berhasil migrate sampai V9.
- [ ] Database dari V8 dengan row pada ketiga tabel berhasil upgrade.
- [ ] Nilai kolom baru untuk row lama tetap `NULL`.
- [ ] Create dan update dapat menyimpan teks panjang yang valid.

### 7.2 Service

- [ ] Create/read/update OPD mempertahankan field.
- [ ] Create/read/update Pemda mempertahankan field.
- [ ] Create/read/update Operasional mempertahankan field.
- [ ] `toRisikoItem()` mengembalikan field untuk kelima type pada ketiga scope.
- [ ] Nilai null existing tidak menyebabkan exception.

### 7.3 Controller/validation

- [ ] JSON request menerima `pengendalian_yang_sudah_ada`.
- [ ] Respons detail dan tab memakai nama snake_case yang tepat.
- [ ] Nilai lebih dari 2000 karakter menghasilkan validation error.
- [ ] Request lama tanpa field baru tetap berhasil selama field ini masih
      opsional.
- [ ] Unknown property lain tetap ditolak.

### 7.4 AI

- [ ] Prompt factory menerima type baru dan menolak key input lain.
- [ ] `pernyataan_risiko` kosong menghasilkan `AI_INPUT_REQUIRED`.
- [ ] Validator menerima tepat tiga proposal valid.
- [ ] Validator menolak jumlah proposal salah, nilai kosong, terlalu panjang,
      dan duplikat.
- [ ] Test memastikan system/task prompt melarang klaim faktual yang tidak
      didukung context.

Jalankan minimal:

```bash
./gradlew test
```

## 8. Deployment dan observability

- [ ] Deploy migration dan backend sebelum frontend baru.
- [ ] Cek `flyway_schema_history` memuat V9 sukses.
- [ ] Smoke test CRUD dan lima endpoint tab pada OPD, Pemda, Operasional.
- [ ] Smoke test type AI baru dengan session valid.
- [ ] Pantau `AI_INVALID_OUTPUT`, `AI_INPUT_REQUIRED`, rate limit, dan latency
      tanpa mencatat isi narasi sensitif secara berlebihan.
- [ ] Setelah backend terverifikasi, beri tanda siap pada dependency plan
      frontend.

## 9. Acceptance criteria

- [ ] V10 menambah kolom nullable pada `risiko`, `risiko_pemda`, dan
      `risiko_operasional` tanpa mengubah migration lama.
- [ ] Entity, request DTO, response DTO, nested item, dan seluruh mapper service
      memakai kontrak `pengendalian_yang_sudah_ada`.
- [ ] Field tersimpan dan tersedia pada seluruh lima type tab untuk tiga scope.
- [ ] Generate AI menerima Pernyataan Risiko dan mengembalikan tiga draft unik.
- [ ] Prompt dan UI contract tidak menyajikan output AI sebagai fakta bahwa
      pengendalian sudah dilaksanakan.
- [ ] Backend lama-compatible terhadap record dan request tanpa field baru.
- [ ] Seluruh test backend lulus sebelum frontend mengaktifkan fitur.
