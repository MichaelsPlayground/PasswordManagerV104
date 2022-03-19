package de.androidcrypto.passwordmanager;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class DatabaseExportFile extends AppCompatActivity {
    // ## version 1.02b ##
    Button btnExportDatabase, btnSaveDatabaseToFile;
    int minimumPassphraseLength = 4; // todo password length

    Context contextSave; // wird für read a file from uri benötigt
    String fileContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_export_file);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        btnExportDatabase = (Button) findViewById(R.id.btnExportDatabaseToFile);
        btnExportDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the passphrase
                EditText etPassphrase = (EditText) findViewById(R.id.etPassphraseDatabaseExport);
                int passphraseLength = 0;
                if (etPassphrase != null) {
                    passphraseLength = etPassphrase.length();
                }
                //System.out.println("passphrase length: " + passphraseLength);
                // todo check for minimum length
                // get the passphrase as char[]
                char[] passphrase = new char[passphraseLength];
                etPassphrase.getText().getChars(0, passphraseLength, passphrase, 0);

                EditText etData = (EditText) findViewById(R.id.etDatabaseContentExport);

                // get the data from database
                // the vars are only available in this function
                ArrayList<EntryModel> entryModelArrayList;
                DBHandler dbHandler;
                dbHandler = new DBHandler(DatabaseExportFile.this);
                // list from db handler class.
                entryModelArrayList = dbHandler.readEntries();

                // here we are generating the export data
                String unencryptedExportData = "";
                for (int l = 0; l < entryModelArrayList.size(); l++) {
                    String exportLine = entryModelArrayList.get(l).getEntryName() + "##" +
                            entryModelArrayList.get(l).getEntryLoginName() + "##" +
                            // note this data (loginPassword) is encrypted
                            //entryModelArrayList.get(l).getEntryLoginPassword() + "##" +
                            Cryptography.decryptStringAesGcmFromBase64(entryModelArrayList.get(l).getEntryLoginPassword()) + "##" +
                            entryModelArrayList.get(l).getEntryCategory() + "##" +
                            entryModelArrayList.get(l).getEntryFavourite();
                    unencryptedExportData = unencryptedExportData + exportLine + "\n";
                }
                System.out.println("number of exported items: " + entryModelArrayList.size());
                //etData.setText(unencryptedExportData);
                String encryptedFileContent = Cryptography.encryptDatabaseAesGcmToBase64(passphrase, unencryptedExportData);
                etData.setText(encryptedFileContent);
                fileContent = encryptedFileContent;
            }
        });

        btnSaveDatabaseToFile = (Button) findViewById(R.id.btnSaveDatabaseToFile);
        btnSaveDatabaseToFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.etDatabaseContentExport);
                String data = et.getText().toString();
                // copy to clipboard
                // Gets a handle to the clipboard service.
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", data);
                // Set the clipboard's primary clip.
                clipboard.setPrimaryClip(clip);
                Toast.makeText(DatabaseExportFile.this, "Database ins Clipboard kopiert..", Toast.LENGTH_SHORT).show();

                contextSave = v.getContext();
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                //boolean pickerInitialUri = false;
                //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
                intent.putExtra(Intent.EXTRA_TITLE, "databaseExport.txt");
                // deprecated startActivityForResult(intent, PICK_TXT_FILE);
                fileSaverActivityResultLauncher.launch(intent);


            }
        });


    }

    ActivityResultLauncher<Intent> fileSaverActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri uri = null;
                        if (resultData != null) {
                            uri = resultData.getData();
                            // Perform operations on the document using its URI.
                            try {
                                //String fileContent = et.getText().toString();
                                writeTextToUri(uri, fileContent);
                                // System.out.println("fileContent written: \n" + fileContent);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

    private void writeTextToUri(Uri uri, String data) throws IOException {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(contextSave.getContentResolver().openOutputStream(uri));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            System.out.println("Exception File write failed: " + e.toString());
        }
    }

}

/* 1234
### PWMANAGER IMPORT START V1 ###
6UxaFrzwa9HFQjKFkVER8rO7tyi8oL+J7G7TZf1Gga0=:usGMWKw53or2oCi/:cyanlpATwHrEJO1XgeBw/1VAxzfhbTnqFuhSkQmyyaO8PIIWqbWOxurIZyHdiEi5YsUHZh7EfKsIgQlZ6aTC4qOvDgIWaMX9eaR61wIyjtcACZkrJy/ncIgvnF6wzANMLY4WD49w0fQmbUSGFGxrOH7CnmVkxm8MqZfBRX2SloetwuDT+c+llruMwPmkv+LCkrDkEV3iHNiyKWiOmRQ3uOO51oxHhn8jqxysP9nAqMfIdtr2b1nLqvBEU36J0vbFlWMZxg3aeQv5LxJrKuvIYytiCyaGa0Fq2UQ7uerLagM2iPesQuaRrm38oXGfXjnfj+RJJvAwSSMX/58M8/z997X9iIeCUAeM:IjNXIHH3+xryB+TycdOvbQ==
### PWMANAGER IMPORT END V1 ###
*/
/*
 */