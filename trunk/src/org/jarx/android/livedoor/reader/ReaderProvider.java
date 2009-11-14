package org.jarx.android.livedoor.reader;

import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class ReaderProvider extends ContentProvider {

    public static final String AUTHORITY = "org.jarx.android.livedoor.reader";

    private static final String DATABASE_NAME = "reader.db";
    private static final int DATABASE_VERSION = 5;

    private static final UriMatcher uriMatcher;
    private static final int URI_SUBSCRIPTION_ID = 10;
    private static final int URI_SUBSCRIPTIONS = 11;
    private static final int URI_ITEM_ID = 20;
    private static final int URI_ITEMS = 21;
    private static final int URI_PIN_ID = 30;
    private static final int URI_PINS = 31;

    private static final String CONTENT_TYPE_ITEM
        = "vnd.android.cursor.item/vnd." + AUTHORITY;
    private static final String CONTENT_TYPE_DIR
        = "vnd.android.cursor.dir/vnd." + AUTHORITY;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, Subscription.TABLE_NAME + "/#", URI_SUBSCRIPTION_ID);
        uriMatcher.addURI(AUTHORITY, Subscription.TABLE_NAME, URI_SUBSCRIPTIONS);
        uriMatcher.addURI(AUTHORITY, Item.TABLE_NAME + "/#", URI_ITEM_ID);
        uriMatcher.addURI(AUTHORITY, Item.TABLE_NAME, URI_ITEMS);
        uriMatcher.addURI(AUTHORITY, Pin.TABLE_NAME + "/#", URI_PIN_ID);
        uriMatcher.addURI(AUTHORITY, Pin.TABLE_NAME, URI_PINS);
    }

    static String sqlCreateIndex(String tableName, String columnName) {
        StringBuilder buff = new StringBuilder(128);
        buff.append("create index idx_");
        buff.append(tableName);
        buff.append("_");
        buff.append(columnName);
        buff.append(" on ");
        buff.append(tableName);
        buff.append("(");
        buff.append(columnName);
        buff.append(")");
        return new String(buff);
    }

    private static String sqlIdWhere(String id, String where) {
        StringBuilder buff = new StringBuilder(128);
        buff.append(BaseColumns._ID);
        buff.append(" = ");
        buff.append(id);
        if (!TextUtils.isEmpty(where)) {
            buff.append(" and ");
            buff.append(where);
        }
        return new String(buff);
    }

    private static class ReaderOpenHelper extends SQLiteOpenHelper {

        private ReaderOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(Subscription.SQL_CREATE_TABLE);
            db.execSQL(Item.SQL_CREATE_TABLE);
            db.execSQL(Pin.SQL_CREATE_TABLE);
            for (String column: Subscription.INDEX_COLUMNS) {
                db.execSQL(sqlCreateIndex(Subscription.TABLE_NAME, column));
            }
            for (String column: Item.INDEX_COLUMNS) {
                db.execSQL(sqlCreateIndex(Item.TABLE_NAME, column));
            }
            for (String column: Pin.INDEX_COLUMNS) {
                db.execSQL(sqlCreateIndex(Pin.TABLE_NAME, column));
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            for (String sql: Subscription.sqlForUpgrade(oldVersion, newVersion)) {
                db.execSQL(sql);
            }
            for (String sql: Item.sqlForUpgrade(oldVersion, newVersion)) {
                db.execSQL(sql);
            }
            for (String sql: Pin.sqlForUpgrade(oldVersion, newVersion)) {
                db.execSQL(sql);
            }
        }
    }

    private ReaderOpenHelper openHelper;

    @Override
    public boolean onCreate() {
        this.openHelper = new ReaderOpenHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case URI_SUBSCRIPTION_ID:
        case URI_ITEM_ID:
        case URI_PIN_ID:
            return CONTENT_TYPE_ITEM;
        case URI_SUBSCRIPTIONS:
        case URI_ITEMS:
        case URI_PINS:
            return CONTENT_TYPE_DIR;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
        case URI_SUBSCRIPTION_ID:
            qb.setTables(Subscription.TABLE_NAME);
            qb.appendWhere(Subscription._ID + " = " + uri.getPathSegments().get(1));
            break;
        case URI_SUBSCRIPTIONS:
            qb.setTables(Subscription.TABLE_NAME);
            break;
        case URI_ITEM_ID:
            qb.setTables(Item.TABLE_NAME);
            qb.appendWhere(Item._ID + " = " + uri.getPathSegments().get(1));
            break;
        case URI_ITEMS:
            qb.setTables(Item.TABLE_NAME);
            break;
        case URI_PIN_ID:
            qb.setTables(Pin.TABLE_NAME);
            qb.appendWhere(Pin._ID + " = " + uri.getPathSegments().get(1));
            break;
        case URI_PINS:
            qb.setTables(Pin.TABLE_NAME);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = this.openHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String tableName;
        Uri contentUri;
        switch (uriMatcher.match(uri)) {
        case URI_SUBSCRIPTIONS:
            tableName = Subscription.TABLE_NAME;
            contentUri = Subscription.CONTENT_URI;
            break;
        case URI_ITEMS:
            tableName = Item.TABLE_NAME;
            contentUri = Item.CONTENT_URI;
            break;
        case URI_PINS:
            tableName = Pin.TABLE_NAME;
            contentUri = Pin.CONTENT_URI;
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = openHelper.getWritableDatabase();
        long rowId = db.insert(tableName, tableName, values);
        if (rowId > 0) {
            Uri insertedUri = ContentUris.withAppendedId(contentUri, rowId);
            getContext().getContentResolver().notifyChange(insertedUri, null);
            return insertedUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        return update(uri, null, where, whereArgs, false);
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        return update(uri, values, where, whereArgs, true);
    }

    private int update(Uri uri, ContentValues values, String where, String[] whereArgs,
            boolean update) {
        SQLiteDatabase db = this.openHelper.getWritableDatabase();
        String tableName;
        switch (uriMatcher.match(uri)) {
        case URI_SUBSCRIPTION_ID:
            tableName = Subscription.TABLE_NAME;
            where = sqlIdWhere(uri.getPathSegments().get(1), where);
            break;
        case URI_SUBSCRIPTIONS:
            tableName = Subscription.TABLE_NAME;
            break;
        case URI_ITEM_ID:
            tableName = Item.TABLE_NAME;
            where = sqlIdWhere(uri.getPathSegments().get(1), where);
            break;
        case URI_ITEMS:
            tableName = Item.TABLE_NAME;
            break;
        case URI_PIN_ID:
            tableName = Pin.TABLE_NAME;
            where = sqlIdWhere(uri.getPathSegments().get(1), where);
            break;
        case URI_PINS:
            tableName = Pin.TABLE_NAME;
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        int count = update ? db.update(tableName, values, where, whereArgs):
            db.delete(tableName, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}