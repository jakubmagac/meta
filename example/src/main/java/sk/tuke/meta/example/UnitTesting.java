package sk.tuke.meta.example;

import org.junit.jupiter.api.Test;
import sk.tuke.meta.persistence.DAOPersistenceManager;
import javax.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sk.tuke.meta.persistence.SqlGeneralHelper.PrepareDB;

public class UnitTesting {
    public static final String DB_PATH = "test.db";
    @Test
    public void MakeObjectFetchFromDB(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }

    @Test
    public void EmptyDepartmentNull(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        manager.save(hrasko);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }


    @Test
    public void EmptyDepartment(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development=new Department();
        hrasko.setDepartment(development);
        Optional<Person> retHrasko= manager.get(Person.class,manager.save(hrasko));
        Person rethrasko=retHrasko.get();
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,rethrasko);
    }


    @Test
    public void SpecialChar(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Jan'\0ko", "Hra'sko", 30);
        manager.save(hrasko);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }

    @Test
    public void MultiplePeople(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Department development = new Department("Development", "DVLP");
        Department marketing = new Department("Marketing", "MARK");
        Department operations = new Department("Operations", "OPRS");

        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development);
        Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
        mrkvicka.setDepartment(operations);
        Person novak = new Person("Jan", "Novak", 45);
        novak.setDepartment(marketing);

        manager.save(hrasko);
        manager.save(mrkvicka);
        manager.save(novak);

        List<Person> People = manager.getAll(Person.class);

        //assuming that only one item in db
        List<Person> retMrkvicka= People.stream()
                .filter(person -> person.getSurname().equals("Jozko"))
                .collect(Collectors.toList());

        List<Person> retHrasko= People.stream()
                .filter(person -> person.getSurname().equals("Janko"))
                .collect(Collectors.toList());

        List<Person> retNovak= People.stream()
                .filter(person -> person.getSurname().equals("Jan"))
                .collect(Collectors.toList());

        if(retMrkvicka.size()>1 || retHrasko.size()>1 || retNovak.size()>1){
            throw new PersistenceException();
        }
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,retHrasko.get(0));
        CheckPerson(mrkvicka,retMrkvicka.get(0));
        CheckPerson(novak,retNovak.get(0));
    }

    @Test
    public void GetBy(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);

        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);

        List<Person> retHrasko=manager.getBy(Person.class,"surname","Janko");

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,retHrasko.get(0));
    }

    @Test
    public void UpdateSimpleField(){

        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);

        hrasko.setAge(50);
        hrasko.setName("Peter");
        hrasko.setSurname("Novak");

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }

    @Test
    public void UpdateSubObjectNew(){

        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);

        Department marketing = new Department("Marketing", "MARK");
        hrasko.setDepartment(marketing);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }

    @Test
    public void UpdateFieldOfSubObject(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);
        development.setName("devops");
        manager.save(development);

        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,manager.save(hrasko));
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }

    @Test
    public void DeleteItemFromDb(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);
        manager.save(hrasko);
        manager.delete(hrasko);
        List<Person> returned=manager.getAll(Person.class);

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if(returned.size()!=0){
            throw new RuntimeException("Problem with deleting from db");
        }
    }


    @Test
    public void DeleteSubItemFromDb(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Person hrasko = new Person("Janko", "Hrasko", 30);
        Department development = new Department("Development", "DVLP");
        hrasko.setDepartment(development);

        long hraskoId=manager.save(hrasko);
        manager.delete(development);

        hrasko.setDepartment(null);
        Optional<Person> hraskoFromDbOptional=manager.get(Person.class,hraskoId);
        Person hraskoFromDb=hraskoFromDbOptional.get();

        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        CheckPerson(hrasko,hraskoFromDb);
    }


    @Test

    public void OneDepartmentMultiplePerson(){
        Connection conn=PrepareDB(DB_PATH);
        DAOPersistenceManager manager= new DAOPersistenceManager(conn);
        Department development = new Department("Development", "DVLP");

        Person hrasko = new Person("Janko", "Hrasko", 30);
        hrasko.setDepartment(development);
        Person mrkvicka = new Person("Jozko", "Mrkvicka", 25);
        mrkvicka.setDepartment(development);

        manager.save(hrasko);
        manager.save(mrkvicka);

        List<Department> departments=manager.getAll(Department.class);
        if(departments.size()>1){
            throw new PersistenceException("Pocet departmentov je viac ako 1 pri rovnakom objekte!");
        }
        List<Person> People=manager.getAll(Person.class);

        List<Person> retMrkvicka= People.stream()
                .filter(person -> person.getSurname().equals("Jozko"))
                .collect(Collectors.toList());


        List<Person> retHrasko= People.stream()
                .filter(person -> person.getSurname().equals("Janko"))
                .collect(Collectors.toList());

        if(retMrkvicka.size()>1 || retHrasko.size()>1 ){
            throw new PersistenceException();
        }

        try {
            conn.close();
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        CheckPerson(hrasko,retHrasko.get(0));
        CheckPerson(mrkvicka,retMrkvicka.get(0));
    }


    public void CheckPerson(Person before, Person after){
        assertEquals(before.getName(),after.getName());
        assertEquals(before.getSurname(),after.getSurname());
        assertEquals(before.getAge(),after.getAge());
        CheckDepartment(before.getDepartment(),after.getDepartment());
    }

    public void CheckDepartment(Department before,Department after){
        if (before == null && after == null) {
            return; // Both objects are null, so the test passes.
        }
        assertEquals(before.getName(),after.getName());
        assertEquals(before.getCode(),after.getCode());
    }
}