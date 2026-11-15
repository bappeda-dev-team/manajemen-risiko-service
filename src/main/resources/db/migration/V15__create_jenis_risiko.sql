CREATE TABLE jenis_risiko
(
    id         BIGSERIAL PRIMARY KEY,
    nama       VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_jenis_risiko_nama_not_blank CHECK (length(btrim(nama)) > 0)
);

CREATE UNIQUE INDEX uk_jenis_risiko_nama_normalized
    ON jenis_risiko (lower(btrim(nama)));

INSERT INTO jenis_risiko (nama) VALUES
    ('Risiko Strategis'),
    ('Risiko Operasional'),
    ('Risiko Keuangan dan Anggaran (Fiskal)'),
    ('Risiko Kepatuhan (Hukum dan Regulasi)'),
    ('Risiko Kecurangan (Fraud Risk)'),
    ('Risiko Reputasi'),
    ('Risiko Bencana dan Keadaan Kahar (Force Majeure)');
