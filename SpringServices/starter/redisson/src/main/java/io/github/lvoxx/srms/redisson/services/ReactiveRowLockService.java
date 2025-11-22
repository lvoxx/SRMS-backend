package io.github.lvoxx.srms.redisson.services;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class ReactiveRowLockService {

    private final RedissonReactiveClient redissonReactive;
    private final long waitTimeSeconds;
    private final long leaseTimeSeconds;

    public ReactiveRowLockService(
            RedissonReactiveClient redissonReactive,
            @Value("${db.row-lock:10}") long waitTimeSeconds,
            @Value("${db.lease-time:30}") long leaseTimeSeconds) {
        this.redissonReactive = redissonReactive;
        this.waitTimeSeconds = waitTimeSeconds;
        this.leaseTimeSeconds = leaseTimeSeconds;
    }

    public Mono<RLockReactive> acquireLock(String table, UUID rowId) {
        String lockKey = "lock:" + table + ":" + rowId;
        RLockReactive lock = redissonReactive.getLock(lockKey);

        return lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS)
                .flatMap(acquired -> {
                    if (acquired) {
                        return Mono.just(lock);
                    } else {
                        return Mono.error(new RuntimeException("Cannot acquire lock for " + lockKey));
                    }
                });
    }

    public Mono<Void> releaseLock(RLockReactive lock) {
        return lock.isHeldByThread(leaseTimeSeconds)
                .flatMap(held -> held ? lock.unlock() : Mono.empty())
                .onErrorResume(e -> {
                    System.err.println("Failed to release lock: " + e.getMessage());
                    return Mono.empty();
                });
    }
}
