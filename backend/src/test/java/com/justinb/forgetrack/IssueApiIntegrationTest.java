package com.justinb.forgetrack;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IssueApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsFiltersAndAggregatesIssues() throws Exception {
        String bootstrap = mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.length()").value(greaterThanOrEqualTo(1)))
                .andReturn().getResponse().getContentAsString();

        String projectId = JsonPath.read(bootstrap, "$.projects[0].id");
        String reporterId = JsonPath.read(bootstrap, "$.members[0].id");

        String body = """
                {
                  "projectId": "%s",
                  "title": "Protect per-project issue numbering",
                  "description": "Reserve issue numbers inside a database transaction.",
                  "type": "TASK",
                  "priority": "HIGH",
                  "reporterId": "%s",
                  "assigneeId": "%s"
                }
                """.formatted(projectId, reporterId, reporterId);

        mockMvc.perform(post("/api/issues").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("FORGE-6"))
                .andExpect(jsonPath("$.status").value("TODO"));

        mockMvc.perform(get("/api/projects/{projectId}/issues", projectId)
                        .param("priority", "HIGH")
                        .param("q", "numbering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Protect per-project issue numbering"));

        mockMvc.perform(get("/api/projects/{projectId}/dashboard", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.byStatus.TODO").value(3));
    }

    @Test
    void rejectsInvalidIssueRequestsWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.validationErrors.projectId").exists())
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }
}
