# MediFlow - Microservicio de Laboratorios (ms-laboratorios)

Este microservicio forma parte de la plataforma **MediFlow** y está construido con Spring Boot y Maven. Se encarga de gestionar de forma exclusiva la creación de órdenes de laboratorio, la carga de resultados médicos y la simulación de notificaciones asíncronas a los médicos solicitantes.

---

## Requisitos Previos

- **Java**: JDK 21 o superior instalado.
- **Maven**: (Incluye el wrapper `mvnw` para ejecución local).
- **PostgreSQL**: Base de datos relacional para persistencia.

---

## 1. Configuración de la Base de Datos PostgreSQL

El microservicio utiliza bases de datos PostgreSQL independientes. Necesitas crear dos bases de datos: una para el entorno de desarrollo y otra para la ejecución de pruebas unitarias.

### Opción A: Levantar PostgreSQL con Docker (Recomendado)
Puedes iniciar una instancia de PostgreSQL en el puerto `5432` con el siguiente comando:
```bash
docker run --name mediflow-postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=mediflow_laboratorios -p 5432:5432 -d postgres
```

Luego, crea la base de datos de pruebas dentro de la misma instancia:
```bash
docker exec -it mediflow-postgres psql -U postgres -c "CREATE DATABASE mediflow_laboratorios_test;"
```

### Opción B: Instalación Local Manual
1. Conéctate a tu servidor local de PostgreSQL.
2. Crea las siguientes bases de datos:
   ```sql
   CREATE DATABASE mediflow_laboratorios;
   CREATE DATABASE mediflow_laboratorios_test;
   ```

*Nota: Hibernate está configurado en modo `update` para desarrollo (`application.properties`) y en `create-drop` para las pruebas (`application-test.properties`), por lo que las tablas correspondientes se crearán automáticamente.*

---

## 2. Ejecución del Microservicio

Para iniciar la aplicación localmente en el puerto `8081` (puerto configurado para evitar colisiones con ms-hce):

En la raíz del proyecto, ejecuta el siguiente comando:

**En Windows (PowerShell/CMD):**
```powershell
.\mvnw spring-boot:run
```

**En Linux / macOS:**
```bash
./mvnw spring-boot:run
```

---

## 3. Ejecución de Pruebas y Reporte de Cobertura (JaCoCo)

Las pruebas unitarias y de integración se ejecutan contra la base de datos PostgreSQL de pruebas (`mediflow_laboratorios_test`). Asegúrate de que el servidor PostgreSQL esté levantado antes de correr los tests.

Para ejecutar el conjunto de pruebas unitarias y compilar el reporte de cobertura de código (mínimo 60% requerido):

```bash
.\mvnw clean test
```

### Ver el Reporte de Cobertura
Una vez completado el comando, JaCoCo generará un reporte de cobertura en formato HTML. Puedes abrirlo directamente en tu navegador desde la siguiente ubicación:
`target/site/jacoco/index.html`

---

## 4. Endpoints de la API REST

Todos los endpoints están expuestos bajo el prefijo `/api/laboratory`.

### A. Crear una Orden de Laboratorio
* **Endpoint:** `POST /api/laboratory/orders`
* **Cuerpo de la Petición (JSON):**
  ```json
  {
    "patientId": 101,
    "doctorId": 202,
    "studyType": "Análisis de Sangre"
  }
  ```
* **Respuesta Exitosa (201 Created):**
  ```json
  {
    "id": 1,
    "patientId": 101,
    "doctorId": 202,
    "studyType": "Análisis de Sangre",
    "status": "PENDING",
    "labResult": null
  }
  ```

### B. Cargar un Resultado de Laboratorio
*Al subir el resultado, el estado de la orden cambia automáticamente a `COMPLETED` y se inicia una simulación de notificación asíncrona hacia el médico.*
* **Endpoint:** `POST /api/laboratory/results`
* **Cuerpo de la Petición (JSON):**
  ```json
  {
    "orderId": 1,
    "resultData": "Hemoglobina: 14.2 g/dL, Colesterol: 180 mg/dL",
    "observations": "Niveles normales. Sin observaciones críticas."
  }
  ```
* **Respuesta Exitosa (201 Created):**
  ```json
  {
    "id": 1,
    "resultData": "Hemoglobina: 14.2 g/dL, Colesterol: 180 mg/dL",
    "observations": "Niveles normales. Sin observaciones críticas.",
    "resultDate": "2026-06-15T20:13:31"
  }
  ```

### C. Consultar Estado de una Orden
* **Endpoint:** `GET /api/laboratory/orders/{id}/status`
* **Respuesta Exitosa (200 OK):**
  ```json
  {
    "orderId": 1,
    "status": "COMPLETED"
  }
  ```
