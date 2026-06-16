package com.example.ms_laboratorios.services;

import com.example.ms_laboratorios.models.LabOrder;
import com.example.ms_laboratorios.models.LabResult;
import com.example.ms_laboratorios.repositories.LabOrderRepository;
import com.example.ms_laboratorios.repositories.LabResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaboratoryServiceTest {

    @Mock
    private LabOrderRepository labOrderRepository;

    @Mock
    private LabResultRepository labResultRepository;

    @InjectMocks
    private LaboratoryService laboratoryService;

    @Test
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

        when(labOrderRepository.save(any(LabOrder.class))).thenReturn(savedOrder);

        // Act
        LabOrder result = laboratoryService.createOrder(patientId, doctorId, studyType);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(patientId, result.getPatientId());
        assertEquals(doctorId, result.getDoctorId());
        assertEquals(studyType, result.getStudyType());
        verify(labOrderRepository, times(1)).save(any(LabOrder.class));
    }

    @Test
    void uploadResult_Success_ShouldUpdateOrderStatusAndSaveResult() {
        // Arrange
        Long orderId = 1L;
        String resultData = "Hemoglobin 14.5 g/dL";
        String observations = "Normal level";
        
        LabOrder existingOrder = LabOrder.builder()
                .id(orderId)
                .patientId(101L)
                .doctorId(202L)
                .studyType("Blood Test")
                .status("PENDING")
                .build();

        LabResult savedResult = LabResult.builder()
                .id(50L)
                .resultData(resultData)
                .observations(observations)
                .labOrder(existingOrder)
                .build();

        when(labOrderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(labOrderRepository.save(any(LabOrder.class))).thenReturn(existingOrder);
        when(labResultRepository.save(any(LabResult.class))).thenReturn(savedResult);

        // Act
        LabResult result = laboratoryService.uploadResult(orderId, resultData, observations);

        // Assert
        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals(resultData, result.getResultData());
        assertEquals(observations, result.getObservations());
        assertEquals("COMPLETED", existingOrder.getStatus());
        assertNotNull(existingOrder.getLabResult());
        
        verify(labOrderRepository, times(1)).findById(orderId);
        verify(labOrderRepository, times(1)).save(existingOrder);
        verify(labResultRepository, times(1)).save(any(LabResult.class));
    }

    @Test
    void uploadResult_OrderNotFound_ShouldThrowException() {
        // Arrange
        Long orderId = 999L;
        when(labOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                laboratoryService.uploadResult(orderId, "Data", "Obs")
        );
        assertEquals("LabOrder not found with id: 999", exception.getMessage());
        verify(labOrderRepository, times(1)).findById(orderId);
        verify(labOrderRepository, never()).save(any());
        verify(labResultRepository, never()).save(any());
    }

    @Test
    void uploadResult_AlreadyCompleted_ShouldThrowException() {
        // Arrange
        Long orderId = 1L;
        LabOrder completedOrder = LabOrder.builder()
                .id(orderId)
                .status("COMPLETED")
                .build();

        when(labOrderRepository.findById(orderId)).thenReturn(Optional.of(completedOrder));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
                laboratoryService.uploadResult(orderId, "Data", "Obs")
        );
        assertEquals("Results have already been uploaded for order id: 1", exception.getMessage());
        verify(labOrderRepository, times(1)).findById(orderId);
        verify(labOrderRepository, never()).save(any());
        verify(labResultRepository, never()).save(any());
    }

    @Test
    void getOrder_Success_ShouldReturnOrder() {
        // Arrange
        Long orderId = 1L;
        LabOrder order = LabOrder.builder().id(orderId).status("PENDING").build();
        when(labOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        LabOrder result = laboratoryService.getOrder(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        verify(labOrderRepository, times(1)).findById(orderId);
    }

    @Test
    void getOrder_NotFound_ShouldThrowException() {
        // Arrange
        Long orderId = 999L;
        when(labOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                laboratoryService.getOrder(orderId)
        );
        assertEquals("LabOrder not found with id: 999", exception.getMessage());
    }
}
