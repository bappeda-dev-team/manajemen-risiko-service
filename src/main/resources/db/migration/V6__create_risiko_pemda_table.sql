CREATE TABLE IF NOT EXISTS risiko_pemda
(
    id                              BIGSERIAL PRIMARY KEY,
    kode_opd                        VARCHAR(255),
    kode_risiko                     VARCHAR(255),
    tahun                           INTEGER,
    kode_sasaran_opd                VARCHAR(255),
    permasalahan                    TEXT,
    sebab_permasalahan              TEXT,
    pernyataan_risiko               TEXT,
    skala_kemungkinan               INTEGER,
    skala_dampak                    INTEGER,
    pihak_terkena_risiko            TEXT,
    rencana_tindak_pengendalian     TEXT,
    metode_pemantauan               TEXT,
    penanggungjawab_pemantauan      TEXT,
    keterangan                      TEXT,
    realisasi_tindak_pengendalian   TEXT,
    dapat_terkendali                VARCHAR(255),
    dampak                          TEXT,
    catatan                         TEXT,
    perangkat_yang_menangani        TEXT,
    kode_perangkat_yang_menangani   VARCHAR(100),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_risiko_pemda_kode_risiko UNIQUE (kode_risiko)
);