package io.github.pankajsinghnirmal.accessguard.audit.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointAuthorizationIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldReturn401WhenSecureEndpointIsCalledWithoutToken() throws Exception {
        mvc.perform(get("/secure/test/ping"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenInternalEndpointIsCalledWithoutInternalRole() throws Exception {
        mvc.perform(get("/internal/test/ping")
                   .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OTHER"))))
           .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200WhenSecureEndpointIsCalledWithValidToken() throws Exception {
        mvc.perform(get("/secure/test/ping")
                   .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OTHER"))))
           .andExpect(status().isOk());
    }
}