package com.workflow.approval_workflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class RestAccessDeniedHandlerTest {

    @Test
    void writesJsonForbiddenBody() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(mapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/requests/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("path").asText()).isEqualTo("/requests/1");
    }
}
