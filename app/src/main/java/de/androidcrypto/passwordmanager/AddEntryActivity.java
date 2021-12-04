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

public class AddEntryActivity extends AppCompatActivity implements ILockableActivity {

    // variables for our edit text, button, strings and dbhandler class.
    private EditText etEntryname, etLoginName, etLoginPassword, etCategory;
    private CheckBox cbFavourite;
    private Button btnAddEntry;
    private DBHandler dbHandler;
    String entryName, entryLoginName, entryLoginPassword, entryCategory;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void lock() {
        etLoginPassword.setVisibility(View.GONE);
        etLoginName.setVisibility(View.GONE);
    }

    @Override
    public void unlock() {
        etLoginPassword.setVisibility(View.VISIBLE);
        etLoginName.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // initializing all our variables.
        etEntryname = findViewById(R.id.idEtEntryName);
        etLoginName = findViewById(R.id.idEtLoginName);
        etLoginPassword = findViewById(R.id.idEtLoginPassword);
        etCategory = findViewById(R.id.idEtCategory);
        cbFavourite = findViewById(R.id.idCbFavourite);
        btnAddEntry = findViewById(R.id.idBtAddEntry);

        // on below line we are initialing our dbhandler class.
        dbHandler = new DBHandler(AddEntryActivity.this);

        // enable on create
        etLoginPassword.setVisibility(View.VISIBLE);
        etLoginName.setVisibility(View.VISIBLE);

        // on below lines we are getting data which
        // we passed in our adapter class.
        entryName = getIntent().getStringExtra("entryName");
        entryLoginName = getIntent().getStringExtra("entryLoginName");
        entryLoginPassword = getIntent().getStringExtra("entryLoginPassword");
        entryCategory = getIntent().getStringExtra("entryCategory");

        btnAddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean entryCheck = true; // everything is ok
                // check length of fields
                if (etEntryname.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Der Name des Eintrages ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(AddEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }
                if (etLoginName.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Der Login Name ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(AddEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }
                if (etLoginPassword.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Das Passwort ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(AddEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }
                if (etCategory.length() < 1) {
                    Snackbar snackbar = Snackbar.make(v, "Die Kategory ist zu kurz", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(AddEntryActivity.this, R.color.orange));
                    snackbar.show();
                    entryCheck = false;
                }

                if (entryCheck) {
                    // get status cbFavourite
                    String favourite = "0";
                    if (cbFavourite.isChecked()) favourite = "1";
                    dbHandler.addNewEntry(etEntryname.getText().toString(), etLoginName.getText().toString(), etLoginPassword.getText().toString(), etCategory.getText().toString(), favourite);
                    Toast.makeText(AddEntryActivity.this, "Eintrag hinzugefügt..", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(AddEntryActivity.this, MainActivity.class);
                    startActivity(i);
                }
            }
        });
    }
}
