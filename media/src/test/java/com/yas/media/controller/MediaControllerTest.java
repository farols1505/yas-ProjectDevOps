package com.yas.media.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = MediaController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MediaService mediaService;

    @Test
    void getById_returnsOk() throws Exception {
        when(mediaService.getMediaById(1L))
            .thenReturn(new MediaVm(1L, "c", "f.png", "image/png", "http://u"));
        mockMvc.perform(MockMvcRequestBuilders.get("/medias/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caption").value("c"))
            .andExpect(jsonPath("$.fileName").value("f.png"));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        when(mediaService.getMediaById(1L)).thenReturn(null);
        mockMvc.perform(MockMvcRequestBuilders.get("/medias/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getByIds_returnsOk() throws Exception {
        when(mediaService.getMediaByIds(List.of(1L, 2L))).thenReturn(List.of(
            new MediaVm(1L, "a", "a.png", "image/png", "u1"),
            new MediaVm(2L, "b", "b.png", "image/png", "u2")));
        mockMvc.perform(MockMvcRequestBuilders.get("/medias").param("ids", "1", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getByIds_returns404WhenEmpty() throws Exception {
        when(mediaService.getMediaByIds(List.of(99L))).thenReturn(List.of());
        mockMvc.perform(MockMvcRequestBuilders.get("/medias").param("ids", "99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/medias/5"))
            .andExpect(status().isNoContent());
        verify(mediaService).removeMedia(5L);
    }

    @Test
    void getFile_returnsBinary() throws Exception {
        MediaDto dto = MediaDto.builder()
            .mediaType(MediaType.IMAGE_PNG)
            .content(new ByteArrayInputStream(new byte[] {1, 2, 3}))
            .build();
        when(mediaService.getFile(1L, "x.png")).thenReturn(dto);
        mockMvc.perform(MockMvcRequestBuilders.get("/medias/1/file/x.png"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"x.png\""))
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void create_returnsNoFileMediaVm() throws Exception {
        Media saved = new Media();
        saved.setId(10L);
        saved.setCaption("cap");
        saved.setFileName("out.png");
        saved.setMediaType("image/png");
        when(mediaService.saveMedia(org.mockito.ArgumentMatchers.any(MediaPostVm.class))).thenReturn(saved);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.GREEN.getRGB());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        MockMultipartFile file = new MockMultipartFile(
            "multipartFile", "in.png", "image/png", baos.toByteArray());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/medias")
                .file(file)
                .param("caption", "cap")
                .param("fileNameOverride", "out.png"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.caption").value("cap"));

        verify(mediaService).saveMedia(org.mockito.ArgumentMatchers.argThat(vm ->
            "cap".equals(vm.caption())
                && "out.png".equals(vm.fileNameOverride())
                && vm.multipartFile() != null));
    }
}
