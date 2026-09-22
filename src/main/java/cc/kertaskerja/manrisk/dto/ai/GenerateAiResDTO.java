package cc.kertaskerja.manrisk.dto.ai;

public record GenerateAiResDTO(
        String requestId,
        String type,
        String contextVersion,
        Object result,
        String model
) {}
