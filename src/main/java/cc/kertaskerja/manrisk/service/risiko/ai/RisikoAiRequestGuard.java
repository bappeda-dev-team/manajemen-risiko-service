package cc.kertaskerja.manrisk.service.risiko.ai;

import cc.kertaskerja.manrisk.config.RisikoAiProperties;
import cc.kertaskerja.manrisk.exception.AiException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
public class RisikoAiRequestGuard {

    private static final int RATE_LIMIT_WINDOW_SECONDS = 60;
    private static final int REQUEST_ID_RETENTION_SECONDS = 300;

    private final RisikoAiProperties properties;
    private final Map<String, ArrayDeque<Instant>> requestsByCaller = new ConcurrentHashMap<>();
    private final Map<String, Instant> requestIds = new ConcurrentHashMap<>();

    private volatile Semaphore availableAiSlots;
    private volatile int availableAiSlotCapacity = -1;

    public RisikoAiRequestGuard(RisikoAiProperties properties) {
        this.properties = properties;
    }

    public Permit acquire(String caller, String requestId) {
        cleanup();

        Instant now = Instant.now();
        String requestKey = caller + ":" + requestId;
        boolean requestAlreadyExists = requestIds.putIfAbsent(requestKey, now) != null;

        if (requestAlreadyExists) {
            throw new AiException(409, "AI_REQUEST_ID_CONFLICT", "Request generate yang sama sudah diproses.");
        }

        ArrayDeque<Instant> callerRequests = requestsByCaller.computeIfAbsent(caller, ignored -> new ArrayDeque<>());

        synchronized (callerRequests) {
            Instant cutoff = now.minusSeconds(RATE_LIMIT_WINDOW_SECONDS);

            removeRequestsBefore(callerRequests, cutoff);

            int rateLimit = Math.max(1, properties.rateLimitPerMinute());

            if (callerRequests.size() >= rateLimit) {
                requestIds.remove(requestKey);

                throw new AiException(429, "AI_RATE_LIMITED", "Terlalu banyak request generate AI. Coba lagi sesaat lagi.");
            }

            callerRequests.addLast(now);
        }

        Semaphore availableSlots = getAvailableAiSlots();

        if (!availableSlots.tryAcquire()) {
            requestIds.remove(requestKey);

            throw new AiException(429, "AI_BUSY", "Layanan AI sedang sibuk.");
        }

        return new Permit(availableSlots);
    }

    private Semaphore getAvailableAiSlots() {
        int configuredCapacity = Math.max(1, properties.maxConcurrentRequests());
        boolean needsInitialization = availableAiSlots == null || availableAiSlotCapacity != configuredCapacity;

        if (needsInitialization) {
            synchronized (this) {
                boolean configurationChanged = availableAiSlots == null || availableAiSlotCapacity != configuredCapacity;

                if (configurationChanged) {
                    availableAiSlots = new Semaphore(configuredCapacity);
                    availableAiSlotCapacity = configuredCapacity;
                }
            }
        }

        return availableAiSlots;
    }

    private void cleanup() {
        Instant now = Instant.now();
        Instant requestIdCutoff = now.minusSeconds(REQUEST_ID_RETENTION_SECONDS);
        Instant rateLimitCutoff = now.minusSeconds(RATE_LIMIT_WINDOW_SECONDS);

        requestIds.entrySet().removeIf(entry -> entry.getValue().isBefore(requestIdCutoff));

        requestsByCaller.entrySet().removeIf(entry -> {
            ArrayDeque<Instant> callerRequests = entry.getValue();

            synchronized (callerRequests) {
                removeRequestsBefore(callerRequests, rateLimitCutoff);

                return callerRequests.isEmpty();
            }
        });
    }

    private static void removeRequestsBefore(ArrayDeque<Instant> requests, Instant cutoff) {
        while (!requests.isEmpty() && requests.peekFirst().isBefore(cutoff)) {
            requests.removeFirst();
        }
    }

    public static final class Permit implements AutoCloseable {

        private final Semaphore availableSlots;
        private boolean closed;

        private Permit(Semaphore availableSlots) {
            this.availableSlots = availableSlots;
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }

            closed = true;
            availableSlots.release();
        }
    }
}
