package com.example.ms_laboratorios.services;

import com.example.ms_laboratorios.models.LabOrder;
import com.example.ms_laboratorios.models.LabResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaboratoryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LaboratoryService laboratoryService;

    @Test
    @SuppressWarnings("unchecked")
    void createOrder_ShouldSaveAndReturnOrder() {
        // Arrange
        Long patientId = 101L;
        Long doctorId = 202L;
        String studyType = "Blood Test";
        LabOrder savedOrder = LabOrder.builder()
                .id(1L)
                .patientId(patientId)
                .doctorId(doctorId)
                .studyType(studyType)
                .status("PENDING")
                .build();

        when(jdbcTemplate.queryForObject(
                eq("SELECT * FROM create_lab_order(?, ?, ?)"),
                any(RowMapper.class),
                eq(patientId),
                eq(doctorId),
                eq(studyType)
        )).thenReturn(savedOrder);

        // Act
        LabOrder result = laboratoryService.createOrder(patientId, doctorId, studyType);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(patientId, result.getPatientId());
        verify(jdbcTemplate, times(1)).queryForObject(any(String.class), any(RowMapper.class), eq(patientId), eq(doctorId), eq(studyType));
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadResult_Success_ShouldUpdateOrderStatusAndSaveResult() {
        // Arrange
        Long orderId = 1L;
        String resultData = "Hemoglobin 14.5 g/dL";
        String observations = "Normal level";

        LabResult savedResult = LabResult.builder()
                .id(50L)
                .resultData(resultData)
                .observations(observations)
                .resultDate(LocalDateTime.now())
                .build();

        when(jdbcTemplate.queryForObject(
                eq("SELECT status FROM lab_orders WHERE id = ?"),
                eq(String.class),
                eq(orderId)
        )).thenReturn("PENDING");

        when(jdbcTemplate.queryForObject(
                eq("SELECT * FROM upload_lab_result(?, ?, ?)"),
                any(RowMapper.class),
                eq(orderId),
                eq(resultData),
                eq(observations)
        )).thenReturn(savedResult);

        when(jdbcTemplate.queryForObject(
                eq("SELECT doctor_id FROM lab_orders WHERE id = ?"),
                eq(Long.class),
                eq(orderId)
        )).thenReturn(202L);

        // Act
        LabResult result = laboratoryService.uploadResult(orderId, resultData, observations);

        // Assert
        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals(resultData, result.getResultData());
        assertEquals(observations, result.getObservations());

        verify(jdbcTemplate, times(1)).queryForObject(eq("SELECT status FROM lab_orders WHERE id = ?"), eq(String.class), eq(orderId));
        verify(jdbcTemplate, times(1)).queryForObject(eq("SELECT * FROM upload_lab_result(?, ?, ?)"), any(RowMapper.class), eq(orderId), eq(resultData), eq(observations));
        verify(jdbcTemplate, times(1)).queryForObject(eq("SELECT doctor_id FROM lab_orders WHERE id = ?"), eq(Long.class), eq(orderId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void uploadResult_AlreadyCompleted_ShouldThrowException() {
        // Arrange
        Long orderId = 1L;

        when(jdbcTemplate.queryForObject(
                eq("SELECT status FROM lab_orders WHERE id = ?"),
                eq(String.class),
                eq(orderId)
        )).thenReturn("COMPLETED");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
                laboratoryService.uploadResult(orderId, "Data", "Obs")
        );
        assertEquals("Results have already been uploaded for order id: 1", exception.getMessage());
        
        verify(jdbcTemplate, times(1)).queryForObject(eq("SELECT status FROM lab_orders WHERE id = ?"), eq(String.class), eq(orderId));
        verify(jdbcTemplate, never()).queryForObject(eq("SELECT * FROM upload_lab_result(?, ?, ?)"), any(RowMapper.class), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrder_Success_ShouldReturnOrder() {
        // Arrange
        Long orderId = 1L;
        LabOrder order = LabOrder.builder().id(orderId).status("PENDING").build();
        
        when(jdbcTemplate.query(
                eq("SELECT o.id, o.patient_id, o.doctor_id, o.study_type, o.status, r.id as r_id, r.result_data, r.observations, r.result_date FROM lab_orders o LEFT JOIN lab_results r ON o.id = r.lab_order_id WHERE o.id = ?"),
                any(RowMapper.class),
                eq(orderId)
        )).thenReturn(Collections.singletonList(order));

        // Act
        LabOrder result = laboratoryService.getOrder(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        verify(jdbcTemplate, times(1)).query(any(String.class), any(RowMapper.class), eq(orderId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrder_NotFound_ShouldThrowException() {
        // Arrange
        Long orderId = 999L;
        
        when(jdbcTemplate.query(
                any(String.class),
                any(RowMapper.class),
                eq(orderId)
        )).thenReturn(Collections.emptyList());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                laboratoryService.getOrder(orderId)
        );
        assertEquals("LabOrder not found with id: 999", exception.getMessage());
    }
}
