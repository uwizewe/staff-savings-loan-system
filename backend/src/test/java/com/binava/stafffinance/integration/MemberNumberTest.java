package com.binava.stafffinance.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:member_number_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","app.seed-demo=true"})
@AutoConfigureMockMvc
class MemberNumberTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    Map<String,Object> payload() {
        return new HashMap<>(Map.of("memberCode","MANUAL-ID", "fullName","Number Test", "department","QA", "phone","1234567", "email","number@example.org", "monthlySavingAmount",100,"membershipStatus","ACTIVE","riskStatus","NORMAL","joiningDate","2026-01-01"));
    }

    JsonNode create() throws Exception {
        return json.readTree(mvc.perform(post("/api/members").with(user("initiator").roles("INITIATOR"))
            .contentType("application/json").content(json.writeValueAsString(payload())))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test void numbersArePaddedContinueExistingSequenceAndStayUniqueUnderConcurrentRegistration() throws Exception {
        JsonNode first=create();
        assertEquals("VFC001",first.get("memberCode").asText());
        assertEquals("VFC002",create().get("memberCode").asText());
        JsonNode updated=json.readTree(mvc.perform(put("/api/members/"+first.get("id").asLong()).with(user("initiator").roles("INITIATOR"))
            .contentType("application/json").content(json.writeValueAsString(payload())))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("VFC001",updated.get("memberCode").asText());
        jdbc.update("UPDATE members SET member_code='VFC-999' WHERE id=?",first.get("id").asLong());
        assertEquals("VFC1000",create().get("memberCode").asText());

        var results=new HashSet<String>();
        var executor=Executors.newFixedThreadPool(6);
        try {
            var tasks=new ArrayList<Callable<String>>();
            for(int i=0;i<12;i++) tasks.add(()->create().get("memberCode").asText());
            for(var result:executor.invokeAll(tasks)) results.add(result.get());
        } finally { executor.shutdownNow(); }
        assertEquals(12,results.size());
        for(int i=1001;i<=1012;i++) assertTrue(results.contains("VFC"+i));

        var invalid=payload(); invalid.put("membershipStatus","LEFT");
        mvc.perform(post("/api/members").with(user("initiator").roles("INITIATOR"))
            .contentType("application/json").content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
        assertEquals("VFC1013",create().get("memberCode").asText());
    }
}
