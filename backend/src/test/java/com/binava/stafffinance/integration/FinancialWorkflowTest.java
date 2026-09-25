package com.binava.stafffinance.integration;

import com.binava.stafffinance.member.entity.Member;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.url=jdbc:h2:mem:workflow_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed-demo=true"
})
@AutoConfigureMockMvc
class FinancialWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    JsonNode call(String role, MockHttpServletRequestBuilder request, Object body, int expected) throws Exception {
        request.with(user(role.toLowerCase()).roles(role));
        if (body != null) request.contentType("application/json").content(json.writeValueAsString(body));
        String response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? json.createObjectNode() : json.readTree(response);
    }
    long member() throws Exception {
        return call("INITIATOR", post("/api/members"), Map.ofEntries(
            Map.entry("memberCode", "TEST-" + UUID.randomUUID().toString().substring(0, 8)),
            Map.entry("fullName", "Workflow Test Member"), Map.entry("department", "QA"),
            Map.entry("phone", "123456789"), Map.entry("email", "test@example.com"),
            Map.entry("monthlySavingAmount", 100), Map.entry("membershipStatus", "ACTIVE"),
            Map.entry("riskStatus", "NORMAL"), Map.entry("joiningDate", "2026-01-01")), 200).get("id").asLong();
    }
    void balance(long member, String field, String expected) throws Exception {
        JsonNode value = call("INITIATOR", get("/api/members/" + member), null, 200).get(field);
        assertEquals(0, new BigDecimal(expected).compareTo(value.decimalValue()), field);
    }
    long saving(long member, String role, String endpoint, int amount) throws Exception {
        return call(role, post("/api/savings/" + endpoint), Map.of("memberId", member, "amount", amount, "transactionDate", "2026-09-01", "description", "Test savings transaction"), 200).get("id").asLong();
    }
    void decision(String type, long id, boolean approve) throws Exception {
        call("APPROVER", post("/api/approvals/" + type + "/" + id + "/decision"), Map.of("approve", approve, "remarks", "Test review"), 200);
    }

    @Test
    void savingsApprovalRejectionWithdrawalAndSelfApproval() throws Exception {
        long member = member();
        call("INITIATOR", post("/api/savings/individual"), Map.of("memberId",member,"amount",100,"transactionDate","2026-09-01","description"," "),400);
        call("INITIATOR", post("/api/savings/withdrawals"), Map.of("memberId",member,"amount",100,"transactionDate","2026-09-01","description","Insufficient savings"),400);
        long saving = saving(member, "INITIATOR", "individual", 1000);
        balance(member, "totalSavings", "0");
        call("INITIATOR", post("/api/savings/" + saving + "/submit"), null, 200);
        balance(member, "totalSavings", "0");
        call("INITIATOR", post("/api/savings/" + saving + "/decision"), Map.of("approve", true), 403);
        call("APPROVER", get("/api/approvals"), null, 200);
        decision("SAVING", saving, true);
        balance(member, "totalSavings", "1000");
        long withdrawal = saving(member, "INITIATOR", "withdrawals", 200);
        call("INITIATOR", post("/api/savings/" + withdrawal + "/submit"), null, 200);
        decision("SAVING", withdrawal, true);
        balance(member, "totalSavings", "800");
        long rejected = saving(member, "INITIATOR", "individual", 50);
        call("INITIATOR", post("/api/savings/" + rejected + "/submit"), null, 200);
        decision("SAVING", rejected, false);
        balance(member, "totalSavings", "800");
        long own = saving(member, "ADMIN", "individual", 100);
        call("ADMIN", post("/api/savings/" + own + "/submit"), null, 200);
        call("ADMIN", post("/api/savings/" + own + "/decision"), Map.of("approve", true), 400);
    }

    @Test
    void withdrawalsRecheckBalanceAtApproval() throws Exception {
        long member = member();
        long deposit = saving(member, "INITIATOR", "individual", 1000);
        call("INITIATOR", post("/api/savings/" + deposit + "/submit"), null, 200);
        decision("SAVING", deposit, true);
        long first = saving(member, "INITIATOR", "withdrawals", 700);
        long second = saving(member, "INITIATOR", "withdrawals", 700);
        call("INITIATOR", post("/api/savings/" + first + "/submit"), null, 200);
        call("INITIATOR", post("/api/savings/" + second + "/submit"), null, 200);
        decision("SAVING", first, true);
        call("APPROVER", post("/api/approvals/SAVING/" + second + "/decision"), Map.of("approve", true), 400);
        balance(member, "totalSavings", "300");
    }

    @Test
    void monthlySavingsMustBeDecidedAsBatch() throws Exception {
        long member = member();
        long batch = call("INITIATOR", post("/api/savings/batches"), Map.of("period", "2026-09", "items", List.of(Map.of("memberId", member, "amount", 120))), 200).get("id").asLong();
        JsonNode batchItems = call("APPROVER", get("/api/savings/batches/" + batch + "/items"), null, 200);
        assertEquals(1, batchItems.size());
        assertEquals("September 2026 Monthly Saving",batchItems.get(0).get("description").asText());
        assertEquals("Workflow Test Member", batchItems.get(0).get("memberName").asText());
        assertEquals(member, batchItems.get(0).get("memberId").asLong());
        assertEquals(0, new BigDecimal("120").compareTo(batchItems.get(0).get("amount").decimalValue()));
        long item = batchItems.get(0).get("id").asLong();
        call("INITIATOR", post("/api/savings/" + item + "/submit"), null, 400);
        call("INITIATOR", post("/api/savings/batches/" + batch + "/submit"), null, 200);
        call("APPROVER", post("/api/savings/" + item + "/decision"), Map.of("approve", true), 400);
        decision("SAVINGS_BATCH", batch, true);
        balance(member, "totalSavings", "120");
    }

    @Test
    void categorizedLoanChangesPreservePaidPrincipalAndRequireIndependentApproval() throws Exception {
        long member = member();
        String code = call("INITIATOR", get("/api/members/" + member), null, 200).get("memberCode").asText();
        assertTrue(code.matches("VFC[0-9]{3,}"));
        long category = call("ADMIN", post("/api/loan-categories"), Map.of("name", "Test " + UUID.randomUUID(), "annualRate", 12), 200).get("id").asLong();
        Map<String,Object> application = Map.of("memberId", member, "applicationDate", "2026-09-01", "requestedAmount", 1000, "annualInterestRate", 12, "repaymentMonths", 12, "purpose", "Loan changes test");
        long loan = call("INITIATOR", post("/api/loans?categoryId=" + category), application, 200).get("id").asLong();
        assertEquals(12, call("INITIATOR", get("/api/loans/" + loan), null, 200).get("annualInterestRate").asInt());
        call("INITIATOR", post("/api/loans/" + loan + "/submit"), null, 200);
        call("INITIATOR", post("/api/loans"), application, 400);
        decision("LOAN", loan, true);
        assertEquals("ACTIVE",call("INITIATOR",get("/api/loans/"+loan),null,200).get("loanStatus").asText());
        call("INITIATOR",post("/api/loans/"+loan+"/disburse"),Map.of("disbursementDate","2026-09-01","reference","Removed action"),404);
        long payment = call("INITIATOR", post("/api/loans/repayments"), Map.of("loanId", loan, "amount", 88.85, "paymentDate", java.time.LocalDate.now().toString()), 200).get("id").asLong();
        call("INITIATOR", post("/api/loans/repayments/" + payment + "/submit"), null, 200);
        decision("REPAYMENT", payment, true);
        JsonNode afterPayment = call("INITIATOR", get("/api/loans/" + loan), null, 200);
        assertEquals(0, new BigDecimal("78.85").compareTo(afterPayment.get("paidPrincipal").decimalValue()));
        assertEquals(0, new BigDecimal("10.00").compareTo(afterPayment.get("paidInterest").decimalValue()));
        Map<String,Object> topUp = Map.of("type", "TOP_UP", "amount", 100, "months", 11, "effectiveDate", java.time.LocalDate.now().toString(), "remarks", "Additional funds reference TOP1");
        JsonNode preview = call("INITIATOR", post("/api/loans/" + loan + "/adjustment-preview"), topUp, 200);
        assertEquals(0, new BigDecimal("1021.15").compareTo(preview.get("principal").decimalValue()));
        call("INITIATOR", post("/api/loans/" + loan + "/adjustments"), topUp, 200);
        JsonNode approvals = call("APPROVER", get("/api/approvals"), null, 200);
        boolean changeVisible = false;
        for (JsonNode item : approvals) if (item.get("type").asText().equals("LOAN_CHANGE") && item.get("id").asLong() == loan) changeVisible = true;
        assertTrue(changeVisible);
        call("INITIATOR", post("/api/loans/" + loan + "/adjustments/decision"), Map.of("approve", true), 403);
        decision("LOAN_CHANGE", loan, true);
        JsonNode rows = call("INITIATOR", get("/api/loans/" + loan + "/schedule"), null, 200);
        assertEquals("PENDING", rows.get(0).get("paymentStatus").asText());
        assertEquals(11, rows.size());
        JsonNode history = call("INITIATOR", get("/api/loans/"+loan+"/schedule-history"),null,200);
        assertEquals(2,history.size());
        assertEquals("PAID",history.get(1).get("rows").get(0).get("paymentStatus").asText());
        assertEquals(12,history.get(1).get("rows").size());
        assertEquals(java.time.LocalDate.now().plusMonths(1).toString(), rows.get(0).get("dueDate").asText());
        Map<String,Object> reschedule = Map.of("type", "RESCHEDULE", "amount", 0, "months", 18, "effectiveDate", java.time.LocalDate.now().toString(), "remarks", "Extend term");
        call("ADMIN", post("/api/loans/" + loan + "/adjustments"), reschedule, 200);
        call("ADMIN", post("/api/loans/" + loan + "/adjustments/decision"), Map.of("approve", true), 400);
        call("APPROVER", post("/api/loans/" + loan + "/adjustments/decision"), Map.of("approve", true), 200);
        assertEquals(18, call("INITIATOR", get("/api/loans/" + loan + "/schedule"), null, 200).size());
        Map<String,Object> restructure = Map.of("type", "RESTRUCTURE", "amount", 50, "months", 6, "effectiveDate", java.time.LocalDate.now().toString(), "remarks", "Additional principal and shorter term");
        call("INITIATOR", post("/api/loans/" + loan + "/adjustments"), restructure, 200);
        call("APPROVER", post("/api/loans/" + loan + "/adjustments/decision"), Map.of("approve", true), 200);
        JsonNode result = call("INITIATOR", get("/api/loans/" + loan), null, 200);
        assertEquals(0, new BigDecimal("1150").compareTo(result.get("approvedAmount").decimalValue()));
        assertEquals(0, result.get("totalPayable").decimalValue().subtract(result.get("amountRepaid").decimalValue()).compareTo(result.get("outstandingBalance").decimalValue()));
        assertEquals(6, call("INITIATOR", get("/api/loans/" + loan + "/schedule"), null, 200).size());
        assertEquals(4,call("INITIATOR",get("/api/loans/"+loan+"/schedule-history"),null,200).size());
    }

    @Test
    void loanApprovalActivatesAndPaymentsCloseLoanExactlyOnce() throws Exception {
        long member = member();
        long loan = call("INITIATOR", post("/api/loans"), Map.of("memberId", member, "applicationDate", "2026-09-01", "requestedAmount", 1000, "annualInterestRate", 12, "repaymentMonths", 12, "purpose", "Workflow test"), 200).get("id").asLong();
        call("INITIATOR", post("/api/loans/" + loan + "/submit"), null, 200);
        decision("LOAN", loan, true);
        balance(member, "outstandingLoans", "1066.19");
        long payment = call("INITIATOR", post("/api/loans/repayments"), Map.of("loanId", loan, "amount", 100.25, "paymentDate", java.time.LocalDate.now().toString()), 200).get("id").asLong();
        call("INITIATOR", post("/api/loans/repayments/" + payment + "/submit"), null, 200);
        balance(member, "outstandingLoans", "1066.19");
        decision("REPAYMENT", payment, true);
        balance(member, "outstandingLoans", "965.94");
        call("APPROVER", post("/api/loans/repayments/" + payment + "/decision"), Map.of("approve", true), 400);
        long finalPayment=call("INITIATOR",post("/api/loans/repayments"),Map.of("loanId",loan,"amount",965.94,"paymentDate","2026-10-01"),200).get("id").asLong();
        call("INITIATOR",post("/api/loans/repayments/"+finalPayment+"/submit"),null,200);
        decision("REPAYMENT",finalPayment,true);
        balance(member, "outstandingLoans", "0");
        JsonNode result = call("INITIATOR", get("/api/loans/" + loan), null, 200);
        assertEquals("CLOSED", result.get("loanStatus").asText());
        assertEquals(0, new BigDecimal("1066.19").compareTo(result.get("amountRepaid").decimalValue()));
        JsonNode schedule = call("INITIATOR", get("/api/loans/" + loan + "/schedule"), null, 200);
        assertEquals(12, schedule.size());
        for (JsonNode row : schedule) assertEquals("PAID", row.get("paymentStatus").asText());
    }
}

