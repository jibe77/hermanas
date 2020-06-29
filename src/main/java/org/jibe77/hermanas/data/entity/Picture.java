package org.jibe77.hermanas.data.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Picture {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String path;

    private Integer eggs;

    private Integer chicken;

    public Picture() {
    }

    public Picture(String path) {
        this.path = path;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getEggs() {
        return eggs;
    }

    public void setEggs(Integer eggs) {
        this.eggs = eggs;
    }

    public Integer getChicken() {
        return chicken;
    }

    public void setChicken(Integer chicken) {
        this.chicken = chicken;
    }

    @Override
    public String toString() {
        return "Picture{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", eggs=" + eggs +
                ", chicken=" + chicken +
                '}';
    }
}
