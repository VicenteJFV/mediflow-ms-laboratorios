package com.example.ms_laboratorios.services;

import com.example.ms_laboratorios.models.LabOrder;
import com.example.ms_laboratorios.models.LabResult;
import com.example.ms_laboratorios.repositories.LabOrderRepository;
import com.example.ms_laboratorios.repositories.LabResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaboratoryService {

    private final LabOrderRepository labOrderRepository;
    private final LabResultRepository labResultRepository;

    /**
     * Creates a new lab order with a default PENDING status.
     */
    @Transactional
    public LabOrder createOrder(Long patientId, Long doctorId, String studyType) {
        log.info("Creating lab order for patient: {}, doctor: {}, study type: {}", patientId, doctorId, studyType);
        
        LabOrder order = LabOrder.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .studyType(studyType)
                .status("PENDING")
                .build();
                
        return labOrderRepository.save(order);
    }

    /**
     * Uploads laboratory results, associates them with the order,
     * updates the order status to COMPLETED, and triggers an asynchronous notification simulation.
     */
    @Transactional
    public LabResult uploadResult(Long orderId, String resultData, String observations) {
        log.info("Uploading result for order: {}", orderId);

        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("LabOrder not found with id: " + orderId));

        if ("COMPLETED".equals(order.getStatus())) {
            throw new IllegalStateException("Results have already been uploaded for order id: " + orderId);
        }

        LabResult result = LabResult.builder()
                .resultData(resultData)
                .observations(observations)
                .resultDate(LocalDateTime.now())
                .labOrder(order)
                .build();

        order.setLabResult(result);
        order.setStatus("COMPLETED");

        labOrderRepository.save(order);
        LabResult savedResult = labResultRepository.save(result);

        // Simulate asynchronous notification to the requesting doctor
        triggerAsyncNotification(order.getDoctorId(), order.getId());

        return savedResult;
    }

    /**
     * Retrieves an order by its ID.
     */
    @Transactional(readOnly = true)
    public LabOrder getOrder(Long orderId) {
        return labOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("LabOrder not found with id: " + orderId));
    }

    /**
     * Simulates sending a notification asynchronously to the doctor.
     */
    private void triggerAsyncNotification(Long doctorId, Long orderId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Asynchronous task started: Simulating notification to doctor...");
                // Simulate network latency or notification process
                Thread.sleep(1500);
                log.info("NOTIFICATION SENT: Doctor {} has been successfully notified about the completed lab order {}", doctorId, orderId);
            } catch (InterruptedException e) {
                log.error("Asynchronous notification was interrupted", e);
                Thread.currentThread().interrupt();
            }
        });
    }
}
