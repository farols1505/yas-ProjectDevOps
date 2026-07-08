package com.yas.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.config.YasConfig;
import com.yas.media.mapper.MediaVmMapper;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.repository.FileSystemRepository;
import com.yas.media.repository.MediaRepository;
import com.yas.media.service.MediaServiceImpl;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceUnitTest {

    @Spy
    private MediaVmMapper mediaVmMapper = Mappers.getMapper(MediaVmMapper.class);

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Mock
    private YasConfig yasConfig;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Media media;

    @BeforeEach
    void setUp() throws Exception {
        media = new Media();
        media.setId(1L);
        media.setCaption("test");
        media.setFileName("file");
        media.setMediaType("image/jpeg");

        lenient().doReturn("/stored/test-file.png").when(fileSystemRepository)
            .persistFile(anyString(), any(byte[].class));
    }

    @Test
    void getMediaById_whenValidId_thenReturnData() throws Exception {
        NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
        when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
        when(yasConfig.publicUrl()).thenReturn("/media/");

        MediaVm mediaVm = mediaService.getMediaById(1L);
        assertNotNull(mediaVm);
        assertEquals("Test", mediaVm.getCaption());
        assertEquals("fileName", mediaVm.getFileName());
        assertEquals("image/png", mediaVm.getMediaType());
        assertEquals(String.format("/media/medias/%s/file/%s", 1L, "fileName"), mediaVm.getUrl());
    }

    @Test
    void getMediaById_whenMediaNotFound_thenReturnNull() throws Exception {
        when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(null);

        MediaVm mediaVm = mediaService.getMediaById(1L);
        assertNull(mediaVm);
    }

    @Test
    void removeMedia_whenMediaNotFound_thenThrowsNotFoundException() throws Exception {
        when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(null);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> mediaService.removeMedia(1L));
        assertEquals(String.format("Media %s is not found", 1L), exception.getMessage());
    }

    @Test
    void removeMedia_whenValidId_thenRemoveSuccess() throws Exception {
        NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
        when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
        doNothing().when(mediaRepository).deleteById(1L);

        mediaService.removeMedia(1L);

        verify(mediaRepository, times(1)).deleteById(1L);
    }

    @Test
    void saveMedia_whenTypePng_thenSaveSuccess() throws Exception {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.png", "image/png", pngFileContent);
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("fileName", mediaSave.getFileName());
        verify(fileSystemRepository).persistFile(eq("fileName"), argThat(b -> b != null && b.length == 0));
    }

    @Test
    void saveMedia_whenFileNameOverrideHasText_usesTrimmedValue() throws Exception {
        byte[] content = new byte[] {1, 2};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "ignored.png", "image/png", content);
        MediaPostVm mediaPostVm = new MediaPostVm("cap", multipartFile, "  custom.png  ");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media saved = mediaService.saveMedia(mediaPostVm);

        assertEquals("custom.png", saved.getFileName());
        verify(fileSystemRepository).persistFile(eq("custom.png"), argThat(b -> b != null && b.length == 2 && b[0] == 1 && b[1] == 2));
    }

    @Test
    void saveMedia_whenTypeJpeg_thenSaveSuccess() throws Exception {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.jpeg", "image/jpeg", pngFileContent);
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("fileName", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenTypeGif_thenSaveSuccess() throws Exception {
        byte[] gifFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.gif", "image/gif", gifFileContent);
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("fileName", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenFileNameIsNull_thenUsesOriginalFilename() throws Exception {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.png", "image/png", pngFileContent);
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, null);

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("example.png", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenFileNameIsEmpty_thenUsesOriginalFilename() throws Exception {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.png", "image/png", pngFileContent);
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("example.png", mediaSave.getFileName());
    }

    @Test
    void saveMedia_whenFileNameIsBlank_thenUsesOriginalFilename() throws Exception {
        byte[] pngFileContent = new byte[] {};
        MultipartFile multipartFile = new MockMultipartFile(
            "file", "example.png", "image/png", pngFileContent);
        MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "   ");

        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Media mediaSave = mediaService.saveMedia(mediaPostVm);
        assertNotNull(mediaSave);
        assertEquals("media", mediaSave.getCaption());
        assertEquals("example.png", mediaSave.getFileName());
    }

    @Test
    void getFile_whenMediaNotInDb_returnsEmptyDto() throws Exception {
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

        MediaDto mediaDto = mediaService.getFile(1L, "fileName");

        assertNull(mediaDto.getContent());
        assertNull(mediaDto.getMediaType());
    }

    @Test
    void getFile_whenFileNameMismatch_returnsEmptyDto() throws Exception {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));

        MediaDto mediaDto = mediaService.getFile(1L, "other.png");

        assertNull(mediaDto.getContent());
        assertNull(mediaDto.getMediaType());
    }

    @Test
    void getFile_whenMatchCaseInsensitive_returnsStreamAndType() throws Exception {
        Media stored = new Media();
        stored.setId(1L);
        stored.setFileName("Photo.PNG");
        stored.setFilePath("/data/photo");
        stored.setMediaType("image/png");
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(stored));
        InputStream stream = new ByteArrayInputStream(new byte[] {9, 8, 7});
        when(fileSystemRepository.getFile("/data/photo")).thenReturn(stream);

        MediaDto mediaDto = mediaService.getFile(1L, "photo.png");

        assertEquals(MediaType.IMAGE_PNG, mediaDto.getMediaType());
        assertArrayEquals(new byte[] {9, 8, 7}, mediaDto.getContent().readAllBytes());
    }

    @Test
    void getMediaByIds_returnsMappedListWithUrls() throws Exception {
        var ip15 = getMedia(-1L, "Iphone 15");
        var macbook = getMedia(-2L, "Macbook");
        var existingMedias = List.of(ip15, macbook);
        when(mediaRepository.findAllById(List.of(ip15.getId(), macbook.getId())))
            .thenReturn(existingMedias);
        when(yasConfig.publicUrl()).thenReturn("https://media/");

        var medias = mediaService.getMediaByIds(List.of(ip15.getId(), macbook.getId()));

        assertFalse(medias.isEmpty());
        verify(mediaVmMapper, times(existingMedias.size())).toVm(any());
        assertThat(medias).allMatch(m -> m.getUrl() != null);
    }

    @Test
    void getMediaByIds_whenEmptyInput_returnsEmptyList() throws Exception {
        when(mediaRepository.findAllById(List.of())).thenReturn(List.of());
        assertThat(mediaService.getMediaByIds(List.of())).isEmpty();
    }

    private static @NotNull Media getMedia(Long id, String name) {
        var m = new Media();
        m.setId(id);
        m.setFileName(name);
        return m;
    }
}
