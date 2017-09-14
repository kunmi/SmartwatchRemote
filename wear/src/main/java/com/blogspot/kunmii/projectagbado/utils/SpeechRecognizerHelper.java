package com.blogspot.kunmii.projectagbado.utils;

import android.app.Activity;
import android.os.AsyncTask;

import com.blogspot.kunmii.projectagbado.SpeechRecognizerSetupListener;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by Olakunmi on 29/11/2016.
 */

public class SpeechRecognizerHelper {

    Activity mActivity;
    SpeechRecognizerSetupListener initializationListener;

    SpeechRecognizer recognizer;

    RecognitionListener recognizationListener;

    public static String WAKEUP_CALL = "WAKEUP";
    public static String KEYWORD_CALLS = "MENU";
    public static String FREE_FORM = "phones";

    public SpeechRecognizerHelper(Activity activity, SpeechRecognizerSetupListener listener)
    {
        this.mActivity = activity;
        this.initializationListener = listener;
    }

    public SpeechRecognizer getRecognizer() {
        return recognizer;
    }


    public void startInitialization(RecognitionListener listener)
    {
        recognizationListener = listener;

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(mActivity);
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
                    initializationListener.onFailedInitialization();
                } else {
                    initializationListener.onSuccessfulInitialization();
                    //switchSearch(WAKEUP_CALL);
                }
            }
        }.execute();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

               // .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();
        recognizer.addListener(recognizationListener);

        recognizer.addKeywordSearch(WAKEUP_CALL, new File(assetsDir,
                "commands.lst"));

        // Create grammar-based searches.
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(KEYWORD_CALLS, menuGrammar);

        // Phonetic search
        File freeFormModel = new File(assetsDir, "weather.dmp");
        recognizer.addNgramSearch(FREE_FORM, freeFormModel);

        /*
        for(String key: keyVocabulary.keySet())
        {
            // Create keyword-activation search.
            recognizer.addKeyphraseSearch(keyVocabulary.get(key),key);
        }
        */
    }

    public void switchSearch(String searchName) {
        recognizer.stop();
        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(WAKEUP_CALL))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);
    }

    public void pauseRecognizer()
    {
        recognizer.stop();
    }

    public void stopRecognizer()
    {
        if(recognizer!=null)
        {
            recognizer.shutdown();
        }
    }

}
