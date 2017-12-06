package buildwin.common;

public class BWCommonCallbacks {

    public BWCommonCallbacks() {
    }

    public interface BWCompletionCallbackWithThreeParam<X, Y, Z> {
        void onSuccess(X var1, Y var2, Z var3);

        void onFailure(BWError error);
    }

    public interface BWCompletionCallbackWithTwoParam<X, Y> {
        void onSuccess(X var1, Y var2);

        void onFailure(BWError error);
    }

    public interface BWCompletionCallbackWith<T> {
        void onSuccess(T var1);

        void onFailure(BWError error);
    }

    public interface BWCompletionCallback {
        void onResult(BWError error);
    }

}
