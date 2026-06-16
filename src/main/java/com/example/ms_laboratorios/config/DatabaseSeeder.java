package com.example.ms_laboratorios.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Create Stored Procedures in PostgreSQL
        createStoredProcedures();

        // 2. Seed default data if database is empty
        seedData();
    }

    private void createStoredProcedures() {
        System.out.println(">> DatabaseSeeder: Creating Stored Procedures in PostgreSQL...");

        // Function 1: create_lab_order
        jdbcTemplate.execute(
            "CREATE OR REPLACE FUNCTION create_lab_order(p_patient_id BIGINT, p_doctor_id BIGINT, p_study_type VARCHAR) " +
            "RETURNS TABLE(r_id BIGINT, r_patient_id BIGINT, r_doctor_id BIGINT, r_study_type VARCHAR, r_status VARCHAR) AS $$ " +
            "BEGIN " +
            "    RETURN QUERY " +
            "    INSERT INTO lab_orders (patient_id, doctor_id, study_type, status) " +
            "    VALUES (p_patient_id, p_doctor_id, p_study_type, 'PENDING') " +
            "    RETURNING id, patient_id, doctor_id, study_type, status; " +
            "END; " +
            "$$ LANGUAGE plpgsql;"
        );

        // Function 2: upload_lab_result
        jdbcTemplate.execute(
            "CREATE OR REPLACE FUNCTION upload_lab_result(p_order_id BIGINT, p_result_data TEXT, p_observations TEXT) " +
            "RETURNS TABLE(r_id BIGINT, r_result_data TEXT, r_observations TEXT, r_result_date TIMESTAMP, r_lab_order_id BIGINT) AS $$ " +
            "BEGIN " +
            "    UPDATE lab_orders SET status = 'COMPLETED' WHERE id = p_order_id; " +
            "    " +
            "    RETURN QUERY " +
            "    INSERT INTO lab_results (lab_order_id, result_data, observations, result_date) " +
            "    VALUES (p_order_id, p_result_data, p_observations, NOW()) " +
            "    RETURNING id, result_data, observations, result_date, lab_order_id; " +
            "END; " +
            "$$ LANGUAGE plpgsql;"
        );

        // Function 3: get_orders_by_patient
        jdbcTemplate.execute(
            "CREATE OR REPLACE FUNCTION get_orders_by_patient(p_patient_id BIGINT) " +
            "RETURNS TABLE(r_id BIGINT, r_patient_id BIGINT, r_doctor_id BIGINT, r_study_type VARCHAR, r_status VARCHAR, r_res_id BIGINT, r_result_data TEXT, r_observations TEXT, r_result_date TIMESTAMP) AS $$ " +
            "BEGIN " +
            "    RETURN QUERY " +
            "    SELECT o.id, o.patient_id, o.doctor_id, o.study_type, o.status, " +
            "           r.id, r.result_data, r.observations, r.result_date " +
            "    FROM lab_orders o " +
            "    LEFT JOIN lab_results r ON o.id = r.lab_order_id " +
            "    WHERE o.patient_id = p_patient_id; " +
            "END; " +
            "$$ LANGUAGE plpgsql;"
        );

        System.out.println(">> DatabaseSeeder: Stored Procedures created/verified successfully.");
    }

    private void seedData() {
        Long orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lab_orders", Long.class);
        if (orderCount != null && orderCount == 0) {
            System.out.println(">> DatabaseSeeder: Seeding default lab records using Stored Procedures...");

            // Create order via stored procedure
            jdbcTemplate.execute(
                "SELECT * FROM create_lab_order(1, 202, 'Perfil Lipídico')"
            );

            // Upload results via stored procedure for order id 1
            jdbcTemplate.execute(
                "SELECT * FROM upload_lab_result(1, 'Colesterol Total: 210 mg/dL, Triglicéridos: 150 mg/dL, Colesterol HDL: 45 mg/dL', 'Se sugiere dieta hipograsa y ejercicio regular.')"
            );

            System.out.println(">> DatabaseSeeder: Default lab order seeded successfully.");
        }
    }
}
