package sk.tuke.meta.example;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
public class Department {
    @Id
    private long pk;
    private String name;
    private String code;

    public Department() {
    }

    public Department(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public long getId() {
        return this.pk;
    }

    public void setId(int id) { this.pk = id; }

    public void setCode(String code) {
        this.code = code;
    }

    public String toString() {
        return String.format("Department %d: %s (%s)", pk, name, code);
    }
}
