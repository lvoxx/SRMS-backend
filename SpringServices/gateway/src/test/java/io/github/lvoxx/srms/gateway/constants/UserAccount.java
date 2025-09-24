package io.github.lvoxx.srms.gateway.constants;

public enum UserAccount {
    STAFF("staff", "staff"),
    MANAGER("manager", "manager"),
    ADMIN("admin", "admin");

    private final String username;
    private final String password;

    UserAccount(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // Optional: Get enum by username
    public static UserAccount fromUsername(String username) {
        for (UserAccount user : values()) {
            if (user.username.equals(username)) {
                return user;
            }
        }
        throw new IllegalArgumentException("Unknown username: " + username);
    }
}