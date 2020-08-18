package org.jibe77.hermanas.data.entity;

import org.jibe77.hermanas.data.repository.PictureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class PictureRepositoryTest {

    @Autowired
    private PictureRepository pictureRepository;

    @BeforeEach
    public void setUp() throws Exception {
        Picture picture1= new Picture("2020/6/23/2020-6-23-0-7.jpg");

        //save user, verify has ID value after save
        assertNull(picture1.getId());
        this.pictureRepository.save(picture1);
        assertNotNull(picture1.getId());
    }

    @Test
    void testFetchData(){
        /*Test data retrieval*/
        Picture picture = pictureRepository.findByPath("2020/6/23/2020-6-23-0-7.jpg");
        assertNotNull(picture);
        assertNotNull(picture.getId());
        assertNull(picture.getChicken());
        assertNull(picture.getEggs());
    }
}
