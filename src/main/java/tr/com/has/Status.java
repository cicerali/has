package tr.com.has;

public enum Status {

    ACTIVE(0, "Active"),
    STANDBY(1, "Standby"),
    UNKNOWN(2, "Unknown");

    private final int code;
    private final String value;

    Status(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Status{" +
                "code=" + code +
                ", value='" + value + '\'' +
                '}';
    }
}
