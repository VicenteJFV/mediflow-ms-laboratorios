package com.example.ms_laboratorios.repositories;

import com.example.ms_laboratorios.models.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {
}
