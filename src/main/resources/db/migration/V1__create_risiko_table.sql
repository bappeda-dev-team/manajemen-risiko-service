CREATE TABLE IF NOT EXISTS risiko
(
    id            BIGSERIAL PRIMARY KEY,
    kode_opd VARCHAR(255),
    kode_risiko VARCHAR(255),
    tahun INTEGER,
    kode_sasaran_opd VARCHAR(255),
    pernyataan_risiko     VARCHAR(255),
    skala_kemungkinan INTEGER,
    skala_dampak INTEGER,
    pihak_terkena_risiko VARCHAR(255),
    rencana_tindak_pengendalian VARCHAR(255),
    metode_pemantauan VARCHAR(255),
    penanggungjawab_pemantauan VARCHAR(255),
    keterangan VARCHAR(255),
    realisasi_tindak_pengendalian VARCHAR(255),
    dapat_terkendali VARCHAR(255),
    dampak VARCHAR(255),
    catatan VARCHAR(255),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);