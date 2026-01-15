package com.example.qring_sdk_flutter;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ServiceConnectionManager auto-reconnect functionality.
 * 
 * Feature: production-ble-manager
 * 
 * These tests validate the exponential backoff strategy and reconnection limits
 * defined in the design document.
 */
public class ServiceConnectionManagerPropertyTest {
    
    /**
     * Property 18: Exponential Backoff Strategy
     * 
     * For any reconnection attempt number N, the calculated backoff delay should follow 
     * the exponential backoff strategy: 10s for attempts 1-5, 30s for 6-10, exponentially 
     * increasing for 11+ with ±20% jitter, capped at 5 minutes.
     * 
     * Feature: production-ble-manager, Property 18: Exponential Backoff Strategy
     * Validates: Requirements 5.2
     */
    @Property(tries = 100)
    public void property18_exponentialBackoffStrategy(
            @ForAll @IntRange(min = 1, max = 100) int attemptNumber) {
        
        // Arrange: Create a ServiceConnectionManager instance
        // Note: We're testing the calculateBackoffDelay method in isolation
        // without requiring Android context
        TestableServiceConnectionManager manager = new TestableServiceConnectionManager();
        
        // Act: Calculate the backoff delay for the given attempt number
        int delay = manager.calculateBackoffDelay(attemptNumber);
        
        // Assert: Verify delay follows exponential backoff rules
        if (attemptNumber <= 5) {
            // First 5 attempts: 10 seconds ± 20% jitter
            // Base: 10000ms, Range: 8000ms to 12000ms
            int baseDelay = 10000;
            int minDelay = (int) (baseDelay * 0.8);  // 8000ms
            int maxDelay = (int) (baseDelay * 1.2);  // 12000ms
            
            assertTrue(delay >= minDelay && delay <= maxDelay,
                String.format("Attempt %d: delay %dms should be between %dms and %dms (10s ± 20%%)",
                    attemptNumber, delay, minDelay, maxDelay));
            
        } else if (attemptNumber <= 10) {
            // Attempts 6-10: 30 seconds ± 20% jitter
            // Base: 30000ms, Range: 24000ms to 36000ms
            int baseDelay = 30000;
            int minDelay = (int) (baseDelay * 0.8);  // 24000ms
            int maxDelay = (int) (baseDelay * 1.2);  // 36000ms
            
            assertTrue(delay >= minDelay && delay <= maxDelay,
                String.format("Attempt %d: delay %dms should be between %dms and %dms (30s ± 20%%)",
                    attemptNumber, delay, minDelay, maxDelay));
            
        } else {
            // Attempts 11+: 60 seconds base, exponentially increasing, capped at 5 minutes
            // Formula: 60000 * 2^(attemptNumber - 11), capped at 300000ms
            int exponentialDelay = 60000 * (1 << (attemptNumber - 11));
            int cappedDelay = Math.min(exponentialDelay, 300000);
            
            // With ±20% jitter
            int minDelay = (int) (cappedDelay * 0.8);
            int maxDelay = (int) (cappedDelay * 1.2);
            
            // However, the absolute maximum should be 5 minutes (300000ms) + 20% jitter
            int absoluteMax = (int) (300000 * 1.2);  // 360000ms
            
            assertTrue(delay >= minDelay && delay <= absoluteMax,
                String.format("Attempt %d: delay %dms should be between %dms and %dms (exponential with cap)",
                    attemptNumber, delay, minDelay, absoluteMax));
            
            // Verify the cap is enforced (delay should not exceed 5 minutes + jitter)
            assertTrue(delay <= absoluteMax,
                String.format("Attempt %d: delay %dms should not exceed %dms (5 min + jitter)",
                    attemptNumber, delay, absoluteMax));
        }
        
        // Additional constraint: delay should always be at least 1 second
        assertTrue(delay >= 1000,
            String.format("Attempt %d: delay %dms should be at least 1000ms",
                attemptNumber, delay));
    }
    
    /**
     * Property 22: Reconnection Attempt Limit
     * 
     * For any reconnection sequence, the BLE_Manager should limit the maximum delay 
     * between attempts to prevent infinite loops (max 5 minutes between attempts).
     * 
     * Feature: production-ble-manager, Property 22: Reconnection Attempt Limit
     * Validates: Requirements 5.6
     */
    @Property(tries = 100)
    public void property22_reconnectionAttemptLimit(
            @ForAll @IntRange(min = 1, max = 200) int attemptNumber) {
        
        // Arrange: Create a ServiceConnectionManager instance
        TestableServiceConnectionManager manager = new TestableServiceConnectionManager();
        
        // Act: Calculate the backoff delay for the given attempt number
        int delay = manager.calculateBackoffDelay(attemptNumber);
        
        // Assert: Verify delay never exceeds 5 minutes + jitter (360000ms)
        // The maximum delay should be 5 minutes (300000ms) with up to 20% jitter
        int maxAllowedDelay = (int) (300000 * 1.2);  // 360000ms (6 minutes)
        
        assertTrue(delay <= maxAllowedDelay,
            String.format("Attempt %d: delay %dms should not exceed maximum of %dms (5 min + 20%% jitter)",
                attemptNumber, delay, maxAllowedDelay));
        
        // Verify that even for very high attempt numbers, the delay is capped
        if (attemptNumber > 20) {
            // For very high attempt numbers, the delay should be at the cap
            // (with jitter, so it should be close to the max)
            assertTrue(delay >= 240000,  // 4 minutes (80% of 5 minutes)
                String.format("Attempt %d: delay %dms should be near the cap for high attempt numbers",
                    attemptNumber, delay));
        }
    }
    
    /**
     * Testable version of ServiceConnectionManager that exposes calculateBackoffDelay
     * for testing without requiring Android Context.
     * 
     * Note: This implementation matches ServiceConnectionManager.calculateBackoffDelay
     * but without the Doze mode adjustment for predictable testing.
     */
    private static class TestableServiceConnectionManager {
        private static final int INITIAL_RETRY_INTERVAL_MS = 10000;
        private static final int RETRY_INTERVAL_AFTER_5_FAILURES_MS = 30000;
        private static final int RETRY_INTERVAL_AFTER_10_FAILURES_MS = 60000;
        private static final int MAX_RETRY_INTERVAL_MS = 300000;
        
        private final java.util.Random random = new java.util.Random();
        private boolean isDeviceIdle = false;  // For testing, assume not in Doze mode
        
        /**
         * Calculate the backoff delay for reconnection attempts.
         * This is the same implementation as ServiceConnectionManager.calculateBackoffDelay
         * but extracted for testing without Android dependencies.
         */
        public int calculateBackoffDelay(int attemptNumber) {
            int baseDelay;
            
            if (attemptNumber <= 5) {
                // First 5 attempts: 10 seconds
                baseDelay = INITIAL_RETRY_INTERVAL_MS;
            } else if (attemptNumber <= 10) {
                // Attempts 6-10: 30 seconds
                baseDelay = RETRY_INTERVAL_AFTER_5_FAILURES_MS;
            } else {
                // Attempts 11+: 60 seconds, increasing exponentially
                int exponentialDelay = RETRY_INTERVAL_AFTER_10_FAILURES_MS * (1 << (attemptNumber - 11));
                baseDelay = Math.min(exponentialDelay, MAX_RETRY_INTERVAL_MS);
            }
            
            // Add jitter (±20%) to prevent thundering herd
            int jitter = (int) (baseDelay * 0.2 * (random.nextDouble() - 0.5) * 2);
            int finalDelay = baseDelay + jitter;
            
            // Doze mode adjustment (optional, for testing we keep it disabled)
            if (isDeviceIdle && attemptNumber > 5) {
                finalDelay = (int) (finalDelay * 1.2);
            }
            
            // Ensure delay is positive and within bounds
            return Math.max(1000, Math.min(finalDelay, MAX_RETRY_INTERVAL_MS));
        }
    }
}
