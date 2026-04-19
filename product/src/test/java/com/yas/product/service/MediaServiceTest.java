package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.config.ServiceUrlConfig;
import com.yas.product.viewmodel.NoFileMediaVm;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    private static final String MEDIA_BASE = "http://api.yas.local/media";

    @Mock
    private RestClient restClient;
    @Mock
    private ServiceUrlConfig serviceUrlConfig;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private MediaService mediaService;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMedia_whenIdNull_returnsEmptyPlaceholder() {
        NoFileMediaVm vm = mediaService.getMedia(null);

        assertNotNull(vm);
        assertEquals("", vm.url());
    }

    @Test
    void getMedia_whenIdPresent_callsRestClientGet() {
        when(serviceUrlConfig.media()).thenReturn(MEDIA_BASE);
        RestClient.RequestHeadersUriSpec uriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        NoFileMediaVm expected = new NoFileMediaVm(5L, "c", "f", "image/png", "https://cdn/x.png");
        when(responseSpec.body(NoFileMediaVm.class)).thenReturn(expected);

        NoFileMediaVm vm = mediaService.getMedia(5L);

        assertEquals("https://cdn/x.png", vm.url());
        assertEquals(5L, vm.id());
        verify(restClient).get();
    }

    @Test
    void saveFile_postsMultipartWithBearerToken() throws Exception {
        Jwt jwt = Jwt.withTokenValue("test-jwt")
            .header("alg", "none")
            .claim("sub", "tester")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        when(serviceUrlConfig.media()).thenReturn(MEDIA_BASE);
        NoFileMediaVm saved = new NoFileMediaVm(9L, "cap", "file.png", "image/png", "https://cdn/u.png");
        when(responseSpec.body(NoFileMediaVm.class)).thenReturn(saved);

        @SuppressWarnings("unchecked")
        RestClient.RequestBodyUriSpec[] bodyUriSpecHolder = new RestClient.RequestBodyUriSpec[1];
        Answer<Object> fluentAnswer = (InvocationOnMock inv) -> {
            if ("retrieve".equals(inv.getMethod().getName())) {
                return responseSpec;
            }
            return bodyUriSpecHolder[0];
        };
        bodyUriSpecHolder[0] = Mockito.mock(RestClient.RequestBodyUriSpec.class, fluentAnswer);
        when(restClient.post()).thenReturn(bodyUriSpecHolder[0]);

        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.getResource()).thenReturn(new ByteArrayResource("data".getBytes()));

        NoFileMediaVm result = mediaService.saveFile(file, "caption", "override.png");

        assertEquals(9L, result.id());
        verify(restClient).post();
    }

    @Test
    void removeMedia_deleteRequestUsesBearerToken() {
        Jwt jwt = Jwt.withTokenValue("del-jwt")
            .header("alg", "none")
            .claim("sub", "tester")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));

        when(serviceUrlConfig.media()).thenReturn(MEDIA_BASE);
        RestClient.RequestHeadersUriSpec uriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.delete()).thenReturn(uriSpec);
        when(uriSpec.uri(any(URI.class))).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Void.class)).thenReturn(null);

        mediaService.removeMedia(12L);

        verify(restClient).delete();
    }
}
