# Dispatch Load Balancer — final submission

A Java 17 / Spring Boot 3.4.5 REST API for capacity-constrained delivery dispatch. It accepts orders and fleet details, assigns orders in HIGH → MEDIUM → LOW priority tiers, calculates route distance with the Haversine formula, and returns vehicle details, route stops, load, distance, and unassigned-order reasons.

## Assignment criteria coverage

| Assignment / evaluation criterion | Implementation |
|---|---|
| Java | Java 17, Spring Boot 3.4.5, Maven |
| REST APIs; no UI | JSON REST endpoints under `/api/dispatch` |
| Postman testable | `postman/Dispatch-Load-Balancer.postman_collection.json` includes the 30-order / 5-vehicle dataset and validation request |
| Modular design | Controller, DTO, entity, repository, service, optimizer, distance calculator, exception handler separated by responsibility |
| API documentation | OpenAPI 3 + Swagger UI |
| JUnit tests | Haversine tests, allocation/capacity/priority tests, and repository/service integration tests |
| Efficient upsert | Spring Data JPA repositories use `saveAll` with IDs as primary keys; existing IDs update, new IDs insert; JDBC batch settings enabled |
| Error handling | Bean Validation, duplicate-ID checks, malformed JSON/enum handling, structured 400 responses, safe storage/internal error responses |
| Performance | Batch persistence, bounded request sizes, deterministic allocation, bounded local-search passes |
| Correctness | Capacity never exceeded; HIGH orders processed before MEDIUM and LOW; Haversine route totals; unassigned reasons included |
| Required response fields | Vehicle capacity, current coordinates/address, assigned order details/addresses, total load, total distance |

## Algorithm and honest optimization note

1. Orders are processed by priority tier (`HIGH`, then `MEDIUM`, then `LOW`). Within each tier, orders are processed by `orderId` for deterministic behavior.
2. For each order, the allocator checks every vehicle with sufficient remaining capacity and inserts the order at the route position with the smallest incremental Haversine distance.
3. It refines each route with 2-opt and performs capacity-safe inter-vehicle relocation moves when they reduce the sum of route distances.
4. All route distances are calculated in kilometers with mean Earth radius 6,371.0088 km.
5. Distance is one-way: from the vehicle's current location through its assigned stops. No return-to-depot leg is counted.

This is a deterministic, capacity-safe local-search heuristic. Vehicle routing with capacity constraints is a combinatorial optimization problem; the heuristic improves the route plan but does **not** mathematically guarantee the global optimum for every possible input. The response identifies the method so this limitation is explicit rather than claiming an unproven global minimum. For a production requirement that mandates a provably optimal solution, replace the heuristic with an exact MILP/constraint-programming solver and define a practical dataset/time limit.

## Prerequisites

- Java 17+
- Maven 3.9+

## Run and test locally

```bash
mkdir -p data
mvn clean verify
mvn spring-boot:run
```

- API base: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/health`

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/dispatch/orders` | Insert/update orders by `orderId` |
| POST | `/api/dispatch/vehicles` | Insert/update vehicles by `vehicleId` |
| GET | `/api/dispatch/plan` | Generate the dispatch plan |
| DELETE | `/api/dispatch/data` | Delete all orders and vehicles (test/reset only) |
| GET | `/health` | Hosting health check |

### Example: submit orders

```json
{
  "orders": [
    {
      "orderId": "ORD001",
      "latitude": 28.6139,
      "longitude": 77.209,
      "address": "Connaught Place, Delhi, India",
      "packageWeight": 15,
      "priority": "HIGH"
    }
  ]
}
```

### Example: submit vehicles

```json
{
  "vehicles": [
    {
      "vehicleId": "VEH001",
      "capacity": 100,
      "currentLatitude": 28.7041,
      "currentLongitude": 77.1025,
      "currentAddress": "Karol Bagh, Delhi, India"
    }
  ]
}
```

### Response shape

`GET /api/dispatch/plan` returns `dispatchPlan`, `unassignedOrders`, aggregate assigned load/distance, counts, and the algorithm/distance definitions. Each vehicle includes `capacity`, `currentLatitude`, `currentLongitude`, `currentAddress`, `totalLoad`, `totalDistance`, `totalDistanceKm`, and `assignedOrders`. Each assigned order includes its address, coordinates, weight, and priority.

## Run the original assignment sample

The original sample payloads are included in `sample-data/`:

- `sample-data/vehicles-5.json`
- `sample-data/orders-30.json`

Run in this order:

1. `DELETE /api/dispatch/data`
2. `POST /api/dispatch/vehicles` with `vehicles-5.json`
3. `POST /api/dispatch/orders` with `orders-30.json`
4. `GET /api/dispatch/plan`

The source sample contains 30 orders and 5 vehicles. The sample's total order weight is 645 and total fleet capacity is 500, so at least 145 weight units cannot be carried even if all capacity is perfectly utilized. Because packages are indivisible, some vehicle capacity may remain unused.

## Postman

1. Import `postman/Dispatch-Load-Balancer.postman_collection.json` into Postman.
2. Set collection variable `baseUrl` to `http://localhost:8080` or your deployed service URL.
3. Run the requests in order: health, clear, vehicles, orders, plan. The invalid-order request is expected to return HTTP 400.

## Storage and deployment notes

The project uses Spring Data JPA with H2. By default it uses a local file database at `./data/dispatchdb`, so local data survives application restarts. For cloud hosting, the H2 file is only durable if the host provides a persistent disk mounted at the application working directory. Render's default ephemeral filesystem can lose data after a restart/redeploy; for durable hosted use, configure a managed PostgreSQL database by setting `DATABASE_URL` and the corresponding JDBC driver/configuration, or attach persistent storage. Do not run multiple replicas against a single local H2 file.

### Deploy on Render using Docker

1. Push this project to a GitHub repository.
2. In Render choose **New → Blueprint** and connect the repository (or create a Docker Web Service).
3. Render detects `render.yaml` and builds the included Dockerfile.
4. Wait for the service health check at `/health` to pass.
5. Open `https://YOUR-SERVICE.onrender.com/swagger-ui.html`.

GitHub Pages cannot host a Spring Boot API; it only serves static websites. Free hosting plans and persistence features can change. Free instances may sleep when idle and take time to wake.

### Browser-only workflow with GitHub Codespaces

Open the repository in GitHub, choose **Code → Codespaces → Create codespace**, and use the included `.devcontainer` configuration for Java 17 and Maven. In the terminal run `mvn clean verify`. Commit and push changes; Render can redeploy from the connected repository.

## Configuration

- `PORT`: server port (defaults to 8080)
- `DATABASE_URL`: JDBC URL override (defaults to local H2 file)
- `JAVA_OPTS`: JVM options in the Docker container

## Important assumptions

- Priorities are allocation tiers, not a hard promise that every order in a tier can be assigned when fleet capacity is insufficient.
- Orders are indivisible; partial package assignment is not supported.
- Distances are straight-line great-circle distances, not road-network driving distances or traffic estimates.
- The current vehicle position is the start point for that vehicle's route; no return leg is included.
- The API is stateless with respect to dispatch plans: a plan is recalculated from the current stored order and vehicle records each time it is requested.

For PostgreSQL, include a JDBC URL such as `jdbc:postgresql://HOST:5432/DATABASE` in `DATABASE_URL`, and set `DB_USERNAME` and `DB_PASSWORD`. The PostgreSQL JDBC driver is included. H2 remains the default for local development and self-contained demos.
