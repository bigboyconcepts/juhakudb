package db.juhaku.juhakudb.core.android;

import java.lang.reflect.Field;

import db.juhaku.juhakudb.annotation.Inject;
import db.juhaku.juhakudb.annotation.Repository;
import db.juhaku.juhakudb.util.ReflectionUtils;

/**
 * Created by juha on 13/03/17.
 *<p>This class provides automatic repository injection for objects having fields annotated with {@link Inject}
 * annotation. Class is mainly useful for {@link android.app.Activity} and {@link android.app.Fragment}
 * classes. </p>
 *
 * <p>This class is called via {@link DatabaseManager} and should not be initialized manually.</p>
 *
 * @author juha
 *
 * @since 1.1.0
 */
public class RepositoryLookupInjector {

    private DatabaseManager databaseManager;

    RepositoryLookupInjector(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Method will lookup and inject repositories if found from given object. Repository should be
     * mapped with {@link Repository} annotation and injected to objects field with {@link Inject}
     * annotation.
     *
     * @param obj Object to look for Injectable repositories.
     * @throws IllegalStateException if lookup injection will fail for number of reason.
     */
    void lookupRepositories(Object obj) {
        if (obj == null) {
            throw new IllegalStateException("Object: " + obj + " provided must not be null");
        }
        Class<?> lookup = obj.getClass();
        while (lookup.getSuperclass() != null) {
            for (Field field : lookup.getDeclaredFields()) {
                boolean accessible = field.isAccessible();
                field.setAccessible(true);

                if (field.getAnnotation(Inject.class) != null) {
                    Class<?> repositoryType = ReflectionUtils.getFieldType(field);

                    // make sure found field with Inject annotation is repository
                    if (repositoryType.getAnnotation(Repository.class) == null) {
                        throw new IllegalStateException("Wrongly placed " + Inject.class.getName() + " annotation. " +
                                "It should only be placed on classes mapped with: " + Repository.class.getName() + " annotation.");
                    }
                    try {
                        Object repository = field.get(obj);

                        // if repository is not injected inject it
                        if (repository == null) {
                            field.set(obj, databaseManager.getRepository(repositoryType));
                        }
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Could not access field value: " + field, e);
                    }
                }

                field.setAccessible(accessible);
            }

            lookup = lookup.getSuperclass();
        }
    }
}
