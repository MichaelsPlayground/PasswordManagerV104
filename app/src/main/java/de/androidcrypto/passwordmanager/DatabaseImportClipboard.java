package de.androidcrypto.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.zip.InflaterOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class DatabaseImportClipboard extends AppCompatActivity {

    // constants
    String headerLine = "### PWMANAGER IMPORT START V1 ###";
    String footerLine = "### PWMANAGER IMPORT END V1 ###";
    String fieldDelimiter = "##";
    int pbkdf2Iterations = 15000; // for en-/decryption key derivation from password

    Button btnImportDatabase, btnCopyDatabaseFromClipboard;
    int minimumPassphraseLength = 4; // todo password length
    DBHandler dbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_import_clipboard);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        dbHandler = new DBHandler(DatabaseImportClipboard.this);

        btnImportDatabase = (Button) findViewById(R.id.btnImportDatabaseFromClipboard);
        btnImportDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the passphrase
                EditText etPassphrase = (EditText) findViewById(R.id.etPassphraseImport);
                int passphraseLength = 0;
                if (etPassphrase != null) {
                    passphraseLength = etPassphrase.length();
                }
                System.out.println("passphrase length: " + passphraseLength);
                // todo check for minimum length
                // get the passphrase as char[]
                char[] passphrase = new char[passphraseLength];
                etPassphrase.getText().getChars(0, passphraseLength, passphrase, 0);

                // get the data from edittext and split into lines
                EditText etData = (EditText) findViewById(R.id.etDatabaseContentImport);
                String items = etData.getText().toString();
                String[] lines = items.split("\n"); //split your String at every new line
                String line;
                int linesTotal = lines.length;
                // read header
                line = lines[0];
                if (line.equals(headerLine)) {
                    // proceed with decryption
                } else {
                    // todo remove prints or toast or SnackBar
                    System.out.println("ciphertext file is not for passwordmanager");
                    System.exit(1);
                }
                // read salt
                String saltLoadBase64 = lines[1];
                // try to Base64 decode
                byte[] saltLoad = new byte[0];
                try {
                    System.out.println("saltLoadBase64: " + saltLoadBase64);
                    saltLoad = base64Decoding(saltLoadBase64);
                } catch (Exception e) {
                    System.out.println("error in reading and decoding salt");
                    System.exit(1);
                }

                // key derivation
                SecretKeySpec secretKeySpecLoad = null;
                try {
                    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                    KeySpec keySpec = new PBEKeySpec(passphrase, saltLoad, pbkdf2Iterations, 32 * 8); // 128 - 192 - 256
                    byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
                    secretKeySpecLoad = new SecretKeySpec(key, "AES");
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                }
                // now read encrypted data lines
                int lineCounter = 2; // the data starts at line 2
                while (lineCounter < linesTotal) {
                    line = lines[lineCounter];
                    System.out.println("line: " + line);

                    // check for footer line
                    if (line.equals(footerLine)) {
                        // stop decryption
                        System.out.println("footerLine found");
                    } else {
                        // start decryption
                        String decryptedData;
                        byte[] dataByte = base64Decoding(line);
                        byte[] nonce = new byte[12];
                        Cipher cipher;
                        try {
                            cipher = Cipher.getInstance("AES/GCM/NOPadding");
                            ByteArrayInputStream bis = new ByteArrayInputStream(dataByte);
                            CipherInputStream cipherInputStream = new CipherInputStream(bis, cipher);
                            //OutputStream output = new FileOutputStream(outFileName);
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            OutputStream ios = new InflaterOutputStream(output);
                            bis.read(nonce);
                            // bis.read(salt);
                            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
                            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpecLoad, gcmParameterSpec);
                            // Transfer bytes from the input file to the output file
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = cipherInputStream.read(buffer)) > 0) {
                                ios.write(buffer, 0, length);
                            }
                            // Close the streams
                            output.flush();
                            output.close();
                            bis.close();
                            decryptedData = new String(output.toByteArray());
                            // todo remove prints
                            System.out.println("decryptedData: " + decryptedData);
                            // todo insert in database
                            // split string into parts
                            String[] fields = decryptedData.split("##");
                            // Kategorie##Beschreibung##Login Name##Login Passwort##Favorit
                            //System.out.println("number of fields: " + fields.length);
                            dbHandler.addNewEntry(fields[1], fields[2], fields[3], fields[0], fields[4]);
                            System.out.println("Enctry added: " + fields[1]);
                        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IOException e) {
                            System.out.println("error during decryption");
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                    lineCounter++;
                }
                // return to main activity
                Intent i = new Intent(DatabaseImportClipboard.this, MainActivity.class);
                startActivity(i);
            }
        });

        btnCopyDatabaseFromClipboard = (Button) findViewById(R.id.btnCopyDatabaseFromClipboard);
        btnCopyDatabaseFromClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.etDatabaseContentImport);
                // copy from clipboard
                // Gets a handle to the clipboard service.
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                // is there any data in the clipboard ?
                if (clipboard.hasPrimaryClip()) {
                    // is the data plaintext ?
                    if (clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                        // Examines the item on the clipboard. If getText() does not return null, the clip item contains the
                        // text. Assumes that this application can only handle one item at a time.
                        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                        // Gets the clipboard as text.
                        et.setText(item.getText());
                    }
                }
            }
        });
    }

    private static byte[] base64Decoding(String input) {
        return Base64.decode(input, Base64.NO_WRAP);
    }
}
/* passphrase 123456
### PWMANAGER IMPORT START V1 ###
eUx+HOaAl5aqpudG50sbKF9g5V8/0JubclPegLeXESs=
YWAxZxIPn+vkntXrzuLT5cL/tA6+Zo7uKl/ppCDl5nQ64JHWcv9TMvYgGsZmsrz/C9guGdpHQ/u/RBLZYKB58prnLJiLSyqgT2OrRIcBhZGywcJDZWqe4A==
b+nCD2ugo3l7VKEsxouFMzHw/ejYmWJQvyVvKyQX6dtWr5+AfCSX73cRHvojFPSW2IA8aq+T4ov0GKD2KY0ZGJRv+jyeaANVgC1j
gJN0sAvtuyuthKHz5wJjHXW/FvCjIWjFqwNH0aUlt4TAn5fmP79WA/Y6VbYg74z28WPiXbja4sC7vs1nwh4XjfFVbvBJ2DctPTwBihhCiw==
0LppAJ+WXxZEVAS3x6GJRq6zU7mYPq9jrTq2PFBicbgUmy47fmt8Rbu8cxwgfZ4QqRj6gq3J/PckSZI8yY0smpJSLUtTdFwwp93Lbh6ZF6lpfA==
### PWMANAGER IMPORT END V1 ###
*/
/*
Kategorie##Beschreibung##Login Name##Login Passwort##Favorit
Mail##GMX EDVMF##edvmf@gmx.de##test1##0
Mail##GMX JC##javacrypto@gmx.de##testpw2##1
Forum##Stackoverflow##bm98@arcor.de##Qw1"§##0
*/
