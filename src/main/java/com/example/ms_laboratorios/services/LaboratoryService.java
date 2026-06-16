package com.example.ms_laboratorios.services;

import com.example.ms_laboratorios.models.LabOrder;
import com.example.ms_laboratorios.models.LabResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaboratoryService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a new lab order invoking the stored procedure.
     */
    @Transactional
    public LabOrder createOrder(Long patientId, Long doctorId, String studyType) {
        log.info("Creating lab order via stored procedure for patient: {}, doctor: {}, study type: {}", patientId, doctorId, studyType);
        
        return jdbcTemplate.queryForObject(
                "SELECT * FROM create_lab_order(?, ?, ?)",
                (rs, rowNum) -> LabOrder.builder()
                        .id(rs.getLong("r_id"))
                        .patientId(rs.getLong("r_patient_id"))
                        .doctorId(rs.getLong("r_doctor_id"))
                        .studyType(rs.getString("r_study_type"))
                        .status(rs.getString("r_status"))
                        .build(),
                patientId, doctorId, studyType
        );
    }

    /**
     * Uploads laboratory results invoking the stored procedure.
     */
    @Transactional
    public LabResult uploadResult(Long orderId, String resultData, String observations) {
        log.info("Uploading result via stored procedure for order: {}", orderId);

        // Check if the order is already completed
        String status = jdbcTemplate.queryForObject("SELECT status FROM lab_orders WHERE id = ?", String.class, orderId);
        if ("COMPLETED".equals(status)) {
            throw new IllegalStateException("Results have already been uploaded for order id: " + orderId);
        }

        LabResult result = jdbcTemplate.queryForObject(
                "SELECT * FROM upload_lab_result(?, ?, ?)",
                (rs, rowNum) -> LabResult.builder()
                        .id(rs.getLong("r_id"))
                        .resultData(rs.getString("r_result_data"))
                        .observations(rs.getString("r_observations"))
                        .resultDate(rs.getTimestamp("r_result_date").toLocalDateTime())
                        .build(),
                orderId, resultData, observations
        );

        // Fetch doctor ID to trigger async notification
        Long doctorId = jdbcTemplate.queryForObject("SELECT doctor_id FROM lab_orders WHERE id = ?", Long.class, orderId);
        triggerAsyncNotification(doctorId, orderId);

        return result;
    }

    /**
     * Retrieves an order by its ID using native query.
     */
    @Transactional(readOnly = true)
    public LabOrder getOrder(Long orderId) {
        List<LabOrder> orders = jdbcTemplate.query(
                "SELECT o.id, o.patient_id, o.doctor_id, o.study_type, o.status, " +
                "r.id as r_id, r.result_data, r.observations, r.result_date " +
                "FROM lab_orders o LEFT JOIN lab_results r ON o.id = r.lab_order_id " +
                "WHERE o.id = ?",
                (rs, rowNum) -> {
                    LabOrder order = LabOrder.builder()
                            .id(rs.getLong("id"))
                            .patientId(rs.getLong("patient_id"))
                            .doctorId(rs.getLong("doctor_id"))
                            .studyType(rs.getString("study_type"))
                            .status(rs.getString("status"))
                            .build();
                    
                    long resultId = rs.getLong("r_id");
                    if (!rs.wasNull()) {
                        LabResult result = LabResult.builder()
                                .id(resultId)
                                .resultData(rs.getString("result_data"))
                                .observations(rs.getString("observations"))
                                .resultDate(rs.getTimestamp("result_date").toLocalDateTime())
                                .build();
                        order.setLabResult(result);
                    }
                    return order;
                },
                orderId
        );

        if (orders.isEmpty()) {
            throw new RuntimeException("LabOrder not found with id: " + orderId);
        }
        return orders.get(0);
    }

    /**
     * Retrieves all lab orders for a given patient ID using the stored procedure.
     */
    @Transactional(readOnly = true)
    public List<LabOrder> getOrdersByPatientId(Long patientId) {
        log.info("Fetching lab orders via stored procedure for patient: {}", patientId);
        return jdbcTemplate.query(
                "SELECT * FROM get_orders_by_patient(?)",
                (rs, rowNum) -> {
                    LabOrder order = LabOrder.builder()
                            .id(rs.getLong("r_id"))
                            .patientId(rs.getLong("r_patient_id"))
                            .doctorId(rs.getLong("r_doctor_id"))
                            .studyType(rs.getString("r_study_type"))
                            .status(rs.getString("r_status"))
                            .build();

                    long resultId = rs.getLong("r_res_id");
                    if (!rs.wasNull()) {
                        LabResult result = LabResult.builder()
                                .id(resultId)
                                .resultData(rs.getString("r_result_data"))
                                .observations(rs.getString("r_observations"))
                                .resultDate(rs.getTimestamp("r_result_date").toLocalDateTime())
                                .build();
                        order.setLabResult(result);
                    }
                    return order;
                },
                patientId
        );
    }

    private void triggerAsyncNotification(Long doctorId, Long orderId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Asynchronous task started: Simulating notification to doctor...");
                Thread.sleep(1500);
                log.info("NOTIFICATION SENT: Doctor {} has been successfully notified about the completed lab order {}", doctorId, orderId);
            } catch (InterruptedException e) {
                log.error("Asynchronous notification was interrupted", e);
                Thread.currentThread().interrupt();
            }
        });
    }
}
