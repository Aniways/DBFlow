package com.raizlabs.android.dbflow.config;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.raizlabs.android.dbflow.DatabaseHelperListener;
import com.raizlabs.android.dbflow.converter.TypeConverter;
import com.raizlabs.android.dbflow.sql.builder.ConditionQueryBuilder;
import com.raizlabs.android.dbflow.sql.migration.Migration;
import com.raizlabs.android.dbflow.structure.BaseModelJoin;
import com.raizlabs.android.dbflow.structure.BaseModelView;
import com.raizlabs.android.dbflow.structure.InvalidDBConfiguration;
import com.raizlabs.android.dbflow.structure.JoinAdapter;
import com.raizlabs.android.dbflow.structure.Model;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.raizlabs.android.dbflow.structure.ModelViewAdapter;
import com.raizlabs.android.dbflow.structure.container.ContainerAdapter;

import java.util.List;
import java.util.Map;

/**
 * Author: andrewgrosner
 * Description: Holds information about the database and wraps some of the methods.
 */
public class FlowManager {

    private static Context context;

    private static DatabaseHolder mDatabaseHolder;

    /**
     * Returns the table name for the specific model class
     *
     * @param table The class that implements {@link com.raizlabs.android.dbflow.structure.Model}
     * @return The table name, which can be different than the {@link com.raizlabs.android.dbflow.structure.Model} class name
     */
    @SuppressWarnings("unchecked")
    public static String getTableName(Class<? extends Model> table) {
        ModelAdapter modelAdapter = getModelAdapter(table);
        String tableName = null;
        if (modelAdapter == null) {
            ModelViewAdapter modelViewAdapter = getDatabaseForTable(table).getModelViewAdapterForTable((Class<? extends BaseModelView>) table);
            if (modelViewAdapter != null) {
                tableName = modelViewAdapter.getViewName();
            }
        } else {
            tableName = modelAdapter.getTableName();
        }
        return tableName;
    }

    /**
     * Returns the corresponding {@link com.raizlabs.android.dbflow.config.FlowManager} for the specified model
     *
     * @param table
     * @return
     */
    public static BaseDatabaseDefinition getDatabaseForTable(Class<? extends Model> table) {
        getDatabaseHolder();

        BaseDatabaseDefinition flowManager = mDatabaseHolder.getDatabaseForTable(table);
        if (flowManager == null) {
            throw new InvalidDBConfiguration("Table: " + table.getName() + " is not registered with a Database. " +
                    "Did you forget the @Table annotation?");
        }
        return flowManager;
    }

    /**
     * Returns the corresponding {@link com.raizlabs.android.dbflow.config.FlowManager} for the specified model
     *
     * @param table
     * @return
     */
    public static BaseDatabaseDefinition getDatabase(String databaseName) {
        getDatabaseHolder();

        BaseDatabaseDefinition flowManager = mDatabaseHolder.getDatabase(databaseName);
        if (flowManager == null) {
            throw new InvalidDBConfiguration();
        }
        return flowManager;
    }

    protected static DatabaseHolder getDatabaseHolder() {
        if (mDatabaseHolder == null) {
            try {
                mDatabaseHolder = (DatabaseHolder) Class.forName("com.raizlabs.android.dbflow.config.Database$Holder").newInstance();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return mDatabaseHolder;
    }

    /**
     * Returns the primary where query for a specific table. Its the WHERE statement containing columnName = ?.
     *
     * @param table The class that implements {@link com.raizlabs.android.dbflow.structure.Model}
     * @return The primary where query
     */
    @SuppressWarnings("unchecked")
    public static <ModelClass extends Model> ConditionQueryBuilder<ModelClass> getPrimaryWhereQuery(Class<ModelClass> table) {
        return getDatabaseForTable(table).getModelAdapterForTable(table).getPrimaryModelWhere();
    }

    /**
     * Will throw an exception if this class is not initialized yet in {@link #init(android.content.Context)}
     *
     * @return
     */
    public static Context getContext() {
        if (context == null) {
            throw new IllegalStateException("Context cannot be null for FlowManager");
        }
        return context;
    }

    /**
     * Initializes DBFlow, retrieving the Database holder via reflection. This will trigger all creations,
     * updates, and instantiation for each database defined.
     * @param context               The shared context for database usage.
     */
    public static void init(Context context) {
        FlowManager.context = context;
        getDatabaseHolder();
    }

    /**
     * Registers a listener for database creation/update events. Call this before running any query.
     *
     * @param databaseName           The name of the database. Will throw an exception if the database doesn't exist.
     * @param databaseHelperListener Provides callbacks for database events.
     */
    public static void setDatabaseListener(String databaseName, DatabaseHelperListener databaseHelperListener) {
        getDatabase(databaseName).setHelperListener(databaseHelperListener);
    }

    /**
     * Returns the specific {@link com.raizlabs.android.dbflow.converter.TypeConverter} for this model. It defines
     * how the class is stored in the DB
     *
     * @param modelClass   The class that implements {@link com.raizlabs.android.dbflow.structure.Model}
     * @param <ModelClass> The class that implements {@link com.raizlabs.android.dbflow.structure.Model}
     * @return
     */
    public static TypeConverter getTypeConverterForClass(Class<?> modelClass) {
        return mDatabaseHolder.getTypeConverterForClass(modelClass);
    }

    // region Getters

    /**
     * Release reference to context
     */
    public static synchronized void destroy() {
        context = null;
    }

    /**
     * Returns the model adapter for the specified table. Used in loading and modifying the model class.
     *
     * @param modelClass   The class of the table
     * @param <ModelClass> The class that implements {@link com.raizlabs.android.dbflow.structure.Model}
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <ModelClass extends Model> ModelAdapter<ModelClass> getModelAdapter(Class<ModelClass> modelClass) {
        return FlowManager.getDatabaseForTable(modelClass).getModelAdapterForTable(modelClass);
    }

    /**
     * Returns the container adapter for the specified table. These are only generated when you specify {@link com.raizlabs.android.dbflow.annotation.ContainerAdapter}
     * in your model class so it can be used for containers. These are not generated by default as a means to save space.
     *
     * @param modelClass   The class of the table
     * @param <ModelClass> The class that implements {@link com.raizlabs.android.dbflow.structure.Model}
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <ModelClass extends Model> ContainerAdapter<ModelClass> getContainerAdapter(Class<ModelClass> modelClass) {
        return FlowManager.getDatabaseForTable(modelClass).getModelContainerAdapterForTable(modelClass);
    }

    /**
     *
     * @param modelViewClass   The class of the VIEW
     * @param <ModelViewClass> The class that implements {@link com.raizlabs.android.dbflow.structure.Model}
     * @return the {@link com.raizlabs.android.dbflow.structure.ModelViewAdapter} for a SQLite VIEW. These are only created with the {@link com.raizlabs.android.dbflow.annotation.ModelView} annotation.
     */
    @SuppressWarnings("unchecked")
    public static <ModelViewClass extends BaseModelView<? extends Model>> ModelViewAdapter<? extends Model, ModelViewClass> getModelViewAdapter(Class<ModelViewClass> modelViewClass) {
        return getDatabaseForTable(modelViewClass).getModelViewAdapterForTable(modelViewClass);
    }

    /**
     * @param joinClass The class to join
     * @param <JoinClass> The class that extends {@link com.raizlabs.android.dbflow.structure.BaseModelJoin}
     * @return the {@link com.raizlabs.android.dbflow.structure.JoinAdapter} for SQLite JOIN. These are only created with the {@link com.raizlabs.android.dbflow.annotation.ModelJoin} annotation
     */
    @SuppressWarnings("unchecked")
    public static <JoinClass extends BaseModelJoin> JoinAdapter<JoinClass>  getJoinAdapter(Class<JoinClass> joinClass) {
        return getDatabaseForTable(joinClass).getJoinAdapterForTable(joinClass);
    }

    static Map<Integer, List<Migration>> getMigrations(String databaseName) {
        return getDatabase(databaseName).getMigrations();
    }

    /**
     * Checks a standard database helper for integrity using quick_check(1).
     * @param helper
     * @return true if it's integrity is OK.
     */
    public static boolean isDatabaseIntegrityOk(SQLiteOpenHelper helper) {
        boolean integrityOk = true;

        SQLiteStatement prog = null;
        try {
            prog = helper.getWritableDatabase().compileStatement("PRAGMA quick_check(1)");
            String rslt = prog.simpleQueryForString();
            if (!rslt.equalsIgnoreCase("ok")) {
                // integrity_checker failed on main or attached databases
                FlowLog.log(FlowLog.Level.E, "PRAGMA integrity_check on temp DB returned: " + rslt);

                integrityOk = false;
            }
        } finally {
            if (prog != null) {
                prog.close();
            }
        }
        return integrityOk;
    }

    // endregion

}
