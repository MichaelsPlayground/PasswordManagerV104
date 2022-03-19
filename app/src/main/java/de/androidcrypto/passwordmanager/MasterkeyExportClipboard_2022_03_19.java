package de.androidcrypto.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MasterkeyExportClipboard_2022_03_19 extends AppCompatActivity {

    Button btnExportMasterkey, btnCopyMasterkeyToClipboard;
    int minimumPassphraseLength = 4; // todo password length

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masterkey_export_clipboard);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        btnExportMasterkey = (Button) findViewById(R.id.btnExportMasterkeyToClipboard);
        btnExportMasterkey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the passphrase
                EditText etPassphrase = (EditText) findViewById(R.id.etPassphraseMasterkeyExport);
                int passphraseLength = 0;
                if (etPassphrase != null) {
                    passphraseLength = etPassphrase.length();
                }
                //System.out.println("passphrase length: " + passphraseLength);
                // todo check for minimum length
                // get the passphrase as char[]
                char[] passphrase = new char[passphraseLength];
                etPassphrase.getText().getChars(0, passphraseLength, passphrase, 0);

                // get the data from edittext and split into lines
                EditText etData = (EditText) findViewById(R.id.etMasterkeyContentExport);
                etData.setText(Cryptography.encryptMasterkeyAesGcmToBase64(passphrase));

            }
        });

        btnCopyMasterkeyToClipboard = (Button) findViewById(R.id.btnCopyMasterkeyToClipboard);
        btnCopyMasterkeyToClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.etMasterkeyContentExport);
                String data = et.getText().toString();
                // copy to clipboard
                // Gets a handle to the clipboard service.
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", data);
                // Set the clipboard's primary clip.
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MasterkeyExportClipboard_2022_03_19.this, "Masterkey ins Clipboard kopiert..", Toast.LENGTH_SHORT).show();
                // back to main activity
                Intent i=new Intent(MasterkeyExportClipboard_2022_03_19.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
    }

}
/* passphrase 123456
### PWMANAGER MASTERKEY START V1 ###
w0/5AdcuFrNweAQ6+ryBxAxxL3wVRu1aCrxe1N+bV3I=:S0nMwdtEL12ruZZG:/v2vDifM9bMNVQeoKDehR2KuH6tD9lnzm7xN6m/9S3Y=:xU6YYSrqjzBqN7qdR/ifoQ==
### PWMANAGER MASTERKEY END V1 ###
*/
/*
*/
