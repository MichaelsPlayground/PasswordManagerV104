package de.androidcrypto.passwordmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.Executor;

public class AuthenticateActivity extends AppCompatActivity {

    /**
     * Manages a biometric prompt, and allows to perform an authentication operation
     */
    private BiometricPrompt biometricPrompt;
    private Executor executor;
    BiometricCodeConversions biometricCodeConversions = new BiometricCodeConversions();
    // sdk version checks, correct values added later in checkSdkVersion
    private int sdkVersion = 20;
    private boolean sdkIsMin23Max29 = false; // sdk version is in the range 23-29 [VERSION_CODES.M-Q]
    private boolean sdkIsMin30 = false; // sdk version is in the range 30+ [VERSION_CODES.R]
    private static final int BIOMETRIC_STRONG = BiometricManager.Authenticators.BIOMETRIC_STRONG;
    private static final int BIOMETRIC_WEAK = BiometricManager.Authenticators.BIOMETRIC_WEAK; // not used
    private static final int DEVICE_CREDENTIAL = BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticate);

        // this activity handles the biometric authentication
        // biometric prompt
        executor = ContextCompat.getMainExecutor(this);

        // the biometric prompt handles the callbacks
        biometricPrompt = new BiometricPrompt(AuthenticateActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                String errorString = biometricCodeConversions.getBiometricError(errorCode);
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                int authorizationType = result.getAuthenticationType();
                //System.out.println("Authentication succeeded with authType " + authorizationType);
                //System.out.println("(authType: 1=PIN, 2=fingerprint)");
                String type = biometricCodeConversions.getAuthenticationType(result.getAuthenticationType());
                //System.out.println("type: " + type);
                Intent i = new Intent(AuthenticateActivity.this, MainActivity.class);
                startActivity(i);
                finish(); // no way back to the authenticate activity
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                System.out.println("Authentication failed - Biometric is valid but not recognized");
            }
        });

        checkSdkVersion();
        boolean biometricReadyToUse = checkBiometric();
        if (biometricReadyToUse) {
            runAppAuthentication();
        } else {
            String message = "Auf diesem Gerät wurde noch keine\nSperrbildschirm-PIN und/oder kein\nFingerprint registriert.\n\nDie App kann nicht gestartet werden.";
            alertView(message);
        }

    }
    private void runAppAuthentication() {
        //BiometricPrompt.PromptInfo promptInfo = buildPromptInfoAuthenticateWithoutCrypto();
        BiometricPrompt.PromptInfo promptInfo = buildPromptInfoAuthenticateEncrypt();
        //System.out.println("promptInfo:\n " + promptInfo.toString());
        if (promptInfo != null) {
            biometricPrompt.authenticate(promptInfo);
        }
    }

    /* AUTHENTICATE + ENCRYPTION */
    @SuppressWarnings("deprecation") // on builder.setDeviceCredentialAllowed
    private BiometricPrompt.PromptInfo buildPromptInfoAuthenticateEncrypt() {
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder();
        //builder.setTitle("Zugang zur App nur mit\nFingerprint oder Geräte PIN").setSubtitle("encryption or decryption").setDescription("Description comes later...");
        builder.setTitle("Zugang zur App nur mit\nFingerprint oder Geräte PIN");
        builder.setConfirmationRequired(true);
        // api 30+
        if (sdkIsMin30) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG |
                    DEVICE_CREDENTIAL);
        }
        // api 23-29
        if (sdkIsMin23Max29) {
            builder.setDeviceCredentialAllowed(true);
        }
        try {
            return builder.build();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    // sdk checks
    private void checkSdkVersion() {
        sdkVersion = Build.VERSION.SDK_INT;
        // check for SDK 30+
        if (sdkVersion >= Build.VERSION_CODES.R) {
            sdkIsMin30 = true;
        }
        // check for SDK in range 23-29
        if (sdkVersion < Build.VERSION_CODES.R &
                sdkVersion >= Build.VERSION_CODES.M) {
            sdkIsMin23Max29 = true;
        }
    }

    // check if Biometric is ready to use
    private boolean checkBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int success = biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
        if (success == BiometricManager.BIOMETRIC_SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    // error dialog
    private void alertView(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(AuthenticateActivity.this).create();
        alertDialog.setTitle("Fehler");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}