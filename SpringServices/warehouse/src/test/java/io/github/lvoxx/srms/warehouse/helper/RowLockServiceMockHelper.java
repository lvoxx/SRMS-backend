package io.github.lvoxx.srms.warehouse.helper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.redisson.api.RLockReactive;

import io.github.lvoxx.srms.redisson.services.ReactiveRowLockService;
import reactor.core.publisher.Mono;

public class RowLockServiceMockHelper {

    private RowLockServiceMockHelper() {}

    /**
     * Setup mock để acquireLock luôn thành công và trả về một lock giả.
     * releaseLock luôn thành công.
     */
    public static void setupSuccessfulLock(ReactiveRowLockService rowLockService) {
        RLockReactive mockLock = mock(RLockReactive.class);

        // acquireLock luôn trả về lock
        when(rowLockService.acquireLock(anyString(), any(UUID.class)))
                .thenReturn(Mono.just(mockLock));

        // releaseLock luôn thành công
        when(rowLockService.releaseLock(any(RLockReactive.class)))
                .thenReturn(Mono.empty());

        // Cần thiết để isHeldByThread trả true (nếu service dùng)
        when(mockLock.isHeldByThread(anyLong())).thenReturn(Mono.just(true));
        when(mockLock.unlock()).thenReturn(Mono.empty());
    }

    /**
     * Setup mock để acquireLock thất bại
     */
    public static void setupLockFailure(ReactiveRowLockService rowLockService, String errorMessage) {
        when(rowLockService.acquireLock(anyString(), any(UUID.class)))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        // Không cần setup releaseLock vì không có lock
    }

    /**
     * Setup mock để acquireLock thành công, nhưng releaseLock thất bại (log error)
     */
    public static void setupLockReleaseFailure(ReactiveRowLockService rowLockService) {
        RLockReactive mockLock = mock(RLockReactive.class);

        when(rowLockService.acquireLock(anyString(), any(UUID.class)))
                .thenReturn(Mono.just(mockLock));

        when(rowLockService.releaseLock(any(RLockReactive.class)))
                .thenReturn(Mono.error(new RuntimeException("Unlock failed")));

        when(mockLock.isHeldByThread(anyLong())).thenReturn(Mono.just(true));
    }
}