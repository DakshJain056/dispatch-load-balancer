# Dispatch Load Balancer

A Java 17 / Spring Boot REST API that allocates delivery orders to vehicles using priority-first dispatching, vehicle capacity constraints, and Haversine distance.

## Requirements
- Java 17+
- Maven 3.9+ (or use the included Maven wrapper after generating one with `mvn wrapper:wrapper`)

## Run locally
```bash
mvn clean test
mvn spring-boot:run
```
Application: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/dispatch/orders` | Create/update orders by `orderId` |
| POST | `/api/dispatch/vehicles` | Create/update vehicles by `vehicleId` |
| GET | `/api/dispatch/plan` | Generate optimized dispatch plan |
| DELETE | `/api/dispatch/data` | Clear in-memory data (demo/reset) |

### Submit orders
```bash
curl -X POST http://localhost:8080/api/dispatch/orders \
  -H "Content-Type: application/json" \
  -d '{"orders":[{"orderId":"ORD001","latitude":28.6139,"longitude":77.2090,"address":"Connaught Place, Delhi, India","packageWeight":15,"priority":"HIGH"}]}'
```

### Submit vehicles
```bash
curl -X POST http://localhost:8080/api/dispatch/vehicles \
  -H "Content-Type: application/json" \
  -d '{"vehicles":[{"vehicleId":"VEH001","capacity":100,"currentLatitude":28.7041,"currentLongitude":77.1025,"currentAddress":"Karol Bagh, Delhi, India"}]}'
```

### Get plan
```bash
curl http://localhost:8080/api/dispatch/plan
```

The response includes `dispatchPlan` and `unassignedOrders`. Every vehicle includes `totalLoad`, `totalDistance` and `assignedOrders`.

## Algorithm and assumptions
1. Orders are sorted by priority (`HIGH`, `MEDIUM`, `LOW`), then by `orderId` for deterministic output.
2. Each order is assigned to the currently feasible vehicle whose last known position is closest to that order. Ties are resolved by vehicle ID.
3. Vehicle capacity is never exceeded. Orders that cannot fit are returned in `unassignedOrders` with a reason.
4. Haversine distance is calculated in kilometers using mean Earth radius 6,371.0088 km.
5. Distance is the sequential route distance from the vehicle's current location through its assigned orders. A return trip to the depot is not included.
6. This is a practical greedy heuristic, not a globally optimal vehicle-routing solver. It prioritizes correctness, deterministic behavior and efficient runtime for large input lists.
7. Data is held in memory and is reset when the process restarts. This keeps the project self-contained for the assignment; replace the maps with a database repository for durable production storage.

## Deployment (free-tier options)
GitHub Pages cannot host Java/Spring Boot APIs because it only serves static sites. Use GitHub to store the code and a backend host such as Render for the API.

### Deploy on Render
1. Push this project to a GitHub repository.
2. In Render, create **New → Web Service** and connect the repository.
3. Choose Docker as the runtime (or choose native runtime with build command `mvn clean package -DskipTests` and start command `java -jar target/dispatch-load-balancer-1.0.0.jar`).
4. For Docker, no additional environment variables are required. Render supplies `PORT`, which the application reads.
5. After deployment, your API base URL will be `https://YOUR-SERVICE.onrender.com`.
6. Swagger: `https://YOUR-SERVICE.onrender.com/swagger-ui.html`.

Free hosting availability and limits can change; free instances may sleep when idle and take time to wake. Confirm current pricing/limits before relying on a host for a deadline. A public URL is only created after you complete deployment from your own hosting account.

## Testing
```bash
mvn test
```
The test suite covers Haversine identity, approximate distance, symmetry, capacity limits, priority order, no-vehicle cases, upsert behavior and duplicate identifiers.

## Notes for submission
- Include your actual deployed API URL after deployment.
- Include the Swagger URL above using the deployed host.
- The current implementation uses in-memory storage; for multi-instance production deployment, use a persistent database and shared repository.
