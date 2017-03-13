# JuhakuDB current release: 1.0.7
Spring DATA like Android ORM Library for SQLite dabaseses

## Introduction
JuhakuDb is created to provide advanced database management with simplicity in mind as well. This libary implements 
Spring DATA and Hibernate like API for database management. Key features are annotation based ORM handling as well 
as filter based criteria API supporting nomal SQL as well in case of necessarity. 

This libaray supporst automatic database creation, schema updates as well as rollbacks. With easy to use and fast to 
configure API it gives you levarage for managing databases in Adroid devices. 

With JuhakuDb managing SQLite database is fast and simply and it is quick to adapt on using the library. Libarary also provides annotated repositories with basic CRUD functionalities.

> Note this is not yet available on jcenter but it will be in future in order to load it with dependency management application.

### ORM handling
Annotation based ORM handling reminds a lot Hibernate. If Hibernate is something you are already familiar with 
this should not be a too much to take then. There are similar annotations like ManyToOne, ManyToMany, OneToOne, OneToMany,
Entiy, Id, Column etc. and they work in similar way to Hibernite.

Annotated classes will be mapped accordingly and database tables will be automatically created with relations by the 
annotations.

### Filter based criteria api
Filter based criteria API is fairly similar to Spring DATA's representation of criteria API but in comparizon this is a lot simplier and easier to use. It provides easy way to create joint queries from multiple tables with easy to use filter chain syntax. This syntax allows you to create complex queries wihtout writing single line of sql.

## Usage
* Database manager
* Annotations
* Repositories
* Filter criterias

### Database manager
With following snippet you can create database manager. This is the core of the JuhakuDb which provides you configuration possibilities for database. Initializing of this class will automatically create your database by the given configurations.
This class should be initialized in super class or in application level and never should be initialized more than once in same runtime context for avoiding unwanted behaviour.
```java
DatabaseManager dbManager = new DatabaseManager(context, new DatabaseConfigurationAdapter() {
    @Override
    public void configure(DatabaseConfiguration configuration) {
        configuration.getBuilder().setBasePackages("db.juhaku.dbdemo.model", "db.juhaku.dbdemo.bean")
                .setVersion(1) // updating this version will cause database to update.
                .setName("dbtest.db")
                .setMode(SchemaCreationMode.UPDATE)
                .setAllowRollback(false)
                .setRollbackHistorySize(5); // back-ups allowed to take from database.
    }
});
```
Context here is a Android Context. Other attributes are quite self explanatory. They are documented so reading java docs will give you more knowledge on them as well.

### Annotations
Currently available annotation.

|Annotation| Description|
|------------------------------| --- |
|Column| Define column name for class attribute, can only be added to non-relation attribute|
|Entity| Define table name for class|
|Id| Define primary key field for database table, id is auto generated per table (way of the anroid). Only numeric value can be id currently|
|ManyToMany| Defines many to many relation|
|ManyToOne| Defines many to one relation|
|OneToMany| Defines one to many relation|
|OneToOne| Defines one to one relation|
|Repository| Marks interface as repository|
|Transient| Marks class attribute as transient which will not be saved to database|

All store operations are cascading and storing will return stored item with populated database id.
Fetch can be either EAGER or LAZY. This is defined by Fetch enum that can be provided as attribute for relation annotations. Lazy will not load relation from database and it need to be manually loaded. EAGER will automatically fetch referenced relation from database along with original item.

Annotated table examples
```java
@Entity(name = "classes")
public class Class {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = Fetch.EAGER)
    private Person person;

    @OneToOne(fetch = Fetch.EAGER)
    private Teacher teacher;
    
    // .... getters and setters
}

@Entity(name = "books")
public class Book {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToMany(fetch = Fetch.EAGER)
    private List<Person> persons;

    @ManyToMany
    private List<Library> libraries;
    
    // .... getters and setters   
}

@Entity(name = "persons")
public class Person {

    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "nick_name")
    private String nickName;

    @ManyToMany(fetch = Fetch.EAGER)
    private List<Book> books;

    @OneToMany(fetch = Fetch.EAGER)
    private List<Class> classes;
    
    // .... getters and setters   
}
```

### Repositories
Below is an example of creating repository interface. Repositories need to be annotated with Repository() annotation. Currently implementing class must be provided, but probably later it is possible to use default implementation as well instead of providing custom implementation for each repository.
```java
@Repository(BookRepositoryImpl.class)
public interface BookRepository {
    Long storeBook(Book book);
    List<Book> findBooks(Filters filters);
}
```
To implement the repository you could write something like this:
```java
public class BookRepositoryImpl extends SimpleRepository<Long, Book> implements BookRepository {

    public BookRepositoryImpl(EntityManager em) {
        super(em);
    }
    
    @Override
    public Long storeBook(Book book) {
        return store(book);
    }
    
    @Override
    public List<Book> findBooks(Filters filters) {
        return find(filters);
    }
}
```

Following snippet will allow you to retrieve instance of automatically initialized repository from database manager.
```java
repository = dbManager.getRepository(BookRepository.class);
```
### Filter criteria API
Following example is implemenation of simple person repository. This should give you an example of how you could work with 
filter criterias. 
```java
public class PersonRepositoryImpl extends SimpleRepository<Long, Person> implements PersonRepository {

    public PersonRepositoryImpl(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public Person storePerson(Person person) {
        return store(person);
    }

    @Override
    public List<Person> storePersons(List<Person> persons) {
        return storeAll(persons);
    }

    @Override
    public List<Person> findPersons(final String name) {
        Filters filters = new Filters();
        filters.add(new Filter<Person>() {
            @Override
            public void filter(Root<Person> root, Predicates predicates) {
                predicates.add(Predicate.eq("p.name", name));
            }
        });

        return find(filters);
    }

    @Override
    public List<Person> findAllPersons() {
        return find(new Filter<Person>() {
            @Override
            public void filter(Root<Person> root, Predicates predicates) {
            }
        });
    }

    @Override
    public List<String> findPersonBooks() {
        Query query = new Query("select persons_id, books_id from books_persons", null);
        return find(query, new ResultTransformer<List<String>>() {
            @Override
            public List<String> transformResult(List<ResultSet> resultSets) {
                List<String> strings = new ArrayList<>();
                for (ResultSet res : resultSets) {
                    String val = "";
                    for (Result result : res.getResults()) {
                        val = val.concat(result.getColumnName()).concat("=").concat(result.getColumnValue().toString());
                    }
                    strings.add(val);
                }

                return strings;
            }
        });
    }
}
```
Below are couple of more advanced examples.
```java
Filters filters = new Filters();
filters.add(new Filter<Person>() {
    @Override
    public void filter(Root<Person> root, Predicates predicates) {
        root.join("rooms", "r", JoinMode.LEFT_JOIN).join("r.teacher", "t", JoinMode.FULL_JOIN).join("groups", "g", JoinMode.INNER_JOIN);

        predicates.add(Predicate.in("p.name", "john", "matthew")).add(Predicate.not(Predicate.eq("p.name", "kim")));

        Disjunction or = Predicate.disjunction();
        or.add(Predicate.eq("t.name", "matt")).add(Predicate.eq("t.name", "laura"));
        predicates.add(or);

        predicates.add(Predicate.conjunction().add(Predicate.between("t.id", 1, 3))
                .add(Predicate.not(Predicate.isNull("t.name"))));

        predicates.sort(Order.ASC, "p.name");
        predicates.setPageSize(20).setPage(1);
    }
});
```

```java
Filters filters = new Filters();
filters.add(new Filter<ClassRoom>() {
    @Override
    public void filter(Root<ClassRoom> root, Predicates predicates) {
        root.join("persons", "p", JoinMode.INNER_JOIN);
        predicates.setPage(2).setPageSize(10).sort(Order.DESC, "c.name", "c._id").sort(Order.ASC, "p._id");
    }
});
```

These examples should help you to write your own filters. It is also possible to chain filters and provide only one predicate criteria per filter. The decision is yours.

## Roadmap

### 2.x release

Own ORM annotations will be replaced with javax.persistence annotations in favor of standardized usages.

### 3.x release

Added support for stand-alone SQLite database as well.

# Terms and conditions MIT
Copyright 2017 juhaku

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.