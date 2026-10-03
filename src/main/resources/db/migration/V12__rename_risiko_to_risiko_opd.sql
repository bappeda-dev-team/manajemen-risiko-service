-- Risiko pada tabel legacy `risiko` adalah Risiko OPD. Pastikan seluruh kode
-- masih mengikuti bentuk canonical V6 sebelum melakukan perubahan in-place.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM risiko
        WHERE kode_risiko IS NULL
           OR kode_risiko <> 'RSK-' ||
              CASE
                  WHEN length(id::text) < 4 THEN lpad(id::text, 4, '0')
                  ELSE id::text
              END
    ) THEN
        RAISE EXCEPTION
            'Migrasi Risiko OPD dibatalkan: ditemukan kode_risiko legacy yang tidak sesuai dengan id';
    END IF;
END
$$;

ALTER TABLE risiko
    RENAME TO risiko_opd;

ALTER SEQUENCE risiko_id_seq
    RENAME TO risiko_opd_id_seq;

ALTER TABLE risiko_opd
    RENAME CONSTRAINT risiko_pkey TO risiko_opd_pkey;

ALTER TABLE risiko_opd
    RENAME CONSTRAINT uk_risiko_kode_risiko TO uk_risiko_opd_kode_risiko;

ALTER TABLE risiko_opd
    RENAME CONSTRAINT ck_risiko_kejadian_consistent TO ck_risiko_opd_kejadian_consistent;

UPDATE risiko_opd
SET kode_risiko = 'RSK-OPD-' ||
    CASE
        WHEN length(id::text) < 4 THEN lpad(id::text, 4, '0')
        ELSE id::text
    END;

CREATE INDEX IF NOT EXISTS idx_risiko_opd_sasaran_id
    ON risiko_opd (kode_sasaran_opd, id);

-- Lindungi insert berikutnya bila data pernah dimasukkan dengan ID manual.
SELECT setval(
    pg_get_serial_sequence('risiko_opd', 'id'),
    COALESCE(MAX(id), 1),
    MAX(id) IS NOT NULL
)
FROM risiko_opd;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM risiko_opd
        WHERE kode_risiko <> 'RSK-OPD-' ||
              CASE
                  WHEN length(id::text) < 4 THEN lpad(id::text, 4, '0')
                  ELSE id::text
              END
    ) THEN
        RAISE EXCEPTION
            'Migrasi Risiko OPD dibatalkan: postcondition kode_risiko gagal';
    END IF;
END
$$;
