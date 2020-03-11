package cn.xpbootcamp.legacy_code.entity;

import javax.transaction.InvalidTransactionException;

public class Order {
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private String orderId;
    private double amount;

    public Order(Long buyerId, Long sellerId, Long productId, String orderId, double amount) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.orderId = orderId;
        this.amount = amount;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isValid() {
        return buyerId != null && (sellerId != null && !(amount < 0.0));
    }
}
