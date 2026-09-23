-- Kode lama dapat duplikat karena sebelumnya dibuat dari jumlah row (count + 1).
-- Samakan seluruh kode dengan primary key yang unik sebelum constraint diterapkan.
UPDATE risiko
SET kode_risiko = 'RSK-' || LPAD(id::text, 4, '0');

ALTER TABLE risiko
    ADD CONSTRAINT uk_risiko_kode_risiko UNIQUE (kode_risiko);
