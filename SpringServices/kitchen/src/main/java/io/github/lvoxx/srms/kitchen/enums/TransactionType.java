package io.github.lvoxx.srms.kitchen.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {
    WAREHOUSE_RECEIVE("warehouse_receive", "Nhận từ warehouse"),
    EXTERNAL_PURCHASE("external_purchase", "Mua từ bên ngoài"),
    DAMAGE_DISPOSAL("damage_disposal", "Huỷ hàng hư hỏng"),
    DAILY_CHECK("daily_check", "Kiểm đồ cuối ngày"),
    ADJUSTMENT("adjustment", "Điều chỉnh"),
    TRANSFER_OUT("transfer_out", "Chuyển ra"),
    TRANSFER_IN("transfer_in", "Chuyển vào");
    
    private final String value;
    private final String description;
    
    public static TransactionType fromValue(String value) {
        for (TransactionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid transaction type: " + value);
    }
    
    public boolean requiresNotes() {
        return this == DAMAGE_DISPOSAL;
    }
}