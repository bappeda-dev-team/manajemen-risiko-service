package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RisikoAiOutputValidatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RisikoAiOutputValidator validator = new RisikoAiOutputValidator(objectMapper);

    @Test
    void addsApplicationProposalIdsToValidRiskStatements() throws Exception {
        JsonNode raw = objectMapper.readTree("""
                {"proposals":[
                  {"kategori":"Operasional","pernyataan_risiko":"Risiko satu","peristiwa":"Peristiwa satu","penyebab":"Penyebab satu","dampak":"Dampak satu"},
                  {"kategori":"Kepatuhan","pernyataan_risiko":"Risiko dua","peristiwa":"Peristiwa dua","penyebab":"Penyebab dua","dampak":"Dampak dua"},
                  {"kategori":"Data & Sistem","pernyataan_risiko":"Risiko tiga","peristiwa":"Peristiwa tiga","penyebab":"Penyebab tiga","dampak":"Dampak tiga"},
                  {"kategori":"Keuangan","pernyataan_risiko":"Risiko empat","peristiwa":"Peristiwa empat","penyebab":"Penyebab empat","dampak":"Dampak empat"}
                ]}
                """);

        JsonNode normalized = validator.normalize("pernyataan-risiko", raw);

        assertEquals(4, normalized.path("proposals").size());
        assertEquals("Risiko satu", normalized.path("proposals").path(0).path("pernyataan_risiko").asText());
        assertEquals(36, normalized.path("proposals").path(0).path("id").asText().length());
    }

    @Test
    void rejectsWrongProposalCountAndInvalidMonitoringNature() throws Exception {
        JsonNode tooFew = objectMapper.readTree("{\"proposals\":[]}");
        assertThrows(AiException.class, () -> validator.normalize("rtp", tooFew));

        JsonNode invalidNature = objectMapper.readTree("""
                {"proposals":[
                  {"aktivitas_pemantauan":"A","sifat":"Harian","frekuensi":"Bulanan"},
                  {"aktivitas_pemantauan":"B","sifat":"Harian","frekuensi":"Bulanan"},
                  {"aktivitas_pemantauan":"C","sifat":"Harian","frekuensi":"Bulanan"},
                  {"aktivitas_pemantauan":"D","sifat":"Harian","frekuensi":"Bulanan"},
                  {"aktivitas_pemantauan":"E","sifat":"Harian","frekuensi":"Bulanan"}
                ]}
                """);
        assertThrows(AiException.class, () -> validator.normalize("metode-pemantauan", invalidNature));
    }

    @Test
    void acceptsValidRtpWithDescriptiveApproach() throws Exception {
        JsonNode raw = objectMapper.readTree("""
                {"proposals":[
                  {"pendekatan":"Penguatan kapasitas dan standardisasi proses pengelolaan data kinerja lintas unit secara terukur dan berkelanjutan","rencana_tindak_pengendalian":"Menyusun standar kerja dan meningkatkan kapasitas pengelola data.","rtp":{"preventif":["Menyusun SOP pengelolaan data"],"detektif":["Melakukan reviu kualitas data berkala"],"korektif":["Memperbaiki data yang tidak sesuai"]}},
                  {"pendekatan":"Integrasi sistem dan validasi data","rencana_tindak_pengendalian":"Membangun validasi dan integrasi pada alur pelaporan.","rtp":{"preventif":["Menetapkan aturan validasi"],"detektif":["Membuat laporan anomali"],"korektif":["Menindaklanjuti anomali data"]}},
                  {"pendekatan":"Pengawasan dan evaluasi berjenjang","rencana_tindak_pengendalian":"Menjalankan evaluasi berjenjang atas ketepatan pelaporan.","rtp":{"preventif":["Menetapkan jadwal evaluasi"],"detektif":["Memeriksa ketepatan laporan"],"korektif":["Memberikan pendampingan perbaikan"]}}
                ]}
                """);

        JsonNode normalized = validator.normalize("rtp", raw);

        assertEquals(3, normalized.path("proposals").size());
        assertEquals("Penguatan kapasitas dan standardisasi proses pengelolaan data kinerja lintas unit secara terukur dan berkelanjutan",
                normalized.path("proposals").path(0).path("pendekatan").asText());
        assertEquals(36, normalized.path("proposals").path(0).path("id").asText().length());
    }
}
