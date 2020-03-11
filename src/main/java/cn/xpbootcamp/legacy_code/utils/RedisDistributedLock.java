package cn.xpbootcamp.legacy_code.utils;

import java.util.function.BooleanSupplier;

public class RedisDistributedLock {
    private static final RedisDistributedLock INSTANCE = new RedisDistributedLock();

    public static RedisDistributedLock getSingletonInstance() {
        return INSTANCE;
    }

    public boolean lock(String transactionId) {
        // Here is connecting to redis server, please do not invoke directly
        throw new RuntimeException("Redis server is connecting......");
    }

    public void unlock(String transactionId) {
        // Here is connecting to redis server, please do not invoke directly
        throw new RuntimeException("Redis server is connecting......");
    }

    public static boolean executeWithLock(BooleanSupplier businessFunc, String key) {
        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(key);

            if (!isLocked) {
                return false;
            }

            return businessFunc.getAsBoolean();

        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(key);
            }
        }
    }
}
