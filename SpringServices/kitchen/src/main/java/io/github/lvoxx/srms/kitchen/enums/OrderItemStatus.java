package io.github.lvoxx.srms.kitchen.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderItemStatus {
    PENDING("pending", "Chờ xử lý"),
    PREPARING("preparing", "Đang chế biến"),
    READY("ready", "Sẵn sàng"),
    REJECTED("rejected", "Từ chối");

    private final String value;
    private final String description;

    public static OrderItemStatus fromValue(String value) {
        for (OrderItemStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid order item status: " + value);
    }
}