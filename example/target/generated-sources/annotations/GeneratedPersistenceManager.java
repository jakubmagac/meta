package sk.tuke.meta.example;
import sk.tuke.meta.persistence.DAOPersistenceManager;

import java.sql.Connection;

import sk.tuke.meta.example.Department;
import sk.tuke.meta.example.DepartmentDAO;
import sk.tuke.meta.example.Person;
import sk.tuke.meta.example.PersonDAO;
import sk.tuke.meta.example.Project;
import sk.tuke.meta.example.ProjectDAO;

public class GeneratedPersistenceManager extends DAOPersistenceManager {
    public GeneratedPersistenceManager(Connection connection) {
        super(connection);
        putDAO(Department.class, new DepartmentDAO(this));
        putDAO(Person.class, new PersonDAO(this));
        putDAO(Project.class, new ProjectDAO(this));
    }
}