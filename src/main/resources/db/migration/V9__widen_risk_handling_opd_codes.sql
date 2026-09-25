-- Satu risiko dapat ditangani oleh maksimal lima OPD. Kode OPD disimpan sebagai
-- daftar yang dipisahkan koma, sehingga tidak lagi aman dibatasi VARCHAR(100/128).
ALTER TABLE risiko
    ALTER COLUMN kode_perangkat_yang_menangani TYPE TEXT;

ALTER TABLE risiko_pemda
    ALTER COLUMN kode_perangkat_yang_menangani TYPE TEXT;

ALTER TABLE risiko_operasional
    ALTER COLUMN kode_perangkat_yang_menangani TYPE TEXT;
