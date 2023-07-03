package sk.tuke.meta.example;

import sk.tuke.meta.persistence.EntityDAO;
import sk.tuke.meta.persistence.DAOPersistenceManager;
import sk.tuke.meta.example.Project;
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

public class ProjectDAO implements EntityDAO<Project> {
    private final DAOPersistenceManager manager;
    private final Connection connection;

    public ProjectDAO(DAOPersistenceManager manager) {
        this.manager = manager;
        this.connection = manager.getConnection();
    }

    @Override
    public void createTable() {
        String query = """
        create table if not exists CompanyProject (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT
                                    ,                                    ProjectName TEXT
                                                            NOT NULL
                                    UNIQUE
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
    public Optional<Project> get(long id) {
        Project instance = new Project();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM CompanyProject WHERE id = id;");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("id"));
                                                                                                instance.setName((String) rs.getObject("ProjectName"));
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
    public List<Project> getAll() {
        List<Project> results = new ArrayList<Project>();
        Project instance = new Project();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM CompanyProject;");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("id"));
                                                                                                instance.setName((String) rs.getObject("ProjectName"));
                                                            } while (rs.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public List<Project> getBy(String fieldName, Object value) {
        List<Project> results = new ArrayList<Project>();
        Project instance = new Project();

        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM CompanyProject WHERE " + fieldName + " = " + value + ";");

            if (!rs.next()) {
                System.out.println("ResultSet in empty in Java");
            } else {
                do {
                                                                        instance.setId((Integer) rs.getObject("id"));
                                                                                                instance.setName((String) rs.getObject("ProjectName"));
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
        Project object = (Project) entity;
        String sql = "";

        if(object.getId() == 0) {
            sql = "INSERT INTO CompanyProject (     ProjectName    ) VALUES ("       + "'" + object.getName() + "'"      + ")";
        } else {
            sql = "UPDATE CompanyProject SET"
                                + "id ="
                                     + object.getId()
                                                     + ","                                 + "ProjectName ="
                                     + "'" + object.getName() + "'"
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
        Project object = (Project) entity;
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

                
        sql = "DELETE FROM CompanyProject WHERE id = " + object.getId() + ";";
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