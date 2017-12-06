package buildwin.common;

public class BWError {

    public static final BWError COMMON_TIMEOUT = new BWError("Execution of this process has timed out");
    public static final BWError COMMON_PARAM_ILLEGAL = new BWError("Param Illegal");
    public static final BWError COMMON_PARAM_INVALID = new BWError("Param Invalid");
    public static final BWError COMMON_UNSUPPORTED = new BWError("Not supported");
    public static final BWError COMMON_DISCONNECTED = new BWError("Disconnected");

    private String mDescription;

    protected BWError() {
    }

    private BWError(String description) {
        this.mDescription = description;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    public String toString() {
        if (this.mDescription != null) {
            return this.mDescription;
        }
        return super.toString();
    }

}
