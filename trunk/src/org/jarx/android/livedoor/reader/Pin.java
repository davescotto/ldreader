package org.jarx.android.livedoor.reader;

import java.io.Serializable;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import static org.jarx.android.livedoor.reader.Utils.*; 

public class Pin implements Cloneable, Serializable, BaseColumns {

    public static final String TABLE_NAME = "pin";

    public static final String CONTENT_URI_NAME
        = "content://" + ReaderProvider.AUTHORITY + "/" + TABLE_NAME;

    public static final Uri CONTENT_URI = Uri.parse(CONTENT_URI_NAME);

    public static final String _URI = "uri";
    public static final String _TITLE = "title";
    public static final String _ACTION = "action";
    public static final String _CREATED_TIME = "created_time";

    public static final String SQL_CREATE_TABLE
        = "create table if not exists " + TABLE_NAME + " ("
        + _ID + " integer primary key,"
        + _URI + " text,"
        + _TITLE + " text,"
        + _ACTION + " integer,"
        + _CREATED_TIME + " integer"
        + ")";

    public static final String[] INDEX_COLUMNS = {
        _URI,
        _ACTION
    };

    public static final int ACTION_NONE = 0;
    public static final int ACTION_ADD = 1;
    public static final int ACTION_REMOVE = 2;

    public static String[] sqlForUpgrade(int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            return new String[] {
                SQL_CREATE_TABLE,
                ReaderProvider.sqlCreateIndex(TABLE_NAME, _URI),
                ReaderProvider.sqlCreateIndex(TABLE_NAME, _ACTION)
            };
        }
        return new String[0];
    }

    private long id;
    private String uri;
    private String title;
    private int action;
    private long createdTime;

    public Pin() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getAction() {
        return this.action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public long getCreatedTime() {
        return this.createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError("clone error: " + e);
        }
    }

    public static class FilterCursor {

        private final Pin pin;
        private final Cursor cursor;
        private final int posId;
        private final int posUri;
        private final int posTitle;
        private final int posAction;
        private final int posCreatedTime;

        public FilterCursor(Cursor cursor) {
            this(cursor, null);
        }

        public FilterCursor(Cursor cursor, Pin pin) {
            this.pin = pin;
            this.cursor = cursor;
            this.posId = cursor.getColumnIndex(Pin._ID);
            this.posUri = cursor.getColumnIndex(Pin._URI);
            this.posTitle = cursor.getColumnIndex(Pin._TITLE);
            this.posAction = cursor.getColumnIndex(Pin._ACTION);
            this.posCreatedTime = cursor.getColumnIndex(Pin._CREATED_TIME);
        }

        public Pin getPin() {
            Pin pin = (this.pin == null) ? new Pin(): this.pin;
            pin.setId(this.cursor.getLong(this.posId));
            pin.setUri(this.cursor.getString(this.posUri));
            pin.setTitle(this.cursor.getString(this.posTitle));
            pin.setAction(this.cursor.getInt(this.posAction));
            pin.setCreatedTime(this.cursor.getLong(this.posCreatedTime));
            return pin;
        }

        public int getCount() {
            return this.cursor.getCount();
        }

        public boolean moveToPosition(int position) {
            return this.cursor.moveToPosition(position);
        }

        public boolean moveToNext() {
            return this.cursor.moveToNext();
        }

        public Cursor getCursor() {
            return this.cursor;
        }

        public void deactivate() {
            this.cursor.deactivate();
        }

        public boolean requery() {
            return this.cursor.requery();
        }

        public boolean isClosed() {
            return this.cursor.isClosed();
        }

        public void close() {
            this.cursor.close();
        }
    }
}
