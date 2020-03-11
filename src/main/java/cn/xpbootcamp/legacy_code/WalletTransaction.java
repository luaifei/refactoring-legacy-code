package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class WalletTransaction {
    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private String orderId;
    private Long createdTimestamp;
    private double amount;
    private STATUS status;
    private String walletTransactionId;


    public WalletTransaction(String preAssignedId, Long buyerId, Long sellerId, Long productId, String orderId) {
        if (preAssignedId != null && !preAssignedId.isEmpty()) {
            this.id = preAssignedId;
        } else {
            this.id = IdGenerator.generateTransactionId();
        }
        if (!this.id.startsWith("t_")) {
            this.id = "t_" + preAssignedId;
        }

        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.orderId = orderId;
        this.status = STATUS.TO_BE_EXECUTED;
        this.createdTimestamp = getCurrentTimeMillis();
    }

    public boolean execute() throws InvalidTransactionException {
        checkArguments();

        if (isExecuted()) {
            return true;
        }

        return executeWithLock(() -> {
            if (isExecuted()) {
                return true;
            }

            if (isExpired()) {
                return false;
            }

            String walletTransactionId = moveMoney();
            return checkResult(walletTransactionId);
        });
    }

    private boolean executeWithLock(BooleanSupplier businessFunc) {
        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(id);

            if (!isLocked) {
                return false;
            }

            return businessFunc.getAsBoolean();

        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(id);
            }
        }
    }

    private boolean checkResult(String walletTransactionId) {
        if (walletTransactionId != null) {
            this.walletTransactionId = walletTransactionId;
            this.status = STATUS.EXECUTED;
            return true;
        }

        this.status = STATUS.FAILED;
        return false;
    }

    private String moveMoney() {
        WalletService walletService = new WalletServiceImpl();
        return walletService.moveMoney(id, buyerId, sellerId, amount);
    }

    private boolean isExpired() {
        long executionInvokedTimestamp = getCurrentTimeMillis();

        if (isExceedThan20Days(executionInvokedTimestamp)) {
            this.status = STATUS.EXPIRED;
            return true;
        }
        return false;
    }

    private void checkArguments() throws InvalidTransactionException {
        if (buyerId == null || (sellerId == null || amount < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
    }

    private boolean isExceedThan20Days(long executionInvokedTimestamp) {
        return executionInvokedTimestamp - createdTimestamp > 1728000000;
    }

    private long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    private boolean isExecuted() {
        return status == STATUS.EXECUTED;
    }

}