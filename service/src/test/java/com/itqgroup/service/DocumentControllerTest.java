package com.itqgroup.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itqgroup.service.dto.DocumentCreateRequest;
import com.itqgroup.service.dto.DocumentActionRequest;
import com.itqgroup.service.model.Document;
import com.itqgroup.service.model.enums.DocumentStatus;
import com.itqgroup.service.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    private Document testDocument;

    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setAuthor("Controller Test");
        testDocument.setTitle("Test Document");
        testDocument = documentRepository.save(testDocument);
    }

    @Test
    void createDocument_ShouldReturnCreatedDocument() throws Exception {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setAuthor("Test Author");
        request.setTitle("Test Title");

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.uniqueNumber").isString())
                .andExpect(jsonPath("$.author").value("Test Author"))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getDocument_ShouldReturnDocumentWithHistory() throws Exception {
        mockMvc.perform(get("/api/documents/{id}", testDocument.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDocument.getId()))
                .andExpect(jsonPath("$.author").value("Controller Test"))
                .andExpect(jsonPath("$.history").isArray());
    }

    @Test
    void getDocuments_WithIds_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .param("ids", testDocument.getId().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testDocument.getId()));
    }

    @Test
    void submitDocuments_WithValidIds_ShouldReturnSuccess() throws Exception {
        DocumentActionRequest request = new DocumentActionRequest();
        request.setIds(List.of(testDocument.getId()));
        request.setUserId("test-user");
        request.setComment("submit test");

        mockMvc.perform(post("/api/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(testDocument.getId()))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void searchDocuments_WithFilters_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/api/documents/search")
                        .param("status", "DRAFT")
                        .param("author", "Controller")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}