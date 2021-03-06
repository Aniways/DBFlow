package com.raizlabs.android.dbflow.sql.migration;

import android.database.sqlite.SQLiteDatabase;

/**
 * Description: Called when the Database is migrating. We can perform custom migrations here. A {@link com.raizlabs.android.dbflow.annotation.Migration}
 * is required for registering this class to automatically be called in an upgrade of the DB.
 */
public interface Migration {

    /**
     * Called before we migrate data. Instantiate migration data before releasing it in {@link #onPostMigrate()}
     */
    public void onPreMigrate();

    /**
     * Perform your migrations here
     *
     * @param database The database to operate on
     */
    public void migrate(SQLiteDatabase database);

    /**
     * Called after the migration completes. Release migration data here.
     */
    public void onPostMigrate();
}
