package sk.tuke.meta.persistence;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.beanutils.BeanUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ReflectivePersistenceManager implements PersistenceManager {
    private final Connection connection;
    private final Class<?>[] types;

    public ReflectivePersistenceManager(Connection connection, Class<?>... types) {
        this.connection = connection;
        this.types = types;
    }

    @Override
    public void createTables() {
        String fileName = "output.sql";
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        StringBuilder sqlQueries = new StringBuilder();

        try {
            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    sqlQueries.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] statements = sqlQueries.toString().split(";");
        executeQueriesInOrder(statements);
    }

    public static String[] reverseArray(String[] arr) {
        int i = 0, j = arr.length - 1;
        while (i < j) {
            String temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
            i++;
            j--;
        }
        return arr;
    }

    public void executeQueriesInOrder(String[] statements) {
        String[] statementsCopy = statements;
        int indexToRemove = 0;

        try {
            for(String sqlQuery : statements) {
                this.connection.setAutoCommit(false);
                Statement stmt = this.connection.createStatement();

                System.out.println(sqlQuery);
                int rowsAffected = stmt.executeUpdate(sqlQuery);
                if (rowsAffected == 0) {
                    System.out.println("Table created");
                    statementsCopy = Arrays
                                .stream(statementsCopy)
                                .filter(el -> !el.equals(sqlQuery))
                                .toArray(String[]::new);
                }

                this.connection.commit();
                this.connection.setAutoCommit(true);
                indexToRemove += 1;
            }
        } catch (SQLException e) {
            System.out.println("Error creating table");
            executeQueriesInOrder(reverseArray(statementsCopy));
            e.printStackTrace();
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        QueryBuilder queryBuilder = new QueryBuilder(type);
        String sql = queryBuilder.get(id);
        T instance = null;

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {

                // CREATE NEW INSTANCE
                try {
                    instance = type.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                // FILL THE FIELDS
                do {
                    assert instance != null;
                    instance = fillTheInstance(instance, rs);
                } while (rs.next());
            }
        } catch (SQLException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        if (instance != null) {
            return Optional.of(instance);
        } else {
            return Optional.empty();
        }
    }

    private <T>List<T> getMultiple(Class<T> type, String sql) {
        List<T> results = new ArrayList<T>();
        T instance = null;

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                // FILL THE FIELDS
                do {
                    // CREATE NEW INSTANCE
                    try {
                        instance = type.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    assert instance != null;
                    instance = fillTheInstance(instance, rs);
                    results.add(instance);
                } while (rs.next());
            }
        } catch (SQLException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return results;
    }

    private <T> T fillTheInstance(T instance, ResultSet rs) throws SQLException, IllegalAccessException, InvocationTargetException {
        assert instance != null;
        for (Field field : instance.getClass().getDeclaredFields()) {
            Annotation manyToOneAnnotation = field.getAnnotation(ManyToOne.class);
            field.setAccessible(true);

            if (manyToOneAnnotation != null) {
                // NEW FOREIGN KEY OBJECT
                Object newInstance = null;
                int primaryKey = 0;

                try {
                    newInstance = field.getType().getDeclaredConstructor().newInstance();

                    Column columnAnnotation = field.getAnnotation(Column.class);
                    String columnName = field.getName();

                    if(columnAnnotation != null) {
                        if(!columnAnnotation.name().isEmpty()) {
                            columnName = columnAnnotation.name();
                        }
                    }

                    primaryKey = rs.getInt(columnName);
                    String primaryKeyName = "";

                    // GET PRIMARY KEY NAME
                    for (Field f : field.getType().getDeclaredFields()) {
                        if (f.isAnnotationPresent(Id.class)) {
                            columnAnnotation = f.getAnnotation(Column.class);
                            columnName = f.getName();

                            if(columnAnnotation != null) {
                                if(!columnAnnotation.name().isEmpty()) {
                                    columnName = columnAnnotation.name();
                                }
                            }

                            primaryKeyName = columnName;
                            break;
                        }
                    }

                    // QUERY THE OBJECT FROM DB
                    Statement stmtForeign = this.connection.createStatement();

                    String tableName = field.getType().getSimpleName();
                    Table tableAnnotation = field.getType().getAnnotation(Table.class);

                    if(tableAnnotation != null) {
                        tableName = tableAnnotation.name();
                    }

                    String sqlForeign = "SELECT * FROM " + tableName + " WHERE " + primaryKeyName + " = " + primaryKey + ";";

                    // IF FOREIGN KEY OBJECT EXIST
                    if (primaryKey != 0) {
                        ResultSet rsForeign = stmtForeign.executeQuery(sqlForeign);

                        // SET OBJECT
                        if (!rsForeign.next()) {
                            System.out.println("ResultSet in empty in Java");
                        } else {
                            for (Field referencedObjectField : field.getType().getDeclaredFields()) {
                                columnAnnotation = referencedObjectField.getAnnotation(Column.class);
                                columnName = referencedObjectField.getName();

                                if(columnAnnotation != null) {
                                    if(!columnAnnotation.name().isEmpty()) {
                                        columnName = columnAnnotation.name();
                                    }
                                }

                                Object columnValue = rsForeign.getObject(columnName);
                                BeanUtils.setProperty(newInstance, referencedObjectField.getName(), columnValue);
                            }
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    System.out.println(e);
                }

                // SET THAT OBJECT TO FOREIGN KEY OBJECT
                field.setAccessible(true);

                if (primaryKey != 0) {
                    for (Field f : instance.getClass().getDeclaredFields()) {
                        if (f.getType().isAssignableFrom(newInstance.getClass())) {
                            BeanUtils.setProperty(instance, f.getName(), newInstance);
                        }
                    }
                }
            } else {
                try {
                    Column columnAnnotation = field.getAnnotation(Column.class);
                    String columnName = field.getName();

                    if(columnAnnotation != null) {
                        if(!columnAnnotation.name().isEmpty()) {
                            columnName = columnAnnotation.name();
                        }
                    }

                    Object columnValue = rs.getObject(columnName);
                    field.set(instance, columnValue);
                } catch (IllegalAccessException e) {
                    System.out.println(e);
                }
            }
        }

        return instance;
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        QueryBuilder queryBuilder = new QueryBuilder(type);
        String sql = queryBuilder.getAll();
        return getMultiple(type, sql);
    }

    @Override
    public <T> List<T> getBy(Class<T> type, String fieldName, Object value) {
        QueryBuilder queryBuilder = new QueryBuilder(type);
        String sql = queryBuilder.getBy(fieldName, value);
        return getMultiple(type, sql);
    }

    public long update(Object entity) {
        String sql = "UPDATE " + entity.getClass().getSimpleName() + "SET ";

        System.out.println(sql);
        return 0;
    };

    @Override
    public long save(Object entity) {
        Object objectPrimaryKey = null;

        for(Field field : entity.getClass().getDeclaredFields()) {
            Annotation idAnnotation = field.getAnnotation(Id.class);
            if (idAnnotation != null) {
                try {
                    field.setAccessible(true);
                    objectPrimaryKey = field.get(entity);
                }catch (IllegalAccessException e) {
                    System.out.println(e);
                }
            }
        }

        System.out.println("PRIMARY KEY OF OBJECT:" + objectPrimaryKey.toString());

//        if(!"0".equals(objectPrimaryKey.toString())) {
//            return update(entity);
//        }

        QueryBuilder queryBuilder = new QueryBuilder(entity.getClass());
        String sql = queryBuilder.save(entity);
        PreparedStatement sqlStatement = null;

        try {
            sqlStatement = this.connection.prepareStatement(sql);
        } catch (SQLException err) {
            System.out.println(err);
        }

        int counter = 1;
        for(Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // only when INSERT
            Annotation idAnnotation = field.getAnnotation(Id.class);
            if (idAnnotation != null) {
                continue;
            }

            Annotation manyToOneAnnotation = field.getAnnotation(ManyToOne.class);
            if (manyToOneAnnotation != null) {
                String idName = "";
                for (Field otherField : field.getType().getDeclaredFields()) {
                    if (otherField.isAnnotationPresent(Id.class)) {
                        idName = otherField.getName();
                        break;
                    }
                }

                Object idValue = null;

                try {
                    Object relatedEntity = field.get(entity);

                    if(relatedEntity == null) {
                        idValue = "NULL";
                    }else {
                        Field idField = relatedEntity.getClass().getDeclaredField(idName);
                        idField.setAccessible(true);
                        idValue = idField.get(relatedEntity); // retrieve the value of the Id field from the related entity object

                        if ("0".equals(idValue.toString())){
                            System.out.println("SAVING FOREIGN KEY");
                            idValue = save(relatedEntity);
                        }
                    }
                } catch (IllegalAccessException | NoSuchFieldException | NullPointerException e) {
                    System.out.println(e);
                }

                try {
                    assert sqlStatement != null;
                    if (idValue == null | idValue == "NULL") {
                        sqlStatement.setNull(counter, Types.INTEGER);
                    } else {
                        sqlStatement.setInt(counter, Integer.parseInt(idValue.toString()));
                    }
                } catch (SQLException err) {
                    System.out.println(err);
                }

                continue;
            }

            // check if sql is number or string
            try {
                Object value = field.get(entity);
                assert sqlStatement != null;
                if(value == null){
                    sqlStatement.setNull(counter, Types.INTEGER);
                } else{
                    sqlStatement.setString(counter, value.toString());
                    System.out.println(value.toString());
                }
            } catch (IllegalAccessException | SQLException | NumberFormatException e) {
                e.printStackTrace();
            }

            counter += 1;
        }

        try {
            assert sqlStatement != null;
            int rowsInserted = sqlStatement.executeUpdate();
            System.out.println("ROW INSERTED: " + rowsInserted);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int primaryKey = 0;
        try {
            Statement stmt = this.connection.createStatement();
            ResultSet getIdQuery = stmt.executeQuery("SELECT last_insert_rowid();");
            if (getIdQuery.next()) {
                primaryKey = getIdQuery.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for(Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            Annotation idAnnotation = field.getAnnotation(Id.class);
            if (idAnnotation != null) {
                try {
                    field.set(entity, primaryKey);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        return primaryKey;
    }

    @Override
    public void delete(Object entity) {
        QueryBuilder queryBuilder = new QueryBuilder(entity.getClass());
        String sql = queryBuilder.delete(entity);
        System.out.println(sql);

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println(e);
        }
    }
}
