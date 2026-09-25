ALTER TABLE risiko
    ADD COLUMN pengendalian_yang_sudah_ada TEXT;

ALTER TABLE risiko_pemda
    ADD COLUMN pengendalian_yang_sudah_ada TEXT;

ALTER TABLE risiko_operasional
    ADD COLUMN pengendalian_yang_sudah_ada TEXT;
