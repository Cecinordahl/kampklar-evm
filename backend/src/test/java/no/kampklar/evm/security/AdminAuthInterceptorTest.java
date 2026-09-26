package no.kampklar.evm.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import no.kampklar.evm.controller.AdminMatchController;
import no.kampklar.evm.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMatchController.class)
@TestPropertySource(properties = {"app.admin-uid=admin-uid", "app.frontend-origin=https://kampklar-evm.vercel.app"})
class AdminAuthInterceptorTest {

    private static final String ORIGIN = "https://kampklar-evm.vercel.app";
    private static final String BODY = """
            {"competitionId":"unl","groupId":"a1","homeTeamId":"norway","awayTeamId":"spain",
             "kickoff":"2026-09-24T18:45:00Z","status":"SCHEDULED"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private MatchService matchService;

    @Autowired
    private ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    @Test
    void rejectsARequestWithoutAToken() throws Exception {
        saveMatch(null).andExpect(status().isUnauthorized());

        verify(matchService, never()).save(any());
    }

    @Test
    void rejectsAnInvalidToken() throws Exception {
        when(firebaseAuth.verifyIdToken("forged", true)).thenThrow(mock(FirebaseAuthException.class));

        saveMatch("Bearer forged").andExpect(status().isUnauthorized());

        verify(matchService, never()).save(any());
    }

    @Test
    void rejectsAValidTokenFromSomeoneOtherThanTheAdmin() throws Exception {
        givenTokenFor("fan-token", "some-fan-uid");

        saveMatch("Bearer fan-token").andExpect(status().isForbidden());

        verify(matchService, never()).save(any());
    }

    @Test
    void letsTheAdminThrough() throws Exception {
        givenTokenFor("admin-token", "admin-uid");
        when(matchService.save(any())).thenReturn(List.of());

        saveMatch("Bearer admin-token").andExpect(status().isOk());

        verify(matchService).save(any());
    }

    @Test
    void rejectionsCarryCorsHeadersSoTheBrowserSeesTheRealStatus() throws Exception {
        saveMatch(null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
    }

    @Test
    void letsCorsPreflightThroughWithoutAToken() throws Exception {
        mockMvc.perform(options("/admin/matches/m1")
                        .header(HttpHeaders.ORIGIN, ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN));
    }

    @Test
    void rejectsEveryoneWhenNoAdminUidIsConfigured() throws Exception {
        AdminAuthInterceptor interceptor = new AdminAuthInterceptor(firebaseAuthProvider, "");
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/admin/matches/m1");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    private void givenTokenFor(String rawToken, String uid) throws FirebaseAuthException {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(firebaseAuth.verifyIdToken(rawToken, true)).thenReturn(token);
    }

    private ResultActions saveMatch(String authorization) throws Exception {
        var request = put("/admin/matches/m1")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY);
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return mockMvc.perform(request);
    }
}
