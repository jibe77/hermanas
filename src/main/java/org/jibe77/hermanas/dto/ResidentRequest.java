package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Payload pour créer ou mettre à jour un pensionnaire")
public class ResidentRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String breed;

    private LocalDate birthDate;

    private LocalDate arrivalDate;

    private LocalDate deathDate;

    @Size(max = 2000)
    private String comments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(LocalDate arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(LocalDate deathDate) {
        this.deathDate = deathDate;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
