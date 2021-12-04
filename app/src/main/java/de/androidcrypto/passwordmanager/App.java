package de.androidcrypto.passwordmanager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

public class App extends Application implements Application.ActivityLifecycleCallbacks {
    private static App instance;
    private int countStarted = 0;
    //private BiometricPrompt prompt;
    //private BiometricPrompt.PromptInfo promptInfo;
    private boolean isLocked = true;

    // sdk version checks, correct values added later in checkSdkVersion
    private int sdkVersion = 20;
    private boolean sdkIsMin23Max29 = false; // sdk version is in the range 23-29 [VERSION_CODES.M-Q]
    private boolean sdkIsMin30 = false; // sdk version is in the range 30+ [VERSION_CODES.R]
    private static final int BIOMETRIC_STRONG = BiometricManager.Authenticators.BIOMETRIC_STRONG;
    private static final int BIOMETRIC_WEAK = BiometricManager.Authenticators.BIOMETRIC_WEAK; // not used
    private static final int DEVICE_CREDENTIAL = BiometricManager.Authenticators.DEVICE_CREDENTIAL;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks(this);
        System.out.println("AP onCreate");
    }

    public static Context appContext() {
        return instance.getApplicationContext();
    }

    public static boolean hasBiometricProtection() {
        return BiometricManager.from(appContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    void onAppForeground(Activity activity) {
        if (hasBiometricProtection() && activity instanceof FragmentActivity)
            showBiometricPrompt((FragmentActivity) activity);
        else {
            isLocked = false;
            unlock(activity);
        }
    }

    void lock(Activity activity) {
        if (activity instanceof ILockableActivity)
            ((ILockableActivity) activity).lock();
    }

    void unlock(Activity activity) {
        if (activity instanceof ILockableActivity)
            ((ILockableActivity) activity).unlock();
    }

    void onAppBackground(Activity activity) {
        isLocked = true;
        System.out.println("AP onAppBackground");
        if (activity instanceof ILockableActivity)
            ((ILockableActivity) activity).lock();
    }

    public static void showBiometricPrompt(FragmentActivity activity) {
        //Log.d("SecretDiary", "show biometric prompt");
        if (activity instanceof ILockableActivity)
            ((ILockableActivity) activity).lock();
        BiometricPrompt prompt = new BiometricPrompt(activity, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                App.instance.isLocked = false;
                App.instance.unlock(activity);
                System.out.println("AP onActivitySucceeded: ");
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                //Log.e(getClass().getSimpleName(), " authentication failed " + errorCode + " " + errString);
                activity.finish();
            }
        });
        //int authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_WEAK;
        int authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_STRONG;
        //BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle(activity.g etString(R.string.biometricAuthentificationRequired)).setAllowedAuthenticators(authenticators).build();
        //BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("biometric Authentification Required").setAllowedAuthenticators(authenticators).build();
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle("Zugang zur App nur mit\nFingerprint oder Geräte PIN").setAllowedAuthenticators(authenticators).build();
        prompt.authenticate(promptInfo);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        System.out.println("AP onActivityStarted countStarted: " + countStarted);
        if (countStarted == 0)
            isLocked = true; // show biometric prompt on resume
        countStarted++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        System.out.println("AP onActivityStarted countStopped: " + countStarted);
        countStarted--;
        System.out.println("AP onActivityStarted countStopped: " + countStarted);
        if (countStarted == 0) {
            // App geht in den Hintergrund
            //Log.d(getClass().getSimpleName(), "App goes into background");
            onAppBackground(activity);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        System.out.println("AP onActivityResumed countStopped: " + countStarted);
        if (isLocked) {
            // App geht in den Vordergrund
            //Log.d(getClass().getSimpleName(), "App comes into foreground");
            onAppForeground(activity); // will show biometric prompt and unlock
        } else {
            unlock(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        System.out.println("AP onActivityPaused countStopped: " + countStarted);
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

}
