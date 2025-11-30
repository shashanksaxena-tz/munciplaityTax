package com.munitax.rules.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.rules.dto.CreateRuleRequest;
import com.munitax.rules.dto.RuleResponse;
import com.munitax.rules.dto.UpdateRuleRequest;
import com.munitax.rules.service.RuleFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RuleConfigController.class)
class RuleConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RuleFacadeService ruleFacadeService;

    private RuleResponse mockRuleResponse;

    @BeforeEach
    void setUp() {
        mockRuleResponse = new RuleResponse();
        mockRuleResponse.setRuleId("rule-123");
        mockRuleResponse.setRuleCode("TAX_RATE");
        mockRuleResponse.setRuleName("Dublin Tax Rate");
        mockRuleResponse.setStatus("ACTIVE");
        mockRuleResponse.setEffectiveFrom(LocalDate.now());
    }

    @Test
    @WithMockUser(roles = "TAX_ADMINISTRATOR")
    void testCreateRule_Success() throws Exception {
        when(ruleFacadeService.createRule(any(CreateRuleRequest.class)))
                .thenReturn(mockRuleResponse);

        CreateRuleRequest request = new CreateRuleRequest();
        request.setRuleCode("TAX_RATE");
        request.setRuleName("Dublin Tax Rate");
        request.setRateValue(BigDecimal.valueOf(0.025));
        request.setEffectiveFrom(LocalDate.now());

        mockMvc.perform(post("/api/rules")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleId").value("rule-123"))
                .andExpect(jsonPath("$.ruleCode").value("TAX_RATE"));
    }

    @Test
    @WithMockUser(roles = "TAX_ADMINISTRATOR")
    void testUpdateRule_Success() throws Exception {
        mockRuleResponse.setStatus("UPDATED");
        when(ruleFacadeService.updateRule(anyString(), any(UpdateRuleRequest.class)))
                .thenReturn(mockRuleResponse);

        UpdateRuleRequest request = new UpdateRuleRequest();
        request.setRuleName("Updated Dublin Tax Rate");
        request.setRateValue(BigDecimal.valueOf(0.030));

        mockMvc.perform(put("/api/rules/rule-123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPDATED"));
    }

    @Test
    @WithMockUser(roles = "TAX_ADMINISTRATOR")
    void testApproveRule_Success() throws Exception {
        mockRuleResponse.setStatus("APPROVED");
        when(ruleFacadeService.approveRule(anyString(), anyString()))
                .thenReturn(mockRuleResponse);

        mockMvc.perform(post("/api/rules/rule-123/approve")
                .with(csrf())
                .param("approverId", "admin-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateRule_Unauthorized() throws Exception {
        CreateRuleRequest request = new CreateRuleRequest();
        request.setRuleCode("TAX_RATE");

        mockMvc.perform(post("/api/rules")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
