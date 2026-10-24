-- Laporan OPD selalu membatasi kode OPD dan tahun; pilihan sasaran menambah IN.
CREATE INDEX IF NOT EXISTS idx_risiko_opd_laporan_context_sasaran_id
    ON risiko_opd (kode_opd, tahun, kode_sasaran_opd, id);

-- Laporan Pemda selalu membatasi tahun; pilihan sasaran menambah IN.
CREATE INDEX IF NOT EXISTS idx_risiko_pemda_laporan_tahun_sasaran_id
    ON risiko_pemda (tahun, kode_sasaran_pemda, id);
