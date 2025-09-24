package io.github.lvoxx.srms.contactor.dto;

public enum Rating {
    // Các hằng số của enum, mỗi hằng số được khởi tạo với một giá trị rate
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    NO_NEED("No Need");

    // Field (trường) private và final để lưu giá trị rate cho mỗi hằng số
    private final String rate;

    // Constructor của enum là private, được gọi cho mỗi hằng số ở trên
    Rating(String rate) {
        this.rate = rate;
    }

    // Phương thức public để lấy giá trị rate
    public String getRate() {
        return this.rate;
    }
}
