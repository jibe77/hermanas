package org.jibe77.hermanas.dto.mapper;

import org.jibe77.hermanas.data.entity.Resident;
import org.jibe77.hermanas.dto.ResidentDTO;
import org.jibe77.hermanas.dto.ResidentRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ResidentMapper {

    public ResidentDTO toDTO(Resident resident) {
        if (resident == null) {
            return null;
        }
        ResidentDTO dto = new ResidentDTO();
        dto.setId(resident.getId());
        dto.setName(resident.getName());
        dto.setBreed(resident.getBreed());
        dto.setBirthDate(resident.getBirthDate());
        dto.setArrivalDate(resident.getArrivalDate());
        dto.setDeathDate(resident.getDeathDate());
        dto.setComments(resident.getComments());
        if (resident.getPhotoFilename() != null && !resident.getPhotoFilename().isEmpty()) {
            dto.setPhotoUrl("/api/v1/residents/" + resident.getId() + "/photo");
        }
        return dto;
    }

    public List<ResidentDTO> toDTOList(List<Resident> residents) {
        if (residents == null) {
            return null;
        }
        return residents.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void applyRequest(Resident resident, ResidentRequest request) {
        resident.setName(request.getName());
        resident.setBreed(request.getBreed());
        resident.setBirthDate(request.getBirthDate());
        resident.setArrivalDate(request.getArrivalDate());
        resident.setDeathDate(request.getDeathDate());
        resident.setComments(request.getComments());
    }
}
