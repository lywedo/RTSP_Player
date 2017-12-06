package cn.com.buildwin.gosky.widget.audiorecognizer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class VoiceRecognizer implements RecognitionListener {

    private static final String TAG = VoiceRecognizer.class.getSimpleName();

    private Context mContext;

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String COMMAND_SEARCH = "command";

    private static final String KEY_PTM = "ptm";
    private static final String KEY_DICT = "dict";
    private static final String KEY_GRAM = "gram";
    private static final String KEY_LM = "lm";

    private SpeechRecognizer recognizer;

    private boolean isReady;
    private boolean waitForReady;

    private VoiceRecognitionListener mVoiceRecognitionListener;
    private HandlerThread listenThread;
    private Handler listenHandler;
    private static HashMap<String, Action> mActionTable;

    public VoiceRecognizer(Context context) {
        mContext = context;

        listenThread = new HandlerThread("listen", THREAD_PRIORITY_BACKGROUND);
        listenThread.start();
        listenHandler = new Handler(listenThread.getLooper());

        runRecognizerSetup();
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(mContext);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    String error = "Failed to init recognizer " + result;
                    onError(error);
                } else {
                    isReady = true;
                    if (waitForReady) {
                        startListening();
                    }
                    waitForReady = false;
                }
            }
        }.execute();
    }

    public void startListening() {
        if (recognizer != null && isReady) {
            if (isInMainThread()) {
                listenHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizer.startListening(COMMAND_SEARCH);
                    }
                });
            }
            onListen();
        } else {
            waitForReady = true;
        }
    }

    public void stopListening() {
        if (recognizer != null && isReady) {
            if (isInMainThread()) {
                listenHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        recognizer.stop();
                    }
                });
            }
            onPause();
        }
    }

    /**
     * Shut down audio recognizer
     */
    public void shutdown() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        Log.d(TAG, "onPartialResult: " + text);

        stopListening();
        startListening();
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            onResult(text);
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
        startListening();
    }

    private void setupRecognizer(File assetsDir) throws IOException {

        HashMap<String, String> fileNameTable = getFileNameTable();

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, fileNameTable.get(KEY_PTM)))
                .setDictionary(new File(assetsDir, fileNameTable.get(KEY_DICT)))
//                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();
        recognizer.addListener(this);

        // Create grammar-based search for command
        File commandGrammar = new File(assetsDir, fileNameTable.get(KEY_GRAM));
        recognizer.addGrammarSearch(COMMAND_SEARCH, commandGrammar);

        // Language model
        File languageModel = new File(assetsDir, fileNameTable.get(KEY_LM));
        recognizer.addNgramSearch(KEY_LM, languageModel);
    }

    @Override
    public void onError(Exception error) {
    }

    @Override
    public void onTimeout() {
    }

    public static enum Action {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        TAKEOFF,
        LANDING
    }

    private HashMap<String, Action> getActionTable() {
        if (mActionTable == null) {
            mActionTable = new HashMap<>();

            if (isLocaleChinese()) {
                mActionTable.put("前进", Action.FORWARD);
                mActionTable.put("后退", Action.BACKWARD);
                mActionTable.put("左侧飞", Action.LEFT);
                mActionTable.put("右侧飞", Action.RIGHT);
                mActionTable.put("起飞", Action.TAKEOFF);
                mActionTable.put("降落", Action.LANDING);
            } else {
                mActionTable.put("forward", Action.FORWARD);
                mActionTable.put("backward", Action.BACKWARD);
                mActionTable.put("left", Action.LEFT);
                mActionTable.put("right", Action.RIGHT);
                mActionTable.put("takeoff", Action.TAKEOFF);
                mActionTable.put("landing", Action.LANDING);
            }
        }
        return mActionTable;
    }

    public void setVoiceRecognitionListener(VoiceRecognitionListener voiceRecognitionListener) {
        mVoiceRecognitionListener = voiceRecognitionListener;
    }

    /**
     * Callback interface
     */
    public interface VoiceRecognitionListener {
        void onListen();
        void onPause();
        void onResult(Action action, String text);
        void onError(String error);
    }

    public void onListen() {
        if (mVoiceRecognitionListener != null) {
            mVoiceRecognitionListener.onListen();
        }
    }

    public void onPause() {
        if (mVoiceRecognitionListener != null) {
            mVoiceRecognitionListener.onPause();
        }
    }

    public void onResult(String text) {
        if (mVoiceRecognitionListener != null) {
            HashMap<String, Action> actionTable = getActionTable();
            Action action = actionTable.get(text);

            if (action != null) {
                mVoiceRecognitionListener.onResult(action, text);
            }
        }
    }

    public void onError(String error) {
        if (mVoiceRecognitionListener != null) {
            mVoiceRecognitionListener.onError(error);
        }
    }

    /**
     * Require file name table of current locale
     */
    private HashMap<String, String> getFileNameTable() {

        HashMap<String, String> fileNameTable = new HashMap<>();
        if (isLocaleChinese()) {
            fileNameTable.put(KEY_PTM, "zh-ptm");
            fileNameTable.put(KEY_DICT, "zh.dict");
            fileNameTable.put(KEY_GRAM, "command.gram");
            fileNameTable.put(KEY_LM, "zh.lm");
        } else {
            fileNameTable.put(KEY_PTM, "en-ptm");
            fileNameTable.put(KEY_DICT, "en.dict");
            fileNameTable.put(KEY_GRAM, "en-command.gram");
            fileNameTable.put(KEY_LM, "en.lm");
        }
        return fileNameTable;
    }

    /**
     * Is language chinese?
     */
    public static boolean isLocaleChinese() {
        String language = Locale.getDefault().getLanguage();
        return language.contains("zh");
    }

    private boolean isInMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
