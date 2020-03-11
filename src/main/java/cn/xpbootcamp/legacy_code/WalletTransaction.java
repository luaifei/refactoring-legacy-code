package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;

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
        if (buyerId == null || (sellerId == null || amount < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }

        if (isExecuted()) {
            return true;
        }

        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(id);

            if (!isLocked) {
                return false;
            }

            if (isExecuted()) {
                return true;
            }

            long executionInvokedTimestamp = getCurrentTimeMillis();

            if (isExceedThan20Days(executionInvokedTimestamp)) {
                this.status = STATUS.EXPIRED;
                return false;
            }

            WalletService walletService = new WalletServiceImpl();
            String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);

            if (walletTransactionId != null) {
                this.walletTransactionId = walletTransactionId;
                this.status = STATUS.EXECUTED;
                return true;
            } else {
                this.status = STATUS.FAILED;
                return false;
            }

        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(id);
            }
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