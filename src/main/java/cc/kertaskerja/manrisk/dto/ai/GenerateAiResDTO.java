package cc.kertaskerja.manrisk.dto.ai;

public record GenerateAiResDTO(
        String requestId,
        String type,
        String contextHash,
        Object result,
        String model
) {}
