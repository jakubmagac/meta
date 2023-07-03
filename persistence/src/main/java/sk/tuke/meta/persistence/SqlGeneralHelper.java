package sk.tuke.meta.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Ref;
import java.sql.SQLException;

public class SqlGeneralHelper {

    public static Connection PrepareDB(String path)  {
        try{
            return DriverManager.getConnection("jdbc:sqlite:" + path);
        }catch (SQLException e){
            throw new RuntimeException("Error");
        }
    }
    public static PersistenceManager CreateNewTables(Connection connection, Class... classes){
        ReflectivePersistenceManager manager = new ReflectivePersistenceManager(connection, classes);
        manager.createTables();
        return manager;
    }

}