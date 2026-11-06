ALTER TABLE risiko_opd
    ADD COLUMN created_by VARCHAR(128),
    ADD COLUMN created_name VARCHAR(255),
    ADD COLUMN created_jabatan VARCHAR(255),
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN revisi_dari VARCHAR(255),
    ALTER COLUMN skala_kemungkinan DROP NOT NULL,
    ALTER COLUMN skala_dampak DROP NOT NULL,
    ALTER COLUMN rencana_tindak_pengendalian DROP NOT NULL,
    ALTER COLUMN kode_perangkat_yang_menangani DROP NOT NULL,
    ADD CONSTRAINT fk_risiko_opd_revisi_dari
        FOREIGN KEY (revisi_dari) REFERENCES risiko_opd(kode_risiko) ON DELETE RESTRICT;

ALTER TABLE risiko_pemda
    ADD COLUMN kode_opd VARCHAR(128),
    ADD COLUMN created_by VARCHAR(128),
    ADD COLUMN created_name VARCHAR(255),
    ADD COLUMN created_jabatan VARCHAR(255),
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN revisi_dari VARCHAR(255),
    ALTER COLUMN skala_kemungkinan DROP NOT NULL,
    ALTER COLUMN skala_dampak DROP NOT NULL,
    ALTER COLUMN rencana_tindak_pengendalian DROP NOT NULL,
    ALTER COLUMN kode_perangkat_yang_menangani DROP NOT NULL,
    ADD CONSTRAINT fk_risiko_pemda_revisi_dari
        FOREIGN KEY (revisi_dari) REFERENCES risiko_pemda(kode_risiko) ON DELETE RESTRICT;

ALTER TABLE risiko_operasional
    ADD COLUMN created_by VARCHAR(128),
    ADD COLUMN created_name VARCHAR(255),
    ADD COLUMN created_jabatan VARCHAR(255),
    ADD COLUMN deleted_at TIMESTAMPTZ,
    ADD COLUMN revisi_dari VARCHAR(255),
    ALTER COLUMN skala_kemungkinan DROP NOT NULL,
    ALTER COLUMN skala_dampak DROP NOT NULL,
    ALTER COLUMN rencana_tindak_pengendalian DROP NOT NULL,
    ALTER COLUMN kode_perangkat_yang_menangani DROP NOT NULL,
    ADD CONSTRAINT fk_risiko_operasional_revisi_dari
        FOREIGN KEY (revisi_dari) REFERENCES risiko_operasional(kode_risiko) ON DELETE RESTRICT;

CREATE TABLE verifikasi_risiko (
    id BIGSERIAL PRIMARY KEY,
    kode_risiko VARCHAR(255) NOT NULL UNIQUE,
    risiko_operasional_id BIGINT REFERENCES risiko_operasional(id) ON DELETE RESTRICT,
    risiko_opd_id BIGINT REFERENCES risiko_opd(id) ON DELETE RESTRICT,
    risiko_pemda_id BIGINT REFERENCES risiko_pemda(id) ON DELETE RESTRICT,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verifikator_id VARCHAR(128),
    verifikator_nama VARCHAR(255),
    catatan TEXT,
    CONSTRAINT ck_verifikasi_risiko_one_risk
        CHECK (num_nonnulls(risiko_operasional_id, risiko_opd_id, risiko_pemda_id) = 1),
    CONSTRAINT ck_verifikasi_risiko_status
        CHECK (status IN ('SUBMITTED', 'VERIFIED', 'REJECTED')),
    CONSTRAINT ck_verifikasi_risiko_decision
        CHECK (
            (status = 'SUBMITTED' AND verifikator_id IS NULL AND verifikator_nama IS NULL AND catatan IS NULL)
            OR (status IN ('VERIFIED', 'REJECTED') AND verifikator_id IS NOT NULL AND verifikator_nama IS NOT NULL)
        ),
    CONSTRAINT ck_verifikasi_risiko_rejected_note
        CHECK (status <> 'REJECTED' OR length(trim(catatan)) BETWEEN 1 AND 2000),
    CONSTRAINT ck_verifikasi_risiko_note_length
        CHECK (catatan IS NULL OR length(catatan) <= 2000),
    CONSTRAINT uk_verifikasi_risiko_operasional UNIQUE (risiko_operasional_id),
    CONSTRAINT uk_verifikasi_risiko_opd UNIQUE (risiko_opd_id),
    CONSTRAINT uk_verifikasi_risiko_pemda UNIQUE (risiko_pemda_id)
);

CREATE INDEX idx_verifikasi_risiko_queue
    ON verifikasi_risiko (status, created_at DESC);
CREATE INDEX idx_risiko_opd_pengajuan ON risiko_opd (created_by, deleted_at);
CREATE INDEX idx_risiko_pemda_pengajuan ON risiko_pemda (created_by, deleted_at);
CREATE INDEX idx_risiko_operasional_pengajuan ON risiko_operasional (created_by, deleted_at);
