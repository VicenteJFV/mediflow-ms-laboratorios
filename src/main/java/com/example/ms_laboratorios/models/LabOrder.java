package com.example.ms_laboratorios.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lab_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "study_type", nullable = false)
    private String studyType;

    @Column(nullable = false)
    private String status; // e.g., "PENDING", "COMPLETED"

    @OneToOne(mappedBy = "labOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LabResult labResult;
}
