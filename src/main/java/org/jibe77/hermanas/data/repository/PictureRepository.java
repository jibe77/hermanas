package org.jibe77.hermanas.data.repository;

import org.jibe77.hermanas.data.entity.Picture;
import org.springframework.data.repository.CrudRepository;

public interface PictureRepository extends CrudRepository<Picture, Long> {

    Picture findByPath(String path);
}
