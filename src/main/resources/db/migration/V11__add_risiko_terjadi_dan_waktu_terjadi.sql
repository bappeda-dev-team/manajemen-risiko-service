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
