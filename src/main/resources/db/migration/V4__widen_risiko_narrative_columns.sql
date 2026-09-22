-- Narasi risiko berasal dari input pengguna dan rekomendasi generator, sehingga
-- panjangnya tidak dapat dibatasi aman pada VARCHAR(255). Kode dan status tetap
-- memakai tipe semula karena merupakan nilai terstruktur/pendek.
ALTER TABLE risiko
    ALTER COLUMN permasalahan TYPE TEXT,
    ALTER COLUMN sebab_permasalahan TYPE TEXT,
    ALTER COLUMN pernyataan_risiko TYPE TEXT,
    ALTER COLUMN pihak_terkena_risiko TYPE TEXT,
    ALTER COLUMN rencana_tindak_pengendalian TYPE TEXT,
    ALTER COLUMN metode_pemantauan TYPE TEXT,
    ALTER COLUMN penanggungjawab_pemantauan TYPE TEXT,
    ALTER COLUMN keterangan TYPE TEXT,
    ALTER COLUMN realisasi_tindak_pengendalian TYPE TEXT,
    ALTER COLUMN dampak TYPE TEXT,
    ALTER COLUMN catatan TYPE TEXT,
    ALTER COLUMN perangkat_yang_menangani TYPE TEXT;
