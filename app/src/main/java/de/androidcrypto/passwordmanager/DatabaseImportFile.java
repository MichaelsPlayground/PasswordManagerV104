package de.androidcrypto.passwordmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class DatabaseImportFile extends AppCompatActivity {

    Context contextImportFile; // wird für read a file from uri benötigt

    Button btnImportDatabase, btnChooseImportFile;
    int minimumPassphraseLength = 4; // todo check password length
    DBHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_import_file);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        dbHandler = new DBHandler(DatabaseImportFile.this);

        btnImportDatabase = (Button) findViewById(R.id.btnImportDatabaseFromFile);
        btnImportDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the passphrase
                EditText etPassphrase = (EditText) findViewById(R.id.etPassphraseImport);
                int passphraseLength = 0;
                if (etPassphrase != null) {
                    passphraseLength = etPassphrase.length();
                }
                // todo check for minimum length
                // get the passphrase as char[]
                char[] passphrase = new char[passphraseLength];
                etPassphrase.getText().getChars(0, passphraseLength, passphrase, 0);
                // get the data from edittext and split into lines
                EditText etData = (EditText) findViewById(R.id.etDatabaseContentImport);
                String items = etData.getText().toString();
                String dataImport = Cryptography.decryptFileAesGcmFromBase64(passphrase, items);
                String[] dataImportLines = dataImport.split("\n");
                if (dataImportLines.length > 0) {
                    for (int i = 0; i < dataImportLines.length; i++) {
                        // split string into parts
                        String[] fields = dataImportLines[i].split("##");
                        // Kategorie##Beschreibung##Login Name##Login Passwort##Favorit
                        dbHandler.addNewEntry(fields[1], fields[2], fields[3], fields[0], fields[4]);
                    }
                }
                // return to main activity
                Intent i = new Intent(DatabaseImportFile.this, MainActivity.class);
                startActivity(i);
            }
        });

        btnChooseImportFile = (Button) findViewById(R.id.btnChooseImportFile);
        btnChooseImportFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                contextImportFile = btnChooseImportFile.getContext(); // Context context1;
                // wird für read a file from uri benötigt
                // https://developer.android.com/training/data-storage/shared/documents-files
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                boolean pickerInitialUri = false;
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
                fileChooserActivityResultLauncher.launch(intent);
            }
        });
    }

    ActivityResultLauncher<Intent> fileChooserActivityResultLauncher = registerForActivityResult(
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
                                String fileContent = readTextFromUri(uri);
                                EditText etData = (EditText) findViewById(R.id.etDatabaseContentImport);
                                etData.setText(fileContent);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });

    private String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        //try (InputStream inputStream = getContentResolver().openInputStream(uri);
        // achtung: context1 muss gefüllt sein !
        try (InputStream inputStream = contextImportFile.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
        }
        return stringBuilder.toString();
    }

}
/*
### PWMANAGER IMPORT START V1 ###
EE9bxAuMIX3aWMQIBJZfqq18tZ5wG9pRa7wPUcfzH98=:4NEFZB+xMuuAb7ce:eO75uuEv+AYCdn5M3VTcJnzb8cZdSEZ8A1c5gyImwECmcDhWHdt2d2maemf5rZj6JPUCrONAH5R7zGMzU0Gti3X1fKnrrajoGfn9vlDrx1QQjow/kwk1rLhJfX3a2L65WIjeVx6690X0gAZ5NFxImQunvgDl2F9ZzrSVEf+q7NwkqGVH8bVu07e4FtpjWTpIMlfKv53KvzoDTsrxAG2E2BwgrgBCpypoKx81/IHihbrBUnBjQtZ/kl71aFM8iF8zGQ==:GVAhBwFd+lvbOYYFZH3oIw==
### PWMANAGER IMPORT END V1 ###
*/