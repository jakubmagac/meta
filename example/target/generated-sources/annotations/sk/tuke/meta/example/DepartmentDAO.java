package sk.tuke.meta.example;

import sk.tuke.meta.persistence.EntityDAO;
import sk.tuke.meta.persistence.DAOPersistenceManager;
import sk.tuke.meta.example.Department;
import java.lang.annotation.Annotation;

import java.sql.*;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

public class DepartmentDAO implements EntityDAO<Department> {
    private final DAOPersistenceManager manager;
    private final Connection connection;

    public DepartmentDAO(DAOPersistenceManager manager) {
        this.manager = manager;
        this.connection = manager.getConnection();
    }

    @Override
    public void createTable() {
        String query = """
        create table if not exists Department (
                                    pk INTEGER PRIMARY KEY AUTOINCREMENT
                                    ,                                    name TEXT
                                    ,                                    code TEXT
                                                    );
        """.replaceAll("\\s+", " ").trim();

        try {
            Statement stmt = this.connection.createStatement();
            int rowsAffected = stmt.executeUpdate(query);
        } catch (SQLException e) {
            System.out.println("Error creating table");
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Department> get(long id) {
        Department instance = new Department();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Department WHERE pk = id;");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("pk"));
                                                                                                instance.setName((String) rs.getObject("name"));
                                                                                                instance.setCode((String) rs.getObject("code"));
                                                            } while (rs.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (instance != null) {
            return Optional.of(instance);
        } else {
            return Optional.empty();
        }
    }

    private Object getForeignKey(Class type, int id) {
        return this.manager.get(type, id);
    }

    @Override
    public List<Department> getAll() {
        List<Department> results = new ArrayList<Department>();
        Department instance = new Department();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Department;");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("pk"));
                                                                                                instance.setName((String) rs.getObject("name"));
                                                                                                instance.setCode((String) rs.getObject("code"));
                                                            } while (rs.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public List<Department> getBy(String fieldName, Object value) {
        List<Department> results = new ArrayList<Department>();
        Department instance = new Department();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Department WHERE " + fieldName + " = " + value + ";");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("pk"));
                                                                                                instance.setName((String) rs.getObject("name"));
                                                                                                instance.setCode((String) rs.getObject("code"));
                                                            } while (rs.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public long save(Object entity) {
        PreparedStatement sqlStatement = null;
        Department object = (Department) entity;
        String sql = "";

        if(object.getId() == 0) {
            sql = "INSERT INTO Department (     name ,     code    ) VALUES ("       + "'" + object.getName() + "'"    + ","       + "'" + object.getCode() + "'"      + ")";
        } else {
            sql = "UPDATE Department SET"
                                + "pk ="
                                     + object.getId()
                                                     + ","                                 + "name ="
                                     + "'" + object.getName() + "'"
                                                     + ","                                 + "code ="
                                     + "'" + object.getCode() + "'"
                                                                     + "WHERE pk = " + object.getId() + ";";
        }

        System.out.println(sql);

        try {
            sqlStatement = this.connection.prepareStatement(sql);
        } catch (SQLException err) {
            System.out.println(err);
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

        System.out.println(primaryKey);

        return primaryKey;
    }

    @Override
    public void delete(Object entity) {
        PreparedStatement sqlStatement = null;
        Department object = (Department) entity;
        String sql = "";

                
        sql =  "UPDATE Person SET department = NULL WHERE department = " + object.getId();

        System.out.println(sql);

        try {
            sqlStatement = this.connection.prepareStatement(sql);
        } catch (SQLException err) {
            System.out.println(err);
        }

        try {
            assert sqlStatement != null;
            int rowsInserted = sqlStatement.executeUpdate();
            System.out.println("Null set");
        } catch (SQLException e) {
            e.printStackTrace();
        }

                
        sql = "DELETE FROM Department WHERE pk = " + object.getId() + ";";
        System.out.println(sql);

        try {
            sqlStatement = this.connection.prepareStatement(sql);
        } catch (SQLException err) {
            System.out.println(err);
        }

        try {
            assert sqlStatement != null;
            int rowsInserted = sqlStatement.executeUpdate();
            System.out.println("Row deleted");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}