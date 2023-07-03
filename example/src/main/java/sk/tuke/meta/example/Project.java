package sk.tuke.meta.example;

import javax.persistence.*;

@Entity
@Table(name = "CompanyProject")
public class Project {
    @Id
    private long id;

    @Column(name = "ProjectName", nullable = false, unique = true)
    private String name;

    public Project(String name) {
        this.name = name;
    }

    public Project() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(int id) { this.id = id; }

    @Override
    public String toString() {
        return String.format("Project %d: %s", id, name);
    }
}
