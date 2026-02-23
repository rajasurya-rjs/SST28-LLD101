public class SendResult {
    public final boolean success;
    public final String errorMessage;

    private SendResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static SendResult ok() {
        return new SendResult(true, null);
    }

    public static SendResult error(String message) {
        return new SendResult(false, message);
    }
}
