package com.example.ms_laboratorios.models;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_data", nullable = false, columnDefinition = "TEXT")
    private String resultData;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "result_date", nullable = false)
    private LocalDateTime resultDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    @JsonIgnore
    private LabOrder labOrder;
}
