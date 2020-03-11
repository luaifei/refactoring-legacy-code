package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.entity.Order;
import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
    private String id;

    private Order order;
    private STATUS status;
    private String walletTransactionId;

    private Long createdTimestamp;

    public WalletTransaction(String preAssignedId, Order order) {
        this.id = generateId(preAssignedId);

        this.order = order;
        this.status = STATUS.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
    }

    private String generateId(String preAssignedId) {
        String id = preAssignedId;
        if (id == null || id.isEmpty()) {
            id = IdGenerator.generateTransactionId();
        }

        if (!id.startsWith("t_")) {
            id = "t_" + preAssignedId;
        }

        return id;
    }

    public boolean execute() throws InvalidTransactionException {
        if (!order.isValid()) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }

        if (isExecuted()) {
            return true;
        }

        return RedisDistributedLock.executeWithLock(() -> {
            if (isExecuted()) {
                return true;
            }

            if (isExpired()) {
                return false;
            }

            WalletService walletService = new WalletServiceImpl();
            String walletTransactionId = walletService.moveMoney(id, order.getBuyerId(),
                    order.getSellerId(), order.getAmount());

            return checkResult(walletTransactionId);
        }, this.id);
    }

    private boolean checkResult(String walletTransactionId) {
        if (isTransactSuccess(walletTransactionId)) {
            this.walletTransactionId = walletTransactionId;
            this.status = STATUS.EXECUTED;
            return true;
        }

        this.status = STATUS.FAILED;
        return false;
    }

    private boolean isTransactSuccess(String walletTransactionId) {
        return walletTransactionId != null;
    }

    private boolean isExpired() {
        long executionInvokedTimestamp = System.currentTimeMillis();

        if (isExceedThan20Days(executionInvokedTimestamp)) {
            this.status = STATUS.EXPIRED;
            return true;
        }
        return false;
    }

    private boolean isExceedThan20Days(long executionInvokedTimestamp) {
        return executionInvokedTimestamp - createdTimestamp > 1728000000;
    }

    private boolean isExecuted() {
        return status == STATUS.EXECUTED;
    }

}