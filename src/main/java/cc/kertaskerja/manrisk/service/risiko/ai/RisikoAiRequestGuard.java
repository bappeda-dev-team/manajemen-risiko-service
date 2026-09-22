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
    private final RisikoAiProperties properties;
    private final Map<String, ArrayDeque<Instant>> requestsByCaller = new ConcurrentHashMap<>();
    private final Map<String, Instant> requestIds = new ConcurrentHashMap<>();
    private volatile Semaphore activeRequests;
    private volatile int semaphoreSize = -1;

    public RisikoAiRequestGuard(RisikoAiProperties properties) { this.properties = properties; }

    public Permit acquire(String caller, String requestId) {
        cleanup();
        String requestKey = caller + ":" + requestId;
        if (requestIds.putIfAbsent(requestKey, Instant.now()) != null) {
            throw new AiException(409, "AI_REQUEST_ID_CONFLICT", "Request generate yang sama sudah diproses.");
        }
        ArrayDeque<Instant> callerRequests = requestsByCaller.computeIfAbsent(caller, ignored -> new ArrayDeque<>());
        synchronized (callerRequests) {
            Instant cutoff = Instant.now().minusSeconds(60);
            while (!callerRequests.isEmpty() && callerRequests.peekFirst().isBefore(cutoff)) callerRequests.removeFirst();
            int limit = Math.max(1, properties.rateLimitPerMinute());
            if (callerRequests.size() >= limit) {
                requestIds.remove(requestKey);
                throw new AiException(429, "AI_RATE_LIMITED", "Terlalu banyak request generate AI. Coba lagi sesaat lagi.");
            }
            callerRequests.addLast(Instant.now());
        }
        Semaphore semaphore = semaphore();
        if (!semaphore.tryAcquire()) {
            requestIds.remove(requestKey);
            throw new AiException(429, "AI_BUSY", "Layanan AI sedang sibuk. Coba lagi sesaat lagi.");
        }
        return new Permit(semaphore);
    }

    private Semaphore semaphore() {
        int capacity = Math.max(1, properties.maxConcurrentRequests());
        if (activeRequests == null || semaphoreSize != capacity) {
            synchronized (this) {
                if (activeRequests == null || semaphoreSize != capacity) {
                    activeRequests = new Semaphore(capacity);
                    semaphoreSize = capacity;
                }
            }
        }
        return activeRequests;
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(300);
        requestIds.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        requestsByCaller.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                while (!entry.getValue().isEmpty() && entry.getValue().peekFirst().isBefore(Instant.now().minusSeconds(60))) entry.getValue().removeFirst();
                return entry.getValue().isEmpty();
            }
        });
    }

    public static final class Permit implements AutoCloseable {
        private final Semaphore semaphore; private boolean closed;
        private Permit(Semaphore semaphore) { this.semaphore = semaphore; }
        @Override public synchronized void close() { if (!closed) { closed = true; semaphore.release(); } }
    }
}
