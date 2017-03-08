package com.aware.plugin.survey;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.Log;
//import android.widget.Toast;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.survey.provider.survey"; //change to package.provider.your_plugin_name

    public static final int DATABASE_VERSION = 3; //increase this if you make changes to the database structure, i.e., rename columns, etc.
    public static final String DATABASE_NAME = "plugin_survey2.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String DB_TBL_TEMPLATE = "plugin_survey2";


    //For each table, add two indexes: DIR and ITEM. The index needs to always increment. Next one is 3, and so on.
    private static final int TABLE_ONE_DIR = 1;
    private static final int TABLE_ONE_ITEM = 2;

    //Put tables names in this array so AWARE knows what you have on the database
    public static final String[] DATABASE_TABLES = {
            DB_TBL_TEMPLATE
    };

    //These are columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    /**
     * Create one of these per database table
     * In this example, we are adding example columns
     */
    public static final class Plugin_Survey_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_TEMPLATE);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.com.aware.plugin.survey.provider.survey"; //modify me
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.com.aware.plugin.survey.provider.survey"; //modify me

        //Note: integers and strings don't need a type prefix_
        public static final String NAME = "name";
        public static final String BIG_NUMBER = "double_big_number"; //a double_ prefix makes a MySQL DOUBLE column
        //public static final String TRIGGER = "trigger";
        //public static final String ANSWERS = "answers";
        public static final String TEST = "test";
        //public static final String PICTURE = "blob_picture"; //a blob_ prefix makes a MySQL BLOB column
    }

    //Define each database table fields
    private static final String DB_TBL_TEMPLATE_FIELDS =
            Plugin_Survey_Data._ID + " integer primary key autoincrement," +
                    Plugin_Survey_Data.TIMESTAMP + " real default 0," +
                    Plugin_Survey_Data.DEVICE_ID + " text default ''," +
                    Plugin_Survey_Data.NAME + " text default ''," +
                    Plugin_Survey_Data.BIG_NUMBER + " real default 0," +
                   // Plugin_Survey_Data.TRIGGER + " text default ''," +
                    //Plugin_Survey_Data.ANSWERS + " text default ''";
                    Plugin_Survey_Data.TEST + " text default ''";
//                   Plugin_Survey_Data.PICTURE + " blob default null";

    /**
     * Share the fields with AWARE so we can replicate the table schema on the server
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_TEMPLATE_FIELDS
    };

    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
            Log.d("DATABSE", dbHelper.toString());
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }
    //--

    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> tableOneHash;

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
        AUTHORITY = getContext().getPackageName() + ".provider.survey"; //make sure xxx matches the first string in this class

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], TABLE_ONE_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", TABLE_ONE_ITEM);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        tableOneHash = new HashMap<>();
        tableOneHash.put(Plugin_Survey_Data._ID, Plugin_Survey_Data._ID);
        tableOneHash.put(Plugin_Survey_Data.TIMESTAMP, Plugin_Survey_Data.TIMESTAMP);
        tableOneHash.put(Plugin_Survey_Data.DEVICE_ID, Plugin_Survey_Data.DEVICE_ID);
        tableOneHash.put(Plugin_Survey_Data.NAME, Plugin_Survey_Data.NAME);
        tableOneHash.put(Plugin_Survey_Data.BIG_NUMBER, Plugin_Survey_Data.BIG_NUMBER);
//        tableOneHash.put(Plugin_Survey_Data.TRIGGER, Plugin_Survey_Data.TRIGGER);
//        tableOneHash.put(Plugin_Survey_Data.ANSWERS, Plugin_Survey_Data.ANSWERS);
        tableOneHash.put(Plugin_Survey_Data.TEST, Plugin_Survey_Data.TEST);
//        tableOneHash.put(Plugin_Survey_Data.PICTURE, Plugin_Survey_Data.PICTURE);

        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case, increasing the index accordingly
            case TABLE_ONE_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();
        Log.d("DATABASE", values.toString());
        Log.d("DATABASE", String.valueOf(database));

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case TABLE_ONE_DIR:
                long _id = database.insert(DATABASE_TABLES[0], Plugin_Survey_Data.DEVICE_ID, values);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Plugin_Survey_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            //Add all tables' DIR entries, with the right table index
            case TABLE_ONE_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableOneHash); //the hashmap of the table
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            //Add each table indexes DIR and ITEM
            case TABLE_ONE_DIR:
                return Plugin_Survey_Data.CONTENT_TYPE;
            case TABLE_ONE_ITEM:
                return Plugin_Survey_Data.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {



        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case TABLE_ONE_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
}