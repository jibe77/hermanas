package org.jibe77.hermanas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Pensionnaire (poule) du poulailler")
public class ResidentDTO {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Henriette")
    private String name;

    @Schema(example = "Marans")
    private String breed;

    @Schema(example = "2022-03-15")
    private LocalDate birthDate;

    @Schema(example = "2022-05-01")
    private LocalDate arrivalDate;

    @Schema(description = "Date du décès (null si la poule est toujours vivante)", example = "2024-08-12")
    private LocalDate deathDate;

    @Schema(example = "Première à venir picorer le matin.")
    private String comments;

    @Schema(description = "URL relative de la photo, null si aucune", example = "/api/v1/residents/1/photo")
    private String photoUrl;

    public ResidentDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
