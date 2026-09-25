package com.binava.stafffinance.integration;

import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.util.*;
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

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:enhancement_test;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","app.seed-demo=true"})
@AutoConfigureMockMvc
class LoanEnhancementTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.binava.stafffinance.loan.service.LoanScheduleUpgradeService upgrade;
    JsonNode call(String role,MockHttpServletRequestBuilder request,Object body,int expected) throws Exception {
        request.with(user(role.toLowerCase()).roles(role));
        if(body!=null) request.contentType("application/json").content(json.writeValueAsString(body));
        String result=mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return result.isBlank()?json.createObjectNode():json.readTree(result);
    }
    long category() throws Exception { return call("ADMIN",post("/api/loan-categories"),Map.of("name","Test "+UUID.randomUUID(),"annualRate",12,"description","Test category","active",true),200).get("id").asLong(); }
    long member() throws Exception { return call("INITIATOR",post("/api/members"),Map.of("fullName","Enhancement Member","department","QA","phone","1234567","email","enhancement@example.org","monthlySavingAmount",100,"membershipStatus","ACTIVE","riskStatus","NORMAL","joiningDate","2026-01-01"),200).get("id").asLong(); }
    Map<String,Object> application(long member) { return Map.of("memberId",member,"applicationDate","2026-09-01","firstInstallmentDate","2026-10-31","requestedAmount",1000,"annualInterestRate",12,"repaymentMonths",12,"purpose","Enhancement test"); }
    long activeLoan() throws Exception {
        long id=call("INITIATOR",post("/api/loans?categoryId="+category()),application(member()),200).get("id").asLong();
        call("INITIATOR",post("/api/loans/"+id+"/submit"),null,200);
        decide("LOAN",id,true);
        return id;
    }
    void decide(String type,long id,boolean approve) throws Exception { call("APPROVER",post("/api/approvals/"+type+"/"+id+"/decision"),Map.of("approve",approve,"remarks","Reviewed"),200); }
    JsonNode loan(long id) throws Exception { return call("INITIATOR",get("/api/loans/"+id),null,200); }
    JsonNode rows(long id) throws Exception { return call("INITIATOR",get("/api/loans/"+id+"/schedule"),null,200); }
    void amount(String expected,JsonNode actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual.decimalValue())); }

    @Test void alreadyApprovedLoansActivateWithoutDisbursementAndKeepTheirSchedule() throws Exception {
        long id=activeLoan();
        JsonNode before=rows(id);
        jdbc.update("update loans set loan_status='APPROVED', disbursement_date=null where id=?",id);
        upgrade.upgrade();
        assertEquals("ACTIVE",loan(id).get("loanStatus").asText());
        assertEquals(before,rows(id));
        upgrade.upgrade();
        assertEquals(1,call("INITIATOR",get("/api/loans/"+id+"/schedule-history"),null,200).size());
    }

    @Test void datesAreAuthoritativeAndCategoryChangesDoNotChangeLoans() throws Exception {
        long category=category(),member=member();
        JsonNode preview=call("INITIATOR",post("/api/loans/preview?categoryId="+category),application(member),200);
        assertEquals("2026-10-31",preview.get("rows").get(0).get("dueDate").asText());
        assertEquals("2026-11-30",preview.get("rows").get(1).get("dueDate").asText());
        assertEquals("2026-12-31",preview.get("rows").get(2).get("dueDate").asText());
        long loan=call("INITIATOR",post("/api/loans?categoryId="+category),application(member),200).get("id").asLong();
        assertEquals(12,rows(loan).size());
        call("ADMIN",put("/api/loan-categories/"+category),Map.of("name","Inactive "+UUID.randomUUID(),"annualRate",20,"active",false),200);
        amount("12",loan(loan).get("annualInterestRate"));
        call("INITIATOR",post("/api/loans?categoryId="+category),application(member()),400);
        for(var row:call("INITIATOR",get("/api/loan-categories?activeOnly=true"),null,200)) assertNotEquals(category,row.get("id").asLong());
    }

    @Test void settlementChargesOneTermInterestAndPreservesHistory() throws Exception {
        long id=activeLoan();
        JsonNode quote=call("INITIATOR",get("/api/loans/"+id+"/settlement-quote"),null,200);
        amount("1000",quote.get("remainingPrincipal")); amount("10",quote.get("applicableInterest")); amount("1010",quote.get("settlementAmount"));
        long payment=call("INITIATOR",post("/api/loans/repayments"),Map.of("loanId",id,"paymentType","FULL_SETTLEMENT","amount",1010,"paymentDate",java.time.LocalDate.now().toString(),"remarks","Full settlement"),200).get("id").asLong();
        call("INITIATOR",post("/api/loans/repayments/"+payment+"/submit"),null,200);
        amount("1066.19",loan(id).get("outstandingBalance"));
        decide("FULL_SETTLEMENT",payment,false);
        amount("1066.19",loan(id).get("outstandingBalance"));
        call("INITIATOR",post("/api/loans/repayments/"+payment+"/submit"),null,200);
        decide("FULL_SETTLEMENT",payment,true);
        JsonNode closed=loan(id);
        assertEquals("CLOSED",closed.get("loanStatus").asText()); amount("0",closed.get("outstandingBalance")); amount("0",closed.get("remainingPrincipal"));
        amount("1000",closed.get("paidPrincipal")); amount("10",closed.get("paidInterest")); amount("1010",closed.get("amountRepaid"));
        for(var row:rows(id)) assertEquals("SETTLED",row.get("paymentStatus").asText());
        assertEquals(1,call("INITIATOR",get("/api/loans/"+id+"/schedule-history"),null,200).size());
        call("APPROVER",post("/api/approvals/FULL_SETTLEMENT/"+payment+"/decision"),Map.of("approve",true),400);
    }

    @Test void monthlyBatchShowsAllocationRejectsDuplicatesAndRollsBackOnStaleItem() throws Exception {
        long first=activeLoan(),second=activeLoan();
        long firstRow=rows(first).get(0).get("id").asLong(),secondRow=rows(second).get(0).get("id").asLong();
        JsonNode due=call("INITIATOR",get("/api/loans/due-installments?period=2026-10"),null,200);
        assertTrue(due.size()>=2);
        long batch=call("INITIATOR",post("/api/loans/repayment-batches"),Map.of("period","2026-10","remarks","October deductions","items",List.of(Map.of("loanId",first,"scheduleId",firstRow,"amount",88.85),Map.of("loanId",second,"scheduleId",secondRow,"amount",88.85))),200).get("id").asLong();
        JsonNode items=call("APPROVER",get("/api/loans/repayment-batches/"+batch+"/items"),null,200);
        amount("78.85",items.get(0).get("principalPaid")); amount("10",items.get(0).get("interestPaid")); assertTrue(items.get(0).get("memberCode").asText().startsWith("VFC"));
        call("INITIATOR",post("/api/loans/repayment-batches/"+batch+"/submit"),null,200);
        long other=call("INITIATOR",post("/api/loans/repayments"),Map.of("loanId",second,"scheduleId",secondRow,"paymentType","INSTALLMENT","amount",88.85,"paymentDate",java.time.LocalDate.now().toString()),200).get("id").asLong();
        call("INITIATOR",post("/api/loans/repayments/"+other+"/submit"),null,200); decide("REPAYMENT",other,true);
        call("APPROVER",post("/api/loans/repayment-batches/"+batch+"/decision"),Map.of("approve",true),400);
        amount("1066.19",loan(first).get("outstandingBalance"));
        for(var item:call("APPROVER",get("/api/loans/repayment-batches/"+batch+"/items"),null,200)) assertEquals("PENDING_APPROVAL",item.get("status").asText());
        call("INITIATOR",post("/api/loans/repayments"),Map.of("loanId",second,"scheduleId",secondRow,"paymentType","INSTALLMENT","amount",88.85,"paymentDate",java.time.LocalDate.now().toString()),400);
        decide("REPAYMENT_BATCH",batch,false);
        long good=call("INITIATOR",post("/api/loans/repayment-batches"),Map.of("period","2026-10","remarks","Corrected October deductions","items",List.of(Map.of("loanId",first,"scheduleId",firstRow,"amount",88.85))),200).get("id").asLong();
        call("INITIATOR",post("/api/loans/repayment-batches/"+good+"/submit"),null,200); decide("REPAYMENT_BATCH",good,true);
        amount("977.34",loan(first).get("outstandingBalance")); assertEquals(1,loan(first).get("installmentsPaid").asInt());
    }
}
