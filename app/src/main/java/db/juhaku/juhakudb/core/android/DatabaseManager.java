package db.juhaku.juhakudb.core.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.TypeVariable;
import java.util.Enumeration;

import dalvik.system.DexFile;
import db.juhaku.juhakudb.annotation.Repository;
import db.juhaku.juhakudb.core.Criteria;
import db.juhaku.juhakudb.core.DatabaseConfiguration;
import db.juhaku.juhakudb.core.DatabaseConfigurationAdapter;
import db.juhaku.juhakudb.exception.SchemaInitializationException;

/**
 * Created by juha on 16/12/15.
 *<p>DatabaseManager manages database initialization, schema creation as well as repository
 * initialization.</p>
 * <p>This class should be provided only once in application context in order to avoid unwanted
 * outcomes.</p>
 * <code>
 *     Super activity or more likely in custom application you can implement this by:<br/><br/>
 *
 *     DatabaseManager dm = new DatabaseManager(this, new DatabaseConfigurationAdapter() &#123;<br/><br/>
 *
 *        &#9;&#64;Override<br/>
 *        &#9;public void configure(DatabaseConfiguration configuration) &#123;<br/>
 *            &#9;&#9;configuration.getBuilder().setBasePackages("db.juhaku.dbdemo.model", "db.juhaku.dbdemo.bean")<br/>
 *              &#9;&#9;.setVersion(1).setName("dbtest.db").setMode(SchemaCreationMode.UPDATE);<br/>
 *        &#9;&#125;<br/>
 *     &#125;);<br/>
 * </code>
 * @author juha
 *
 * @since 1.0.2
 */
public class DatabaseManager {


    private DatabaseConfiguration configuration;
    private DatabaseHelper databaseHelper;
    private Object[] repositories = new Object[0];
    private EntityManager em;

    /**
     * Initialize new DatabaseManager with current context and database configuration adapter.
     * @param context instance of {@link Context} where to create manager for.
     * @param adapter instance of {@link DatabaseConfigurationAdapter} to provide configuration for
     *                this database manager.
     *
     * @since 1.0.2
     */
    public DatabaseManager(Context context, DatabaseConfigurationAdapter adapter) {
        this.configuration = new DatabaseConfiguration();
        adapter.configure(configuration);
        Class<?>[] entityClasses = resolveClasses(context, new EntityCriteria(configuration.getBasePackages()));
        try {
            databaseHelper = new DatabaseHelper(context, entityClasses, configuration);
        } catch (SchemaInitializationException e) {
            Log.e(getClass().getName(), "Failed to create database", e);
        }
        em = new EntityManager(databaseHelper);

        if (configuration.getRepositoryLocations() == null) {
            initializeRepositories(resolveClasses(context,
                    new RepositoryCriteria(context.getApplicationInfo().packageName)));
        } else {
            String[] locations = new String[configuration.getRepositoryLocations().length + 1];
            System.arraycopy(configuration.getRepositoryLocations(), 0, locations, 0,
                    configuration.getRepositoryLocations().length);
            locations[locations.length - 1] = context.getApplicationInfo().packageName;
            initializeRepositories(resolveClasses(context, new RepositoryCriteria(locations)));
        }
    }

    /*
     * Resolve classes by given criteria and return resolved classes as array.
     */
    private Class<?>[] resolveClasses(Context context, Criteria criteria) {
        Class<?>[] retVal = new Class[0];
        DexFile dexFile = getSourcesDex(context);
        if (dexFile != null) {
            Enumeration<String> dexEntries = dexFile.entries();
            while (dexEntries.hasMoreElements()) {
                String dexFileName = dexEntries.nextElement();
                if (!criteria.meetCriteria(dexFileName)) {
                    continue;
                }
                Class<?> clazz = initializeClass(dexFileName);
                if (clazz != null) {
                    int length = retVal.length;
                    Class<?>[] newRetVal = new Class[length + 1];
                    System.arraycopy(retVal, 0, newRetVal, 0, length);
                    newRetVal[length] = clazz;
                    retVal = newRetVal;
                }
            }
        }

        return retVal;
    }

    private DexFile getSourcesDex(Context context) {
        DexFile dexFile = null;
        try {
            dexFile = new DexFile(context.getApplicationInfo().sourceDir);
        } catch (IOException e) {
            Log.e(getClass().getName(), "Could not load sources, no database tables " +
                    "will be created" + e);
        }

        return dexFile;
    }

    private static Class<?> initializeClass(String name) {
        try {
            return Class.forName(name);
        } catch (Exception e) {
            Log.e(DatabaseManager.class.getName(), "Class could not be initialized by name: " +
                    name + ", table wont be created to database", e);
        }

        return null;
    }

    /*
     * Initialize given repository interfaces.
     */
    private <T> void initializeRepositories(Class[] interfaces) {
        for (Class<?> repository : interfaces) {
            Class<?> impl = repository.getAnnotation(Repository.class).value();
            for (Constructor cons : impl.getDeclaredConstructors()) {
                Class[] params = cons.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(EntityManager.class)) {
                    try {
                        T repo = (T) cons.newInstance(em);
                        int len = repositories.length;
                        Object[] newRepos = new Object[len + 1];
                        System.arraycopy(repositories, 0, newRepos, 0, len);
                        repositories = newRepos;
                        repositories[len] = repo;
                    } catch (Exception e) {
                        Log.w(getClass().getName(), "could not initialize constructor with params: "
                                + em, e);
                    }
                }
            }
        }
    }

    /**
     * Get repository from database manager for data access purposes.
     *
     * @param type Class<T> of type to look for repository.
     *
     * @return Found repository as given type or null if not found.
     *
     * @since 1.0.2
     */
    public <T> T getRepository(Class<T> type) {
        for (Object repository : repositories) {
            if (type.isAssignableFrom(repository.getClass())) {
                return (T) repository;
            }
        }

        return null;
    }
}