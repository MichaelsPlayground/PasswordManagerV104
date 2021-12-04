package de.androidcrypto.passwordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class UpdateEntryActivity extends AppCompatActivity implements ILockableActivity {

    // variables for our edit text, button, strings and dbhandler class.
    private EditText etEntryname, etLoginName, etLoginPassword, etCategory;
    private CheckBox cbFavourite;
    private Button btnUpdateEntry, btnDeleteEntry;
    private DBHandler dbHandler;
    String entryId, entryName, entryLoginName, entryLoginPassword, entryCategory, entryFavourite;

    @Override
    protected void onResume() {
        super.onResume();
        /*
        EditText et = findViewById(R.id.idEtLoginPassword);
        et.setVisibility(View.GONE);
        et = findViewById(R.id.idEtLoginName);
        et.setVisibility(View.GONE);

         */
    }

    @Override
    public void lock() {
        //EditText et = findViewById(R.id.idEtLoginPassword);
        etLoginPassword.setVisibility(View.GONE);
        //et = findViewById(R.id.idEtLoginName);
        etLoginName.setVisibility(View.GONE);
    }

    @Override
    public void unlock() {
        //EditText et = findViewById(R.id.idEtLoginPassword);
        etLoginPassword.setVisibility(View.VISIBLE);
        //et = findViewById(R.id.idEtLoginName);
        etLoginName.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_entry);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // initializing all our variables.
        etEntryname = findViewById(R.id.idEtEntryName);
        etLoginName = findViewById(R.id.idEtLoginName);
        etLoginPassword = findViewById(R.id.idEtLoginPassword);
        etCategory = findViewById(R.id.idEtCategory);
        cbFavourite = findViewById(R.id.idCbFavourite);
        btnUpdateEntry = findViewById(R.id.idBtUpdateEntry);
        btnDeleteEntry = findViewById(R.id.idBtDeleteEntry);

        // on below line we are initialing our dbhandler class.
        dbHandler = new DBHandler(UpdateEntryActivity.this);

        // enable on create
        etLoginPassword.setVisibility(View.VISIBLE);
        etLoginName.setVisibility(View.VISIBLE);

        // on below lines we are getting data which
        // we passed in our adapter class.
        entryId = getIntent().getStringExtra("entryId");
        entryName = getIntent().getStringExtra("entryName");
        entryLoginName = getIntent().getStringExtra("entryLoginName");
        entryLoginPassword = Cryptography.decryptStringAesGcmFromBase64(getIntent().getStringExtra("entryLoginPassword"));
        entryCategory = getIntent().getStringExtra("entryCategory");
        entryFavourite = getIntent().getStringExtra("entryFavourite");

        etEntryname.setText(entryName);
        etLoginName.setText(entryLoginName);
        etLoginPassword.setText(entryLoginPassword);
        etCategory.setText(entryCategory);
        if (entryFavourite.equals("1")) {
            cbFavourite.setChecked(true);
        } else {
            cbFavourite.setChecked(false);
        }

        btnUpdateEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean entryCheck = true; // everything is ok
                // check length of fields
                if (etEntryname.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Der Name des Eintrages ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(UpdateEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }
                if (etLoginName.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Der Login Name ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(UpdateEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }
                if (etLoginPassword.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Das Passwort ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(UpdateEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }
                if (etCategory.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Die Kategory ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(UpdateEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }
                if (entryCheck) {
                    // get status cbFavourite
                    String favourite = "0";
                    if (cbFavourite.isChecked()) favourite = "1";
                    dbHandler.updateEntry(entryId, etEntryname.getText().toString(), etLoginName.getText().toString(), etLoginPassword.getText().toString(), etCategory.getText().toString(), favourite);
                    Toast.makeText(UpdateEntryActivity.this, "Eintrag geändert..", Toast.LENGTH_SHORT).show();
                    // launching our main activity.
                    Intent i = new Intent(UpdateEntryActivity.this, MainActivity.class);
                    startActivity(i);
                }
            }
        });

        // adding on click listener to our update course button.
        btnDeleteEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHandler.deleteEntry(entryId);
                Toast.makeText(UpdateEntryActivity.this, "Eintrag gelöscht", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(UpdateEntryActivity.this, MainActivity.class);
                startActivity(i);
            }
        });

    }
}
