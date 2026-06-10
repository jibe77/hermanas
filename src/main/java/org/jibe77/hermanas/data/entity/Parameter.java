package org.jibe77.hermanas.data.entity;

import javax.persistence.*;
import java.util.Objects;

@Entity
public class Parameter {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true)
    private String entryKey;

    // TEXT instead of the default VARCHAR(255). The AI prompt alone is ~1.6 kB
    // and previously triggered MariaDB "Data too long for column 'entry_value'"
    // (SQL state 22001) on save. TEXT gives us 64 kB which covers the prompt
    // plus any other multi-paragraph config we might want to persist later.
    @Lob
    @Column(columnDefinition = "TEXT")
    private String entryValue;

    public Parameter() {
    }

    public String getEntryKey() {
        return entryKey;
    }

    public void setEntryKey(String entryKey) {
        this.entryKey = entryKey;
    }

    public String getEntryValue() {
        return entryValue;
    }

    public void setEntryValue(String entryValue) {
        this.entryValue = entryValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return Objects.equals(entryKey, parameter.entryKey) &&
                Objects.equals(entryValue, parameter.entryValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryKey, entryValue);
    }

    @Override
    public String toString() {
        return "Parameter{" +
                "entryKey='" + entryKey + '\'' +
                ", entryValue='" + entryValue + '\'' +
                '}';
    }
}
