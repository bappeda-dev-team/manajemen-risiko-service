CREATE TABLE IF NOT EXISTS risiko_pemda
(
    id                              BIGSERIAL PRIMARY KEY,
    kode_risiko                     VARCHAR(255),
    tahun                           INTEGER NOT NULL,
    kode_sasaran_pemda              VARCHAR(128) NOT NULL,
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

    CONSTRAINT uk_risiko_pemda_kode_risiko UNIQUE (kode_risiko),
    CONSTRAINT ck_risiko_pemda_skala_kemungkinan CHECK (skala_kemungkinan BETWEEN 1 AND 5),
    CONSTRAINT ck_risiko_pemda_skala_dampak CHECK (skala_dampak BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_risiko_pemda_sasaran_id
    ON risiko_pemda (kode_sasaran_pemda, id);
