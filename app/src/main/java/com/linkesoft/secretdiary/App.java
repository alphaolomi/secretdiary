package com.linkesoft.secretdiary;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

public class App extends Application implements Application.ActivityLifecycleCallbacks {
    private static App instance;
    private int countStarted = 0;
    //private BiometricPrompt prompt;
    //private BiometricPrompt.PromptInfo promptInfo;
    private boolean isLocked = true;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        registerActivityLifecycleCallbacks(this);
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
        if (activity instanceof ILockableActivity)
            ((ILockableActivity) activity).lock();
    }

    public static void showBiometricPrompt(FragmentActivity activity) {
        Log.d("SecretDiary", "show biometric prompt");
        if (activity instanceof ILockableActivity)
            ((ILockableActivity) activity).lock();
        BiometricPrompt prompt = new BiometricPrompt(activity, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                App.instance.isLocked = false;
                App.instance.unlock(activity);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Log.e(getClass().getSimpleName(), " authentication failed " + errorCode + " " + errString);
                activity.finish();
            }
        });
        int authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_WEAK;
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder().setTitle(activity.getString(R.string.biometricAuthentificationRequired)).setAllowedAuthenticators(authenticators).build();
        prompt.authenticate(promptInfo);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (countStarted == 0)
            isLocked = true; // show biometric prompt on resume
        countStarted++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        countStarted--;
        if (countStarted == 0) {
            // App geht in den Hintergrund
            Log.d(getClass().getSimpleName(), "App goes into background");
            onAppBackground(activity);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (isLocked) {
            // App geht in den Vordergrund
            Log.d(getClass().getSimpleName(), "App comes into foreground");
            onAppForeground(activity); // will show biometric prompt and unlock
        } else {
            unlock(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}
