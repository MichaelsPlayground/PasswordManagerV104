package de.androidcrypto.passwordmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.security.keystore.KeyGenParameterSpec;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.os.CancellationSignal;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.concurrent.Executor;


public class MainActivity extends AppCompatActivity {

    // service infos
    // https://github.com/IvBaranov/MaterialFavoriteButton
    // https://stackoverflow.com/questions/35866370/implementing-add-to-favourite-mechanism-in-recyclerview

    // pbkdf2withhmac256 ist erst ab SDK 26 verfügbar, daher setting min sdk 26

    // add in gradle.build (module)
    // implementation "androidx.security:security-crypto:1.0.0"
    // https://developer.android.com/topic/security/data

    // stores data in an encrypted file
    String mainKeyAlias; // for the masterKey
    String encryptedPreferencesFilename = "encryptedpreferences.dat";

    FloatingActionButton btnAddEntry;
    // service buttons
    Button btnCountEntries;

    private DBHandler dbHandler;

    // recycler view
    private ArrayList<EntryModel> entryModelArrayList;
    private EntryRVAdapter entryRVAdapter;
    private RecyclerView entriesRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        // init the crypto part
        boolean initSuccess = Cryptography.cryptographicInit(getApplicationContext());
        if (!initSuccess) {
            errorAndQuitAlert("Die sichere Datenspeicherung konnte nicht eingerichtet werden. Die Nutzung der App ist leider nicht möglich.");
        }

        // enable haptic feedback = vibrate
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

        // creating a new dbhandler class
        // and passing our context to it.
        dbHandler = new DBHandler(MainActivity.this);
        entryModelArrayList = new ArrayList<>();

        // list from db handler class.
        entryModelArrayList = dbHandler.readEntries();

        // here we are filtering in entryName
        String filterString = "";
        ArrayList<EntryModel> entryModelFilteredArrayList = new ArrayList<>();
        for (int l = 0; l < entryModelArrayList.size(); l++) {
            String serviceName = entryModelArrayList.get(l).getEntryName().toLowerCase();
            if (serviceName.contains(filterString.toLowerCase())) {
                entryModelFilteredArrayList.add(entryModelArrayList.get(l));
            }
        }
        entryRVAdapter = new EntryRVAdapter(entryModelFilteredArrayList, de.androidcrypto.passwordmanager.MainActivity.this);
        entriesRV = findViewById(R.id.idRVEntries);

        // setting layout manager for our recycler view.
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(de.androidcrypto.passwordmanager.MainActivity.this, RecyclerView.VERTICAL, false);
        entriesRV.setLayoutManager(linearLayoutManager);

        // setting our adapter to recycler view.
        entriesRV.setAdapter(entryRVAdapter);

        btnAddEntry = (FloatingActionButton) findViewById(R.id.fabAddEntry);
        btnAddEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AddEntryActivity.class);
                startActivity(i);
            }
        });

/* manifests.xml org without fingerprint authentication
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
 */


/*
        // service buttons
        btnCountEntries = (Button) findViewById(R.id.idBtnCountEntries);
        btnCountEntries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int countEntries = dbHandler.readEntries().size();
                System.out.println("countEntries: " + countEntries);
            }
        });
/*
        btnCountEntries.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                v.setHapticFeedbackEnabled(true);
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                System.out.println("countEntries long clicked");
                return false;
            }
        });
*/

/*
        btnCountEntries = (Button) findViewById(R.id.idBtnCountEntries);
        btnCountEntries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

 */

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        MenuItem mSearch = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) mSearch.getActionView();
        mSearchView.setQueryHint("suche einen Eintrag");
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //adapter.getFilter().filter(newText);
                // hier wird gefiltered
                String filterString = newText; // in courseDuration
                ArrayList<EntryModel> entryModelFilteredArrayList = new ArrayList<>();
                for (int l = 0; l < entryModelArrayList.size(); l++) {
                    String serviceName = entryModelArrayList.get(l).getEntryName().toLowerCase();

                    if (serviceName.contains(filterString.toLowerCase())) {
                        entryModelFilteredArrayList.add(entryModelArrayList.get(l));
                    }

                    // original: courseRVAdapter = new CourseRVAdapter(courseModalArrayList, de.androidcrypto.sqllitetutorial1.ViewFilteredCourses.this);
                    entryRVAdapter = new EntryRVAdapter(entryModelFilteredArrayList, de.androidcrypto.passwordmanager.MainActivity.this);

                    entriesRV = findViewById(R.id.idRVEntries);

                    // setting layout manager for our recycler view.
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(de.androidcrypto.passwordmanager.MainActivity.this, RecyclerView.VERTICAL, false);
                    entriesRV.setLayoutManager(linearLayoutManager);

                    // setting our adapter to recycler view.
                    entriesRV.setAdapter(entryRVAdapter);

                }
                onPrepareOptionsMenu(menu); // zeigt die app-bar wieder vollständig an
                return true;

            }
        });
        MenuItem mAdd = menu.findItem(R.id.action_add);
        mAdd.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, AddEntryActivity.class);
                startActivity(i);
                return false;
            }
        });
        MenuItem mQuit = menu.findItem(R.id.action_quit);
        mQuit.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // todo clear masterkey ?
                System.out.println("*** quit clicked ***");
                finishAndRemoveTask(); // stops the app
                finishAffinity();
                return false;
            }
        });
        MenuItem mImportClipboard = menu.findItem(R.id.action_importClipboard);
        mImportClipboard.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, DatabaseImportClipboard.class);
                startActivity(i);
                return false;
            }
        });
        MenuItem mImportFile = menu.findItem(R.id.action_importFile);
        mImportFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, DatabaseImportFile.class);
                startActivity(i);
                return false;
            }
        });
        MenuItem mExportMasterkey = menu.findItem(R.id.action_exportMasterkey);
        mExportMasterkey.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, MasterkeyExportClipboard.class);
                startActivity(i);
                return false;
            }
        });
        MenuItem mHelpInformation = menu.findItem(R.id.action_help);
        mHelpInformation.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                displayHelpAlertDialog();
                return false;
            }
        });
        MenuItem mLicenseInformation = menu.findItem(R.id.action_licenseInformation);
        mLicenseInformation.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                displayLicensesAlertDialog();
                return false;
            }
        });
        MenuItem mAbout = menu.findItem(R.id.action_about);
        mAbout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(i);
                return false;
            }
        });

        MenuItem mDeleteAllEntries = menu.findItem(R.id.action_deleteAllEntries);
        mDeleteAllEntries.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Datenbank löschen");
                String message = "\nEs werden alle Einträge gelöscht.\n\nDrücken Sie auf LÖSCHEN, um alle\nEinträge endgültig zu löschen.";
                alertDialog.setMessage(message);
                RelativeLayout container = new RelativeLayout(MainActivity.this);
                RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                container.setLayoutParams(rlParams);
                alertDialog.setView(container);
                alertDialog.setPositiveButton("löschen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHandler.deleteAllEntries();
                        Intent i = new Intent(MainActivity.this, MainActivity.class);
                        startActivity(i);
                    }
                });
                alertDialog.setNegativeButton("abbrechen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
                alertDialog.show();
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    // error dialog
    private void errorAndQuitAlert(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Fehler");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finishAndRemoveTask(); // stops the app
                    }
                });
        // to avoid the back button usage
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                finishAndRemoveTask(); // stops the app
                finishAffinity();
            }
        });
        alertDialog.show();
    }

    // run: displayLicensesAlertDialog();
    // display licenses dialog see: https://bignerdranch.com/blog/open-source-licenses-and-android/
    private void displayLicensesAlertDialog() {
        WebView view = (WebView) LayoutInflater.from(this).inflate(R.layout.dialog_licenses, null);
        view.loadUrl("file:///android_asset/open_source_licenses.html");
        android.app.AlertDialog mAlertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();
        mAlertDialog = new android.app.AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle(getString(R.string.action_licenses))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void displayLicensesDialogFragment() {
        LicensesDialogFragment dialog = LicensesDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "LicensesDialog");
    }

    // help
    private void displayHelpAlertDialog() {
        WebView view = (WebView) LayoutInflater.from(this).inflate(R.layout.dialog_help, null);
        view.loadUrl("file:///android_asset/help.html");
        android.app.AlertDialog mAlertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();
        mAlertDialog = new android.app.AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle(getString(R.string.action_help))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void displayHelpDialogFragment() {
        HelpDialogFragment dialog = HelpDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "HelpDialog");
    }
}