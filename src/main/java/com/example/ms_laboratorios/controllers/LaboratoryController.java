package com.example.ms_laboratorios.controllers;

import com.example.ms_laboratorios.models.LabOrder;
import com.example.ms_laboratorios.models.LabResult;
import com.example.ms_laboratorios.services.LaboratoryService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/laboratory")
@RequiredArgsConstructor
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    /**
     * POST /api/laboratory/orders
     * Creates a new lab order.
     */
    @PostMapping("/orders")
    public ResponseEntity<LabOrder> createOrder(@RequestBody OrderRequest request) {
        if (request.getPatientId() == null || request.getDoctorId() == null || request.getStudyType() == null) {
            throw new IllegalArgumentException("Fields patientId, doctorId, and studyType are required.");
        }
        LabOrder order = laboratoryService.createOrder(
                request.getPatientId(),
                request.getDoctorId(),
                request.getStudyType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * POST /api/laboratory/results
     * Uploads results for an existing lab order.
     */
    @PostMapping("/results")
    public ResponseEntity<LabResult> uploadResult(@RequestBody ResultRequest request) {
        if (request.getOrderId() == null || request.getResultData() == null) {
            throw new IllegalArgumentException("Fields orderId and resultData are required.");
        }
        LabResult result = laboratoryService.uploadResult(
                request.getOrderId(),
                request.getResultData(),
                request.getObservations()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /api/laboratory/orders/{id}/status
     * Retrieves the status of a specific lab order.
     */
    @GetMapping("/orders/{id}/status")
    public ResponseEntity<StatusResponse> getOrderStatus(@PathVariable("id") Long id) {
        LabOrder order = laboratoryService.getOrder(id);
        StatusResponse response = new StatusResponse(order.getId(), order.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/laboratory/patients/{patientId}
     * Retrieves all lab orders for a specific patient.
     */
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<java.util.List<LabOrder>> getOrdersByPatientId(@PathVariable("patientId") Long patientId) {
        java.util.List<LabOrder> orders = laboratoryService.getOrdersByPatientId(patientId);
        return ResponseEntity.ok(orders);
    }

    // --- DTOs ---

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderRequest {
        private Long patientId;
        private Long doctorId;
        private String studyType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultRequest {
        private Long orderId;
        private String resultData;
        private String observations;
    }

    @Data
    @AllArgsConstructor
    public static class StatusResponse {
        private Long orderId;
        private String status;
    }

    // --- Exception Handlers ---

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
