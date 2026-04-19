package com.yas.media.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileTypeValidatorTest {

    private FileTypeValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new FileTypeValidator();
        ValidFileType ann = mock(ValidFileType.class);
        when(ann.allowedTypes()).thenReturn(new String[] {"image/jpeg", "image/png", "image/gif"});
        when(ann.message()).thenReturn("invalid");
        validator.initialize(ann);

        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void isValid_whenFileNull_returnsFalse() {
        assertFalse(validator.isValid(null, context));
    }

    @Test
    void isValid_whenContentTypeNull_returnsFalse() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn(null);
        assertFalse(validator.isValid(file, context));
    }

    @Test
    void isValid_whenContentTypeNotAllowed_returnsFalse() {
        MultipartFile file = new MockMultipartFile("f", "a.bin", "application/octet-stream", new byte[] {1});
        assertFalse(validator.isValid(file, context));
    }

    @Test
    void isValid_whenPngImageReadable_returnsTrue() throws Exception {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.BLUE.getRGB());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        MultipartFile file = new MockMultipartFile("f", "x.png", "image/png", baos.toByteArray());
        assertTrue(validator.isValid(file, context));
    }

    @Test
    void isValid_whenBytesAreNotImage_returnsFalse() {
        MultipartFile file = new MockMultipartFile("f", "x.png", "image/png", "not-an-image".getBytes());
        assertFalse(validator.isValid(file, context));
    }
}
