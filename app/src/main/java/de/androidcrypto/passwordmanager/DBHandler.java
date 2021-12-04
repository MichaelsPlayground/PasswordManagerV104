package de.androidcrypto.passwordmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;

public class DBHandler extends SQLiteOpenHelper {

    // creating a constant variables for our database.
    // below variable is for our database name.
    private static final String DB_NAME = "entrydb";

    // below int is our database version,
    // when changing don't forget to change HEADER & FOOTER as well
    private static final int DB_VERSION = 1;

    // below variable is for our table name.
    private static final String TABLE_NAME = "entries";

    // below variable is for our id column.
    private static final String ID_COL = "id";

    // below variable are for our column names
    private static final String ENTRY_NAME_COL = "entryname";
    private static final String LOGIN_NAME_COL = "loginname";
    private static final String LOGIN_PASSWORD_COL = "loginpassword";
    private static final String CATEGORY_COL = "category";
    private static final String FAVOURITE_COL = "favourite";
    private static final String EXPIRED_COL = "expired";
    private static final String RESERVED_COL = "reserved";

    // get a context to the database
    private Context dbContext;

    // header and footer export string
    private static final String DATABASE_HEADER = "### DATABASE START V1 ###";
    private static final String DATABASE_FOOTER = "### DATABASE END V1 ###";
    private static final String DATA_HEADER = "#Z#";
    private static final String DATA_FOOTER = "#Y#";
    private int PBKDF2ITERATIONS = 15000; // for en-/decryption key derivation from password

    // creating a constructor for our database handler.
    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        dbContext = context;
    }

    // below method is for creating a database by running a sqlite query
    @Override
    public void onCreate(SQLiteDatabase db) {
        // on below line we are creating
        // an sqlite query and we are
        // setting our column names
        // along with their data types.
        String query = "CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ENTRY_NAME_COL + " TEXT,"
                + LOGIN_NAME_COL + " TEXT,"
                + LOGIN_PASSWORD_COL + " TEXT,"
                + CATEGORY_COL + " TEXT,"
                + FAVOURITE_COL + " TEXT, "
                + EXPIRED_COL + " TEXT, "
                + RESERVED_COL + " TEXT)";
        // at last we are calling a exec sql
        // method to execute above sql query
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // this method is called to check if the table exists already.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addNewEntry(String entryName, String loginName, String loginPassword, String category, String favourite) {
        // on below line we are creating a variable for
        // our sqlite database and calling writable method
        // as we are writing data in our database.
        SQLiteDatabase db = this.getWritableDatabase();

        // on below line we are creating a
        // variable for content values.
        ContentValues values = new ContentValues();

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(ENTRY_NAME_COL, entryName);
        values.put(LOGIN_NAME_COL, loginName);
        //values.put(LOGIN_PASSWORD_COL, loginPassword); // missing encryption to Base64String
        values.put(LOGIN_PASSWORD_COL, Cryptography.encryptStringAesGcmToBase64(loginPassword));
        values.put(CATEGORY_COL, category);
        values.put(FAVOURITE_COL, favourite);
        values.put(EXPIRED_COL, false);
        values.put(RESERVED_COL, "");

        // after adding all values we are passing
        // content values to our table.
        db.insert(TABLE_NAME, null, values);

        // at last we are closing our
        // database after adding database.
        db.close();
    }


    public void updateEntry(String entryId, String entryName, String loginName, String loginPassword, String category, String favourite) {
        // calling a method to get writable database.
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(ENTRY_NAME_COL, entryName);
        values.put(LOGIN_NAME_COL, loginName);
        //values.put(LOGIN_PASSWORD_COL, loginPassword); // missing encryption to Base64String
        values.put(LOGIN_PASSWORD_COL, Cryptography.encryptStringAesGcmToBase64(loginPassword));
        values.put(CATEGORY_COL, category);
        values.put(FAVOURITE_COL, favourite);
        values.put(EXPIRED_COL, false);
        values.put(RESERVED_COL, "");

        db.update(TABLE_NAME, values, "id=?", new String[]{entryId});
        db.close();
    }

    // below is the method for deleting our course.
    public void deleteAllEntries() {

        // on below line we are creating
        // a variable to write our database.
        SQLiteDatabase db = this.getWritableDatabase();

        // on below line we are calling a method to delete our
        // course and we are comparing it with our course name.
        db.delete(TABLE_NAME, null, null);
        db.close();
    }

    public void deleteEntry(String entryId) {
        // on below line we are creating
        // a variable to write our database.
        SQLiteDatabase db = this.getWritableDatabase();
        // on below line we are calling a method to delete our
        // course and we are comparing it with our course name.
        db.delete(TABLE_NAME, "id=?", new String[]{entryId});
        db.close();
    }

    // we have created a new method for reading all the courses.
    public ArrayList<EntryModel> readEntries() {
        // on below line we are creating a
        // database for reading our database.
        SQLiteDatabase db = this.getReadableDatabase();

        // on below line we are creating a cursor with query to read data from database.
        Cursor cursorEntries = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        // on below line we are creating a new array list.
        ArrayList<EntryModel> entryModelArrayList = new ArrayList<>();

        // moving our cursor to first position.
        if (cursorEntries.moveToFirst()) {
            do {
                // on below line we are adding the data from cursor to our array list.
                entryModelArrayList.add(new EntryModel(
                        cursorEntries.getString(1),
                        cursorEntries.getString(2),
                        cursorEntries.getString(3),
                        cursorEntries.getString(4),
                        cursorEntries.getString(5),
                        cursorEntries.getString(6),
                        //cursorEntries.getString(7)));
                        cursorEntries.getString(7),
                        cursorEntries.getString(0))); // 0 = entryId
            } while (cursorEntries.moveToNext());
            // moving our cursor to next.
        }
        // at last closing our cursor
        // and returning our array list.
        cursorEntries.close();
        return entryModelArrayList;
    }

    // we have created a new method for reading all the courses.
    public String getNrDatabaseRecords() {
        SQLiteDatabase db = this.getReadableDatabase();
        long numberOfRows = DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        return String.valueOf(numberOfRows);
    }

    // we have created a new method for reading all the courses.
    public String getDatabaseSize(Context context) {
        File f = context.getDatabasePath(DB_NAME);
        long dbSize = f.length();
        return String.valueOf(dbSize);
    }
}
