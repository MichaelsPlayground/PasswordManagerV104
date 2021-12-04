package de.androidcrypto.passwordmanager;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

public class BiometricCodeConversions {
    // from https://github.com/husaynhakeem/android-playground/blob/master/BiometricSample/app/src/main/java/com/husaynhakeem/biometricsample/biometric/BiometricCodeConversions.kt

    public String getBiometricAvailability(int availability) {
        switch (availability) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return "Biometric success";
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return "Biometric error - Hardware unavailable";
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return "Biometric error - No biometrics enrolled";
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return "Biometric error - No biometric hardware";
            default:
                return "Unknown biometric error - $availability";
        }
    }

    public String getBiometricError(int error) {
        switch (error) {
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
                return "The hardware is unavailable";
            case BiometricPrompt.ERROR_UNABLE_TO_PROCESS:
                return "The sensor is unable to process the current image";
            case BiometricPrompt.ERROR_TIMEOUT:
                return "The current request has been running too long";
            case BiometricPrompt.ERROR_NO_SPACE:
                return "Not enough storage to complete the operation";
            case BiometricPrompt.ERROR_CANCELED:
                return "The operation was cancelled due to the sensor being unavailable";
            case BiometricPrompt.ERROR_LOCKOUT:
                return "The operation was cancelled due to too many attempts";
            case BiometricPrompt.ERROR_VENDOR:
                return "Vendor specific error";
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                return "Permanent lockout until the user unlocks with strong authentication (PIN/pattern/Password)";
            case BiometricPrompt.ERROR_USER_CANCELED:
                return "The operation was cancelled by the user";
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
                return "The user doesn't have any biometrics enrolled";
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                return "The device does not have a biometric sensor";
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                return "The user pressed the negative button";
            case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
                return "The device does not have pin, pattern or password set up";
            default:
                return "";
        }
    }

    public String getAuthenticationType(int type) {
        switch (type) {
            case BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC:
                return "Biometric success";
            case BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL:
                return "Device credential";
            case BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN:
                return "Unknown";
            default:
                return "Unknown authentication type - $type";
        }
    }
}
