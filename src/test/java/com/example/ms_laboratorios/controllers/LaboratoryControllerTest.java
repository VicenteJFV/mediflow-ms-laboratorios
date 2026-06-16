package com.example.ms_laboratorios.controllers;

import com.example.ms_laboratorios.models.LabOrder;
import com.example.ms_laboratorios.models.LabResult;
import com.example.ms_laboratorios.services.LaboratoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LaboratoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LaboratoryService laboratoryService;

    @InjectMocks
    private LaboratoryController laboratoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(laboratoryController).build();
    }

    @Test
    void createOrder_Success_ShouldReturnCreated() throws Exception {
        // Arrange
        LabOrder createdOrder = LabOrder.builder()
                .id(1L)
                .patientId(101L)
                .doctorId(202L)
                .studyType("Blood Test")
                .status("PENDING")
                .build();

        when(laboratoryService.createOrder(eq(101L), eq(202L), eq("Blood Test"))).thenReturn(createdOrder);

        // Act & Assert
        mockMvc.perform(post("/api/laboratory/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":101,\"doctorId\":202,\"studyType\":\"Blood Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientId").value(101))
                .andExpect(jsonPath("$.doctorId").value(202))
                .andExpect(jsonPath("$.studyType").value("Blood Test"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(laboratoryService, times(1)).createOrder(101L, 202L, "Blood Test");
    }

    @Test
    void createOrder_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/laboratory/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":null,\"doctorId\":202,\"studyType\":\"Blood Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fields patientId, doctorId, and studyType are required."));

        verify(laboratoryService, never()).createOrder(any(), any(), any());
    }

    @Test
    void uploadResult_Success_ShouldReturnCreated() throws Exception {
        // Arrange
        LabResult createdResult = LabResult.builder()
                .id(10L)
                .resultData("Hemoglobin normal")
                .observations("No special notes")
                .build();

        when(laboratoryService.uploadResult(eq(1L), eq("Hemoglobin normal"), eq("No special notes"))).thenReturn(createdResult);

        // Act & Assert
        mockMvc.perform(post("/api/laboratory/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1,\"resultData\":\"Hemoglobin normal\",\"observations\":\"No special notes\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.resultData").value("Hemoglobin normal"))
                .andExpect(jsonPath("$.observations").value("No special notes"));

        verify(laboratoryService, times(1)).uploadResult(1L, "Hemoglobin normal", "No special notes");
    }

    @Test
    void uploadResult_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/laboratory/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":1,\"resultData\":null,\"observations\":\"No notes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Fields orderId and resultData are required."));

        verify(laboratoryService, never()).uploadResult(any(), any(), any());
    }

    @Test
    void uploadResult_OrderNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(laboratoryService.uploadResult(eq(99L), eq("Some data"), eq("Some obs")))
                .thenThrow(new RuntimeException("LabOrder not found with id: 99"));

        // Act & Assert
        mockMvc.perform(post("/api/laboratory/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":99,\"resultData\":\"Some data\",\"observations\":\"Some obs\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LabOrder not found with id: 99"));
    }

    @Test
    void getOrderStatus_Success_ShouldReturnStatus() throws Exception {
        // Arrange
        LabOrder order = LabOrder.builder()
                .id(1L)
                .status("PENDING")
                .build();
        when(laboratoryService.getOrder(1L)).thenReturn(order);

        // Act & Assert
        mockMvc.perform(get("/api/laboratory/orders/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(laboratoryService, times(1)).getOrder(1L);
    }

    @Test
    void getOrderStatus_NotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(laboratoryService.getOrder(99L)).thenThrow(new RuntimeException("LabOrder not found with id: 99"));

        // Act & Assert
        mockMvc.perform(get("/api/laboratory/orders/99/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("LabOrder not found with id: 99"));

        verify(laboratoryService, times(1)).getOrder(99L);
    }
}
