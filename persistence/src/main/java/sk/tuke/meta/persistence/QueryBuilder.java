package sk.tuke.meta.persistence;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;

public class QueryBuilder {
    private Class<?> table;

    QueryBuilder(Class<?> table) {
        this.table = table;
    }

    public String get(long id) {
        String tableName = this.table.getSimpleName();
        Table tableAnnotation = this.table.getAnnotation(Table.class);

        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        }

        String sql = "SELECT * FROM " + tableName + " WHERE";
        String idName = "";

        for (Field otherField : this.table.getDeclaredFields()) {
            if (otherField.isAnnotationPresent(Id.class)) {
                idName = otherField.getName();
                break;
            }
        }

        sql += " " + idName + " = " + id + ";";
        return sql;
    }

    public String getAll() {
        String tableName = this.table.getSimpleName();
        Table tableAnnotation = this.table.getAnnotation(Table.class);

        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        }

        return "SELECT * FROM " + tableName;
    }

    public String getBy(String fieldName, Object value) {
        String tableName = this.table.getSimpleName();
        String columnName = fieldName;
        Table tableAnnotation = this.table.getAnnotation(Table.class);

        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        }

        for(Field field : this.table.getDeclaredFields()) {
            Column columnAnnotation = field.getAnnotation(Column.class);

            if(columnAnnotation != null & field.getName().equals(fieldName) ) {
                if(!columnAnnotation.name().isEmpty()) {
                    columnName = columnAnnotation.name();
                }
            }
        }

        return  "SELECT * FROM " + tableName + " WHERE " + columnName + " = '" + value.toString() + "';";
    }

    public String delete(Object entity) {
        String tableName = this.table.getSimpleName();
        Table tableAnnotation = this.table.getAnnotation(Table.class);

        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        }

        String sql = "DELETE FROM " + tableName + " WHERE ";
        String primaryKeyName = "";
        String primaryKey = "";

        for(Field f : entity.getClass().getDeclaredFields()) {
            if(f.isAnnotationPresent(Id.class)){
                Column columnAnnotation = f.getAnnotation(Column.class);
                primaryKeyName = f.getName();


                if(columnAnnotation != null) {
                    if(!columnAnnotation.name().isEmpty()) {
                        primaryKeyName = columnAnnotation.name();
                    }
                }
                f.setAccessible(true);

                try {
                    primaryKey = f.get(entity).toString();
                } catch (IllegalAccessException e) {
                    System.out.println(e);
                }
                break;
            }
        }

        sql += primaryKeyName + " = " + primaryKey + ";";
        return sql;
    }

    public String save(Object entity) {
        String tableName = this.table.getSimpleName();
        Table tableAnnotation = this.table.getAnnotation(Table.class);

        if(tableAnnotation != null) {
            tableName = tableAnnotation.name();
        }

        String sql = "INSERT INTO " + tableName + "(";
        boolean firstField = true;

        for(Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // only when INSERT
            Annotation idAnnotation = field.getAnnotation(Id.class);
            if (idAnnotation != null) {
                continue;
            }

            if(firstField) {
                firstField = false;
            } else {
                sql += ", ";
            }

            Column columnAnnotation = field.getAnnotation(Column.class);
            String columnName = field.getName();

            if(columnAnnotation != null) {
                if(!columnAnnotation.name().isEmpty()) {
                    columnName = columnAnnotation.name();
                }
            }

            sql += columnName;
        }

        sql += ") VALUES (";

        firstField = true;
        for(Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            // only when INSERT
            Annotation idAnnotation = field.getAnnotation(Id.class);
            if (idAnnotation != null) {
                continue;
            }

            if(firstField) {
                firstField = false;
            } else {
                sql += ", ";
            }

            sql += "?";
        }

        sql += ");";
        return sql;
    }
 }
