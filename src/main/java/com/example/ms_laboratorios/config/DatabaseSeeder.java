package com.example.ms_laboratorios.config;

import com.example.ms_laboratorios.models.LabOrder;
import com.example.ms_laboratorios.models.LabResult;
import com.example.ms_laboratorios.repositories.LabOrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final LabOrderRepository labOrderRepository;

    public DatabaseSeeder(LabOrderRepository labOrderRepository) {
        this.labOrderRepository = labOrderRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (labOrderRepository.count() == 0) {
            LabOrder order = LabOrder.builder()
                    .patientId(1L) // Maps to the first patient (Juan Carlos Gómez)
                    .doctorId(202L)
                    .studyType("Perfil Lipídico")
                    .status("COMPLETED")
                    .build();

            LabResult result = LabResult.builder()
                    .resultData("Colesterol Total: 210 mg/dL, Triglicéridos: 150 mg/dL, Colesterol HDL: 45 mg/dL")
                    .observations("Se sugiere dieta hipograsa y ejercicio regular.")
                    .resultDate(LocalDateTime.of(2026, 6, 1, 10, 0, 0))
                    .labOrder(order)
                    .build();

            order.setLabResult(result);

            labOrderRepository.save(order);
            System.out.println(">> DatabaseSeeder Laboratorios: Default lab order seeded successfully.");
        }
    }
}
