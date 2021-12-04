package de.javacrypto.eudcc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    // https://github.com/DIGGSweden/dgc-java
    // https://search.maven.org/artifact/se.digg.dgc/dgc-create-validate
    // version 1.0.1: implementation 'se.digg.dgc:dgc-create-validate:1.0.1'
    // https://search.maven.org/artifact/se.digg.dgc/dgc-schema/1.0.1/jar
    // version 1.0.1: implementation 'se.digg.dgc:dgc-schema:1.0.1'
    // official testdata: https://github.com/eu-digital-green-certificates/dgc-testdata
    // schemata: https://github.com/eu-digital-green-certificates/ehn-dgc-schema
    // schemata latest: https://github.com/ehn-dcc-development/ehn-dcc-schema


    // for DscList
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    // implementation group: 'com.google.code.gson', name: 'gson', version: '2.8.7'

    // https://mvnrepository.com/artifact/com.googlecode.json-simple/json-simple
    // implementation group: 'com.googlecode.json-simple', name: 'json-simple', version: '1.1.1'


    // icons für buttons: https://fonts.google.com/icons?selected=Material+Icons:history

    // app icon als set erzeugen
    // https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html#foreground.type=clipart&foreground.clipart=android&foreground.space.trim=1&foreground.space.pad=0.25&foreColor=rgba(96%2C%20125%2C%20139%2C%200)&backColor=rgb(68%2C%20138%2C%20255)&crop=0&backgroundShape=square&effects=none&name=ic_launcher

    Intent scanQrcodeIntent, aboutIntent;
    // just for testing:
    Intent showQrcodeDetail;

    public static String filenameSelected = ""; // filled by ShowCertificateOverview
    public static String qrcodeToCheck = ""; // filled by verifyCertificate

    // app mode
    boolean productionModeEnabled = true;
    static boolean productionModeEnabledStatic = true;

    ConstantsClass constantsClass = new ConstantsClass();

    boolean dscFilesProdAvailable = false;
    boolean dscFilesTestAvailable = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanQrcodeIntent = new Intent(MainActivity.this, ScanQrcode.class);
        aboutIntent = new Intent(MainActivity.this, AboutActivity.class);

        // todo remove
        // just for testing:
        showQrcodeDetail = new Intent(MainActivity.this, ShowQrcodeDetail.class);

        // todo SnackBar nach Erststart rot, dann download über Button im Menü, dann sollte die
        // snackbar verschwinden und durch den neuen Check ersetzt werden -> passiert noch nicht !

        Button btnQrcodeScanning = findViewById(R.id.btnQrcodeScanning);
        btnQrcodeScanning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrcodeToCheck = "";
                checkDscFiles(v);
                productionModeEnabledStatic = productionModeEnabled;
                if (dscFilesProdAvailable & dscFilesTestAvailable) {
                    startActivity(scanQrcodeIntent);
                } else {
                    String message = "Bitte laden Sie zuerst\ndie Prüfdateien herunter\n(siehe unten DOWNLOAD)";
                    alertErrorDialog(message);
                }
            }
        });

        Button btnShowLicenseInformation = findViewById(R.id.btnShowLicense);
        btnShowLicenseInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLicensesAlertDialog();
            }
        });

        Button btnDownloadDscLists = findViewById(R.id.btnDownloadDscList);
        btnDownloadDscLists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo toggle prod test
                // its better to load both files
                downloadDscLists(productionModeEnabled);
                downloadDscLists(!productionModeEnabled);
                checkDscFiles(v);
                showSnackBarAgeOfDscFiles();
            }
        });

        Button btnAbout = findViewById(R.id.btnAboutApp);
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(aboutIntent);
            }
        });

        Button btnSetQrcode = findViewById(R.id.btnSetQrcode);
        btnSetQrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // code spain
                // https://github.com/eu-digital-green-certificates/dgc-testdata/blob/main/ES/2DCode/raw/2101.json
                String qrcSpainVac = "HC1:NCFOXN%TSMAHN-HKTGX94G-ICWEXWP769 5W3XH74M6R5J711MG*CM+W4967RXQQHIZC4.OI1RM8ZA*LP0W25$074M9-8YC8O.CP-823L*/GF/GD4T9-8+5TW$5X4PDOPYE9/MV+:T.A53XH395*CBVZ0K1H$$0VONZ 2ZJJ+.K4IJZJJBY4.Z8YLV0C58G13HHEGJDFHIE9MIHJ6W48UK.GA68$8D7AD:XIBEIVG395EV3EVCK09DEWCFVA1RO5VA81K0ECM8CXVDC8C90JK.A+ C/8DXEDKG0CGJ5ALR.4YMR$ZJ*DJWP42W5Z/6S:7Q+MX4CMBNXJUH.VI%K6ZN7BM1US4K5+WJDSH TU/UIGSUJLEH-V: B1-V-VF+4WOUKYZQ.J9 0OERGP%D+W31 FNSGUVPQRHIY1VS11O1QR3*V0Y/FCV7*7L-P9C94HHVCEE.ETSO9GFS5RAI-96HU PV22P6Z42*1E1BGDN51A6STH3S1CO488O:RV%EW07%$5*2OQ*VQ3EOWBYM0WHC24G";
                String qrcSpainRec = "HC1:NCFOXN%TSMAHN-HKTGX94G-ICWEXWPX9H9M9ESIARGHXK*+D$055B9TUDP0J-MPW$NLEEEJCY S2%KYZPF97.P8-6C2UODM8-K6NN9UNARK4I/2*I2WN8I/6S-2-N9:WO$*SVFKC K:YM8J4RK46YBBOA3O8C KUGAACQ*883DO4C5K11P4D%PDS1JGDBTTTYIJGDBQMIGZIY8MGOIM423DJTIJ/CJ5VA81K0ECM8CXVDC8C 1J5OI:N0XHQVE4L/5R3FMIA9.B-7A6/D1FA1MGNPTU.H8ZA9NTGB7-PV.4H8F5G.4$2TW-26ALW.IDS02$VU*0H R*2H5NV9Y4KCTSVCVLVUXN+47U*00GVA057*KB*KYQTHFT4S8$ZG:SU5JUZDFSM7HMPF8V0FWM2A:EDE$KXYK/RRD1G79L7$OWLGY-QUGLESDN0UZCCR657673OVW-M7AUV7UIXRQU1O RHBO%6SU F%40IR485";
                qrcodeToCheck = qrcSpainRec;
                filenameSelected = "";
                startActivity(showQrcodeDetail);
            }
        });

        Button btnCheckDscFiles = findViewById(R.id.btnCheckDscFiles);
        btnCheckDscFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkDscFiles(v);
            }
        });

        ToggleButton toggleButtonProdMode = (ToggleButton) findViewById(R.id.btnToggleProdMode);
        toggleButtonProdMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    productionModeEnabled = true;
                    productionModeEnabledStatic = true;
                    btnQrcodeScanning.setText("Scan QR-Code");
                    btnQrcodeScanning.getBackground().setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.blue), PorterDuff.Mode.SRC);
                    showSnackBarAgeOfDscFiles();
                } else {
                    // The toggle is disabled
                    productionModeEnabled = false;
                    productionModeEnabledStatic = false;
                    btnQrcodeScanning.setText("Scan Test QR-Code");
                    btnQrcodeScanning.getBackground().setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.orange), PorterDuff.Mode.SRC);
                    showSnackBarAgeOfDscFiles();
                }
            }
        });

        showSnackBarAgeOfDscFiles();
    }

    private void showSnackBarAgeOfDscFiles() {
        // show snackbar permanently
        // color depending on age of dsc files
        int ageDscFiles = getAgeOfDscFiles(productionModeEnabled);
        String snackBarText = "Alter der Prüfzertifikate: " + ageDscFiles + " Tage";
        if (ageDscFiles < constantsClass.getDslListAgeOrangeWarningDays()) {
            showSnackBar(snackBarText, R.color.blue);
        } else {
            if (ageDscFiles < constantsClass.getDslListAgeRedWarningDays()) {
                showSnackBar(snackBarText, R.color.orange);
            } else {
                showSnackBar(snackBarText, R.color.red);
            }
        }
        if (ageDscFiles < 0) {
            showSnackBar(snackBarText, R.color.red);
        }
    }

    private void showSnackBar(String text, int color) {
        TextView contextView = (TextView) findViewById(R.id.btnDownloadDscList);
        Snackbar snackbar = Snackbar
                .make(contextView, text, Snackbar.LENGTH_INDEFINITE)
                .setTextColor(getResources().getColor(R.color.black))
                .setActionTextColor(getResources().getColor(R.color.black))
                .setBackgroundTint(ContextCompat.getColor(MainActivity.this, color))
                .setAction("DOWNLOAD", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Button btnDownloadDscLists = (Button) findViewById(R.id.btnDownloadDscList);
                        btnDownloadDscLists.performClick();
                    }
                });
        snackbar.show();
    }

    private void checkDscFiles(View v) {
        dscFilesProdAvailable = checkDslFilesAvailability(true);
        dscFilesTestAvailable = checkDslFilesAvailability(false);
    }

    private int getAgeOfDscFiles(boolean prodMode) {
        // open information file, get storage timestamp and calculate difference to actual date
        // return age in days, -1 if information file does not exist
        File dir = getFilesDir();
        File file;
        String downloadTimestampString;
        if (prodMode) {
            file = new File(dir, constantsClass.getFileNameDownloadTimestampProd());
            if (!file.exists()) return -1; // file not existing
             downloadTimestampString = loadFileFromInternalStorage(constantsClass.getFileNameDownloadTimestampProd());
        } else {
            file = new File(dir, constantsClass.getFileNameDownloadTimestampTest());
            if (!file.exists()) return -1; // file not existing
            downloadTimestampString = loadFileFromInternalStorage(constantsClass.getFileNameDownloadTimestampTest());
        }
        long downloadTimestamp = Long.parseLong(downloadTimestampString);
        Timestamp timestampDownload = new Timestamp(downloadTimestamp);
        Timestamp timestampActual = new Timestamp(System.currentTimeMillis());
        long timestampDifference = getDateDiff(timestampDownload, timestampActual, TimeUnit.DAYS);
        return (int) timestampDifference;
    }

    /**
     * Get a diff between two dates
     * @param date1 the oldest date
     * @param date2 the newest date
     * @param timeUnit the unit in which you want the diff
     * @return the diff value, in the provided unit
     */
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    private boolean checkDslFilesAvailability(boolean checkProdFiles) {
        File dir = getFilesDir();
        if (checkProdFiles) {
            // check for production files
            File file = new File(dir, constantsClass.getFileNameDscListProd());
            boolean fileDscListAvailable = file.exists();
            file = new File(dir, constantsClass.getFileNamePublicKeyProd());
            boolean filePublicKeyAvailable = file.exists();
            return (fileDscListAvailable & filePublicKeyAvailable);
        } else {
            // check for test files
            File file = new File(dir, constantsClass.getFileNameDscListTest());
            boolean fileDscListAvailable = file.exists();
            file = new File(dir, constantsClass.getFileNamePublicKeyTest());
            boolean filePublicKeyAvailable = file.exists();
            return (fileDscListAvailable & filePublicKeyAvailable);
        }
    }

    private void downloadDscLists(boolean prodMode) {
        // File url to download
        if (prodMode) {
            new DownloadFileFromURL().execute(constantsClass.getUrlDscListProd());
            new DownloadFileFromURL().execute(constantsClass.getUrlPublicKeyProd());
        } else {
            new DownloadFileFromURL().execute(constantsClass.getUrlDscListTest());
            new DownloadFileFromURL().execute(constantsClass.getUrlPublicKeyTest());
        }

        // todo check for existence
        
        //String fileName = "downloadtime.txt";
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestampString = String.valueOf(timestamp.getTime());
        ByteArrayInputStream input = new ByteArrayInputStream(timestampString.getBytes(StandardCharsets.UTF_8));
        FileOutputStream output = null;
        try {
            if (prodMode) {
                output = openFileOutput(constantsClass.getFileNameDownloadTimestampProd(), MODE_PRIVATE);
            } else {
                output = openFileOutput(constantsClass.getFileNameDownloadTimestampTest(), MODE_PRIVATE);
            }
            int DEFAULT_BUFFER_SIZE = 1024;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            n = input.read(buffer, 0, DEFAULT_BUFFER_SIZE);
            while (n >= 0) {
                output.write(buffer, 0, n);
                n = input.read(buffer, 0, DEFAULT_BUFFER_SIZE);
            }
            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadFileFromInternalStorage(String filename) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = openFileInput(filename))
        {
            byte[] buffer = new byte[8192];
            int nread;
            while ((nread = in.read(buffer)) > 0) {
                out.write(buffer, 0, nread);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            return "";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(out.toByteArray());
    }

    private static String base64Encoding(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP);
    }

    private static byte[] base64Decoding(String input) {
        return Base64.decode(input, Base64.NO_WRAP);
    }

    // download dscList- and publicKey files to internal storage
    class DownloadFileFromURL extends AsyncTask<String, String, String> {
        ProgressDialog pd;
        String pathFolder = "";
        String pathFile = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setTitle("Processing...");
            pd.setMessage("Please wait.");
            pd.setMax(100);
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.setCancelable(true);
            pd.show();
        }

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                // get fileName from f_url
                String fileName = "test.txt"; // default
                String filenameUrl = f_url[0];

                // I'm using if instead of switch to avoid error "Constant expression required"
                if (filenameUrl.equals(constantsClass.getUrlDscListProd())) fileName = constantsClass.getFileNameDscListProd();
                if (filenameUrl.equals(constantsClass.getUrlDscListTest())) fileName = constantsClass.getFileNameDscListTest();
                if (filenameUrl.equals(constantsClass.getUrlPublicKeyProd())) fileName = constantsClass.getFileNamePublicKeyProd();
                if (filenameUrl.equals(constantsClass.getUrlPublicKeyTest())) fileName = constantsClass.getFileNamePublicKeyTest();

                URLConnection connection = url.openConnection();
                connection.connect();

                // this will be useful so that you can show a typical 0-100 %
                // progress bar
                int lengthOfFile = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream());
                FileOutputStream output = openFileOutput(fileName, MODE_PRIVATE);
                byte[] data = new byte[1024]; //anybody know what 1024 means ?
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));
                    // writing data to file
                    output.write(data, 0, count);
                }
                // flushing output
                output.flush();
                // closing streams
                output.close();
                input.close();
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return pathFile;
        }

        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pd.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {
            if (pd!=null) {
                pd.dismiss();
            }
        }
    }

    // convertPlainToDer and convertDerToPlain
    // source https://stackoverflow.com/a/61873962/8166854  answered May 18 at 16:07 dave_thompson_085
    // secp384r1 (aka P-384) has 384-bit order so use 384/8 which is 48 for n
    private static String convertSignatureP1363ToDerBase64 (String plainBase64) {
        byte[] plain = base64Decoding(plainBase64);
        int n = 32; // for example assume 256-bit-order curve like P-256
        BigInteger r = new BigInteger (+1, Arrays.copyOfRange(plain,0,n));
        BigInteger s = new BigInteger (+1, Arrays.copyOfRange(plain,n,n*2));
        byte[] x1 = r.toByteArray(), x2 = s.toByteArray();
        // already trimmed two's complement, as DER wants
        int len = x1.length + x2.length + (2+2), idx = len>=128? 3: 2;
        // the len>=128 case can only occur for curves of 488 bits or more,
        // and can be removed if you will definitely not use such curve(s)
        byte[] out = new byte[idx+len];
        out[0] = 0x30;
        if( idx==3 ){
            out[1] = (byte)0x81;
            out[2] = (byte)len; }
        else {
            out[1] = (byte)len; }
        out[idx] = 2;
        out[idx+1] = (byte)x1.length;
        System.arraycopy(x1, 0, out, idx+2, x1.length);
        idx += x1.length + 2;
        out[idx] = 2;
        out[idx+1] = (byte)x2.length;
        System.arraycopy(x2, 0, out, idx+2, x2.length);
        return base64Encoding(out);
    }

    // error dialog
    private void alertErrorDialog(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
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

    // display licenses dialog see: https://bignerdranch.com/blog/open-source-licenses-and-android/
    private void displayLicensesAlertDialog() {
        WebView view = (WebView) LayoutInflater.from(this).inflate(R.layout.dialog_licenses, null);
        view.loadUrl("file:///android_asset/open_source_licenses.html");
        AlertDialog mAlertDialog = new AlertDialog.Builder(MainActivity.this).create();
        mAlertDialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle(getString(R.string.action_licenses))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void displayLicensesDialogFragment() {
        LicensesDialogFragment dialog = LicensesDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "LicensesDialog");
    }
}
