package cc.kertaskerja.manrisk.config;


public record ApiProperties(
        PenetapanApi penetapan
) {
    public record PenetapanApi(
            String baseUrl
    ) {
    }
}
