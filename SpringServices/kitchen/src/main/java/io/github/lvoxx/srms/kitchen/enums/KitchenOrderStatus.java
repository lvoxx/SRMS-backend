package io.github.lvoxx.srms.kitchen.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KitchenOrderStatus {
    RECEIVED("received", "Đã nhận"),
    KITCHEN_REJECTED("kitchen_rejected", "Nhà bếp từ chối"),
    ADJUSTED("adjusted", "Điều chỉnh"),
    PREPARING("preparing", "Đang chế biến"),
    CUSTOMER_REJECTED("customer_rejected", "Khách từ chối"),
    COMPLETED("completed", "Hoàn thành");
    
    private final String value;
    private final String description;
    
    public static KitchenOrderStatus fromValue(String value) {
        for (KitchenOrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid kitchen order status: " + value);
    }
    
    public boolean requiresKitchenNote() {
        return this == KITCHEN_REJECTED || this == ADJUSTED;
    }
    
    public boolean requiresCustomerNote() {
        return this == CUSTOMER_REJECTED;
    }
}