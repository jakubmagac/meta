package sk.tuke.meta.example;

import sk.tuke.meta.persistence.EntityDAO;
import sk.tuke.meta.persistence.DAOPersistenceManager;
import sk.tuke.meta.example.Person;
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

public class PersonDAO implements EntityDAO<Person> {
    private final DAOPersistenceManager manager;
    private final Connection connection;

    public PersonDAO(DAOPersistenceManager manager) {
        this.manager = manager;
        this.connection = manager.getConnection();
    }

    @Override
    public void createTable() {
        String query = """
        create table if not exists Person (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT
                                    ,                                    surname TEXT
                                    ,                                    name TEXT
                                    ,                                    age INTEGER
                                    ,                                    department INTEGER,
                FOREIGN KEY (department) REFERENCES Department (pk)
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
    public Optional<Person> get(long id) {
        Person instance = new Person();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Person WHERE id = id;");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("id"));
                                                                                                instance.setSurname((String) rs.getObject("surname"));
                                                                                                instance.setName((String) rs.getObject("name"));
                                                                                                instance.setAge((Integer) rs.getObject("age"));
                                                                                                Object nullCheck = rs.getObject("department");
                                if (nullCheck != null) {
                                    instance.setDepartment(
                                        (sk.tuke.meta.example.Department)
                                            this.getForeignKey(sk.tuke.meta.example.Department.class, (Integer) rs.getObject("department"))
                                    );
                                } else {
                                    instance.setDepartment(null); // or handle the case when the department is null
                                }
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
    public List<Person> getAll() {
        List<Person> results = new ArrayList<Person>();
        Person instance = new Person();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Person;");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("id"));
                                                                                                instance.setSurname((String) rs.getObject("surname"));
                                                                                                instance.setName((String) rs.getObject("name"));
                                                                                                instance.setAge((Integer) rs.getObject("age"));
                                                                                                Object nullCheck = rs.getObject("department");
                                if (nullCheck != null) {
                                    instance.setDepartment(
                                        (sk.tuke.meta.example.Department)
                                            this.getForeignKey(sk.tuke.meta.example.Department.class, (Integer) rs.getObject("department"))
                                    );
                                } else {
                                    instance.setDepartment(null); // or handle the case when the department is null
                                }
                                                            } while (rs.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public List<Person> getBy(String fieldName, Object value) {
        List<Person> results = new ArrayList<Person>();
        Person instance = new Person();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Person WHERE " + fieldName + " = " + value + ";");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("id"));
                                                                                                instance.setSurname((String) rs.getObject("surname"));
                                                                                                instance.setName((String) rs.getObject("name"));
                                                                                                instance.setAge((Integer) rs.getObject("age"));
                                                                                                Object nullCheck = rs.getObject("department");
                                if (nullCheck != null) {
                                    instance.setDepartment(
                                        (sk.tuke.meta.example.Department)
                                            this.getForeignKey(sk.tuke.meta.example.Department.class, (Integer) rs.getObject("department"))
                                    );
                                } else {
                                    instance.setDepartment(null); // or handle the case when the department is null
                                }
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
        Person object = (Person) entity;
        String sql = "";

        if(object.getId() == 0) {
            sql = "INSERT INTO Person (     surname ,     name ,     age ,     department    ) VALUES ("       + "'" + object.getSurname() + "'"    + ","       + "'" + object.getName() + "'"    + ","       + object.getAge()    + ","      + object.getDepartment().getId()     + ")";
        } else {
            sql = "UPDATE Person SET"
                                + "id ="
                                     + object.getId()
                                                     + ","                                 + "surname ="
                                     + "'" + object.getSurname() + "'"
                                                     + ","                                 + "name ="
                                     + "'" + object.getName() + "'"
                                                     + ","                                 + "age ="
                                     + object.getAge()
                                                     + ","                                 + "department ="
                                    + object.getDepartment().getId()
                                                 + "WHERE id = " + object.getId() + ";";
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
        Person object = (Person) entity;
        String sql = "";

        
        sql = "DELETE FROM Person WHERE id = " + object.getId() + ";";
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