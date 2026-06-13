package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.entity.Resident;
import org.jibe77.hermanas.data.repository.ResidentRepository;
import org.jibe77.hermanas.dto.ResidentDTO;
import org.jibe77.hermanas.dto.ResidentRequest;
import org.jibe77.hermanas.dto.mapper.ResidentMapper;
import org.jibe77.hermanas.service.event.EventService;
import org.jibe77.hermanas.service.resident.ResidentPhotoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/residents")
@Tag(name = "Residents", description = "CRUD des pensionnaires (poules) du poulailler")
public class ResidentRestController {

    private static final Logger logger = LoggerFactory.getLogger(ResidentRestController.class);

    private final ResidentRepository repository;
    private final ResidentMapper mapper;
    private final ResidentPhotoStorage photoStorage;
    private final EventService eventService;

    public ResidentRestController(ResidentRepository repository,
                                  ResidentMapper mapper,
                                  ResidentPhotoStorage photoStorage,
                                  EventService eventService) {
        this.repository = repository;
        this.mapper = mapper;
        this.photoStorage = photoStorage;
        this.eventService = eventService;
    }

    @Operation(summary = "Liste tous les pensionnaires, triés par date d'arrivée décroissante")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "OK"))
    @GetMapping
    public List<ResidentDTO> list() {
        return mapper.toDTOList(repository.findAllByOrderByArrivalDateDescIdDesc());
    }

    @Operation(summary = "Détail d'un pensionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Pensionnaire introuvable", content = @Content)
    })
    @GetMapping("/{id}")
    public ResidentDTO get(@PathVariable Long id) {
        return mapper.toDTO(find(id));
    }

    @Operation(summary = "Crée un nouveau pensionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Créé",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResidentDTO.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalide", content = @Content)
    })
    @PostMapping
    public ResidentDTO create(@Valid @RequestBody ResidentRequest request) {
        Resident resident = new Resident();
        mapper.applyRequest(resident, request);
        Resident saved = repository.save(resident);
        logger.info("Resident created: id={}, name={}", saved.getId(), saved.getName());
        eventService.record(EventType.RESIDENT_CREATED,
                "id=" + saved.getId() + " name=" + saved.getName());
        return mapper.toDTO(saved);
    }

    @Operation(summary = "Met à jour un pensionnaire existant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mis à jour"),
            @ApiResponse(responseCode = "404", description = "Pensionnaire introuvable", content = @Content)
    })
    @PutMapping("/{id}")
    public ResidentDTO update(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
        Resident resident = find(id);
        mapper.applyRequest(resident, request);
        Resident saved = repository.save(resident);
        logger.info("Resident updated: id={}, name={}", saved.getId(), saved.getName());
        eventService.record(EventType.RESIDENT_UPDATED,
                "id=" + saved.getId() + " name=" + saved.getName());
        return mapper.toDTO(saved);
    }

    @Operation(summary = "Supprime définitivement un pensionnaire et sa photo")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Supprimé"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Resident resident = find(id);
        String name = resident.getName();
        if (resident.getPhotoFilename() != null) {
            photoStorage.delete(resident.getPhotoFilename());
        }
        repository.delete(resident);
        logger.info("Resident deleted: id={}", id);
        eventService.record(EventType.RESIDENT_DELETED, "id=" + id + " name=" + name);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Retourne la photo binaire du pensionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image binaire"),
            @ApiResponse(responseCode = "404", description = "Pas de photo", content = @Content)
    })
    @GetMapping("/{id}/photo")
    public ResponseEntity<FileSystemResource> getPhoto(@PathVariable Long id) {
        Resident resident = find(id);
        String filename = resident.getPhotoFilename();
        if (filename == null || filename.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No photo");
        }
        Path path = photoStorage.resolve(filename);
        if (path == null || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file missing on disk");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photoStorage.contentTypeFor(filename)))
                .body(new FileSystemResource(path.toFile()));
    }

    @Operation(summary = "Upload (ou remplace) la photo d'un pensionnaire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo enregistrée"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide", content = @Content)
    })
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResidentDTO uploadPhoto(@PathVariable Long id,
                                   @RequestParam("file") MultipartFile file) throws IOException {
        Resident resident = find(id);
        String oldFilename = resident.getPhotoFilename();
        String newFilename = photoStorage.save(file);
        resident.setPhotoFilename(newFilename);
        Resident saved = repository.save(resident);
        if (oldFilename != null && !oldFilename.equals(newFilename)) {
            photoStorage.delete(oldFilename);
        }
        logger.info("Resident {} photo uploaded: {}", id, newFilename);
        eventService.record(EventType.RESIDENT_PHOTO_UPLOADED,
                "id=" + id + " name=" + resident.getName() + " file=" + newFilename);
        return mapper.toDTO(saved);
    }

    @Operation(summary = "Supprime la photo du pensionnaire (l'enregistrement reste)")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Photo supprimée"))
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        Resident resident = find(id);
        if (resident.getPhotoFilename() != null) {
            String oldFilename = resident.getPhotoFilename();
            photoStorage.delete(oldFilename);
            resident.setPhotoFilename(null);
            repository.save(resident);
            logger.info("Resident {} photo deleted: {}", id, oldFilename);
            eventService.record(EventType.RESIDENT_PHOTO_DELETED,
                    "id=" + id + " name=" + resident.getName() + " file=" + oldFilename);
        }
        return ResponseEntity.noContent().build();
    }

    private Resident find(Long id) {
        Optional<Resident> opt = repository.findById(id);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resident " + id + " not found");
        }
        return opt.get();
    }
}
