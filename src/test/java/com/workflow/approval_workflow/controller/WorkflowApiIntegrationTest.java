package com.workflow.approval_workflow.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end tests: real Spring context, real H2 database seeded by data.sql, real JWTs and real
 * Spring Security. Seeded users: alice/bob (REQUESTER), carol/dave (APPROVER), admin (ADMIN).
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowApiIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JsonMapper jsonMapper;

    // ------------------------------------------------------------------ helpers

    private String login(String username) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private long createRequest(String token, String type) throws Exception {
        MvcResult result = mvc.perform(post("/requests")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("type", type, "description", "test request"))))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions act(String token, long id, String action) throws Exception {
        return mvc.perform(post("/requests/" + id + "/" + action).header("Authorization", bearer(token)));
    }

    private ResultActions fetch(String token, long id) throws Exception {
        return mvc.perform(get("/requests/" + id).header("Authorization", bearer(token)));
    }

    private ResultActions history(String token, long id) throws Exception {
        return mvc.perform(get("/requests/history/" + id).header("Authorization", bearer(token)));
    }

    // ------------------------------------------------------------------ authentication

    @Test
    void loginReturnsJwtAndRole() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("APPROVER"));
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginWithBlankFieldsFailsValidation() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)));
    }

    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/requests/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/requests/1/approve")).andExpect(status().isUnauthorized());
        mvc.perform(post("/requests/1/reject")).andExpect(status().isUnauthorized());
        mvc.perform(get("/requests/history/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/requests/1").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    // ------------------------------------------------------------------ create + validation

    @Test
    void requesterCreatesRequestAndWorkflowIsInitialisedFromDatabase() throws Exception {
        String alice = login("alice");

        mvc.perform(post("/requests").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"leave\",\"description\":\"Holiday\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("LEAVE"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdBy").value("alice"))
                .andExpect(jsonPath("$.currentStep.stepOrder").value(1))
                .andExpect(jsonPath("$.currentStep.role").value("APPROVER"));
    }

    @Test
    void onlyRequesterRoleCanCreateRequests() throws Exception {
        for (String user : new String[] {"carol", "admin"}) {
            mvc.perform(post("/requests").header("Authorization", bearer(login(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"LEAVE\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void unknownWorkflowTypeIsRejected() throws Exception {
        mvc.perform(post("/requests").header("Authorization", bearer(login("alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TRAVEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No approval workflow is configured for request type 'TRAVEL'"));
    }

    @Test
    void payloadValidationErrorsAreReportedPerField() throws Exception {
        String alice = login("alice");

        mvc.perform(post("/requests").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"no type\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        mvc.perform(post("/requests").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"bad type!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("type: type may only contain letters, digits, '_' and '-'"));

        mvc.perform(post("/requests").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"LEAVE\",\"description\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/requests").header("Authorization", bearer(login("alice")))
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable JSON request body"));
    }

    @Test
    void tooLongApprovalCommentIsBadRequest() throws Exception {
        long id = createRequest(login("alice"), "LEAVE");

        mvc.perform(post("/requests/" + id + "/approve").header("Authorization", bearer(login("carol")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comments\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ multi-step workflows

    @Test
    void leaveRequestMovesThroughManagerThenAdminAndIsFullyAudited() throws Exception {
        String alice = login("alice");
        String carol = login("carol");
        String admin = login("admin");
        long id = createRequest(alice, "LEAVE");

        // step 1 (APPROVER) -> moves to step 2 (ADMIN)
        mvc.perform(post("/requests/" + id + "/approve").header("Authorization", bearer(carol))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comments\":\"fine by me\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currentStep.stepOrder").value(2))
                .andExpect(jsonPath("$.currentStep.role").value("ADMIN"));

        // an APPROVER can no longer act: step 2 belongs to ADMIN
        act(carol, id, "approve").andExpect(status().isForbidden());

        // step 2 (ADMIN) -> workflow complete
        act(admin, id, "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.currentStep").doesNotExist());

        // finished workflow: any further action is an invalid transition
        act(carol, id, "approve").andExpect(status().isConflict());
        act(carol, id, "reject").andExpect(status().isConflict());
        act(admin, id, "approve").andExpect(status().isConflict());

        history(alice, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].actionBy").value("alice"))
                .andExpect(jsonPath("$[1].action").value("APPROVED"))
                .andExpect(jsonPath("$[1].actionBy").value("carol"))
                .andExpect(jsonPath("$[1].stepOrder").value(1))
                .andExpect(jsonPath("$[1].comments").value("fine by me"))
                .andExpect(jsonPath("$[2].action").value("APPROVED"))
                .andExpect(jsonPath("$[2].actionBy").value("admin"));
    }

    @Test
    void expenseRequestUsesItsOwnThreeStepWorkflowFromTheDatabase() throws Exception {
        String alice = login("alice");
        long id = createRequest(alice, "EXPENSE");

        act(login("carol"), id, "approve")
                .andExpect(jsonPath("$.currentStep.stepOrder").value(2))
                .andExpect(jsonPath("$.currentStep.role").value("APPROVER"));
        act(login("dave"), id, "approve")
                .andExpect(jsonPath("$.currentStep.stepOrder").value(3))
                .andExpect(jsonPath("$.currentStep.role").value("ADMIN"));
        act(login("admin"), id, "approve")
                .andExpect(jsonPath("$.status").value("APPROVED"));

        history(alice, id).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void rejectTerminatesTheWorkflow() throws Exception {
        String alice = login("alice");
        String carol = login("carol");
        long id = createRequest(alice, "EXPENSE");

        mvc.perform(post("/requests/" + id + "/reject").header("Authorization", bearer(carol))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comments\":\"over budget\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.currentStep").doesNotExist());

        act(carol, id, "approve").andExpect(status().isConflict());
        act(carol, id, "reject").andExpect(status().isConflict());
        fetch(alice, id).andExpect(jsonPath("$.status").value("REJECTED"));
        history(alice, id)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].action").value("REJECTED"))
                .andExpect(jsonPath("$[1].comments").value("over budget"));
    }

    // ------------------------------------------------------------------ security rules

    @Test
    void requesterCannotApproveOrRejectTheirOwnRequest() throws Exception {
        String alice = login("alice");
        long id = createRequest(alice, "LEAVE");

        act(alice, id, "approve").andExpect(status().isForbidden());
        act(alice, id, "reject").andExpect(status().isForbidden());
        fetch(alice, id).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void approverCannotActOnAStepAssignedToAnotherRole() throws Exception {
        long id = createRequest(login("alice"), "LEAVE");
        String carol = login("carol");
        act(carol, id, "approve").andExpect(status().isOk());   // now waiting for ADMIN

        act(login("dave"), id, "reject")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Step 2 of this request requires role ADMIN; your role is APPROVER"));
    }

    @Test
    void adminCanOverrideApproveAtAnyStep() throws Exception {
        String alice = login("alice");
        long id = createRequest(alice, "EXPENSE");   // waiting at step 1 (APPROVER)

        act(login("admin"), id, "approve")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        history(alice, id)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].action").value("OVERRIDE_APPROVED"))
                .andExpect(jsonPath("$[1].actionBy").value("admin"));
    }

    @Test
    void adminCanOverrideRejectAtAnyStep() throws Exception {
        String alice = login("alice");
        long id = createRequest(alice, "EXPENSE");

        act(login("admin"), id, "reject")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        history(alice, id).andExpect(jsonPath("$[1].action").value("OVERRIDE_REJECTED"));
    }

    // ------------------------------------------------------------------ read access

    @Test
    void requesterSeesOwnRequestButNotSomeoneElses() throws Exception {
        long id = createRequest(login("alice"), "LEAVE");

        fetch(login("alice"), id).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        fetch(login("bob"), id).andExpect(status().isForbidden());
    }

    @Test
    void approverAndAdminCanViewAnyRequest() throws Exception {
        long id = createRequest(login("alice"), "LEAVE");

        fetch(login("carol"), id).andExpect(status().isOk());
        fetch(login("admin"), id).andExpect(status().isOk());
    }

    @Test
    void historyIsForRequesterOwnerAndAdminOnly() throws Exception {
        long id = createRequest(login("alice"), "LEAVE");

        history(login("alice"), id).andExpect(status().isOk());
        history(login("admin"), id).andExpect(status().isOk());
        history(login("bob"), id).andExpect(status().isForbidden());     // another requester
        history(login("carol"), id).andExpect(status().isForbidden());   // APPROVER role is not allowed
    }

    // ------------------------------------------------------------------ errors

    @Test
    void unknownRequestIdsAreNotFound() throws Exception {
        fetch(login("alice"), 987654).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Request 987654 not found"));
        act(login("carol"), 987654, "approve").andExpect(status().isNotFound());
        act(login("carol"), 987654, "reject").andExpect(status().isNotFound());
        history(login("admin"), 987654).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        mvc.perform(get("/requests/abc").header("Authorization", bearer(login("alice"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void unknownPathIsNotFoundAndWrongMethodIsRejected() throws Exception {
        String alice = login("alice");

        mvc.perform(get("/does-not-exist").header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/requests/1").header("Authorization", bearer(alice)))
                .andExpect(status().isMethodNotAllowed());
    }
}
