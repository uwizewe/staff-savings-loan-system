package com.binava.stafffinance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:loan_endpoint_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed-demo=true"
})
@AutoConfigureMockMvc
class LoanEndpointTest {
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "initiator", roles = "INITIATOR")
    void reportSourcesIncludeRelatedNames() throws Exception {
        mvc.perform(get("/api/savings")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").isNumber())
                .andExpect(jsonPath("$[0].memberName").isNotEmpty());
        mvc.perform(get("/api/finance")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "initiator", roles = "INITIATOR")
    void loanWorkspaceEndpointsLoadPersistedRecords() throws Exception {
        for (String path : new String[]{"/api/loans", "/api/loans/repayments", "/api/members", "/api/loans/repayment-batches"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/loans/repayments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").isNumber())
                .andExpect(jsonPath("$[0].memberName").isNotEmpty())
                .andExpect(jsonPath("$[0].loanNumber").isNotEmpty());
    }
}
