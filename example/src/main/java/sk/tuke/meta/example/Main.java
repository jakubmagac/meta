package sk.tuke.meta.example;
import sk.tuke.meta.example.GeneratedPersistenceManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {
    public static final String DB_PATH = "test.db";

    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

        GeneratedPersistenceManager manager = new GeneratedPersistenceManager(conn);
        manager.createTables();

        Project project = new Project("Next.js");
        Department mng = new Department("managment", "mng");
        Person hrasko = new Person("Janko", "Hrasko", 30);
        manager.save(mng);
        hrasko.setDepartment(mng);

        manager.save(project);
        manager.save(hrasko);
        manager.delete(mng);

        List<Person> people = manager.getAll(Person.class);

        for(Person p : people) {
            System.out.println(p.toString());
        }

        conn.close();
    }
}
