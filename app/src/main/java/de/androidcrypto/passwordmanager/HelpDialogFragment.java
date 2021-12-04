package de.androidcrypto.passwordmanager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class HelpDialogFragment extends DialogFragment {

    public static HelpDialogFragment newInstance() {
        return new HelpDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        WebView view = (WebView) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_help, null);
        view.loadUrl("file:///android_asset/help.html");
        return new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle(getString(R.string.action_help))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

}


