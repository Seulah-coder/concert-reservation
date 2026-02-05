# API Documentation

## 🎯 Overview

Your Concert Reservation System API documentation is now live and accessible via Swagger UI!

## 📚 Accessing API Documentation

### Swagger UI (Interactive Documentation)
```
http://localhost:8080/swagger-ui.html
```
- Browse all available APIs
- Try out API calls directly in the browser
- View request/response examples
- See detailed descriptions and parameter information

### OpenAPI Specification (JSON)
```
http://localhost:8080/v3/api-docs
```
- Raw OpenAPI 3.0 specification
- Can be imported into Postman, Insomnia, or other API tools

## 🎫 API Categories

### 1. **Balance** - 사용자 잔액 관리
- `GET /api/balance/{userId}` - 잔액 조회
- `POST /api/balance/charge` - 잔액 충전

### 2. **Concerts** - 콘서트 정보 조회
- `GET /api/v1/concerts/dates` - 예약 가능한 날짜 조회
- `GET /api/v1/concerts/{concertDateId}/seats` - 좌석 조회

### 3. **Queue** - 대기열 관리
- `POST /api/v1/queue/token` - 대기열 토큰 발급
- `GET /api/v1/queue/status` - 대기 상태 조회

### 4. **Reservations** - 좌석 예약
- `POST /api/v1/reservations` - 좌석 예약
- `DELETE /api/v1/reservations/{reservationId}` - 예약 취소

### 5. **Payments** - 결제 처리
- `POST /api/payments` - 결제 완료

### 6. **Refunds** - 환불 처리
- `POST /api/refunds` - 환불 요청

## 🚀 Quick Start Example

### 1. Charge Balance
```bash
POST http://localhost:8080/api/balance/charge
Content-Type: application/json

{
  "userId": "user123",
  "amount": 100000
}
```

### 2. Get Available Dates
```bash
GET http://localhost:8080/api/v1/concerts/dates
```

### 3. Reserve a Seat
```bash
POST http://localhost:8080/api/v1/reservations
Content-Type: application/json

{
  "userId": "user123",
  "seatId": 1
}
```

### 4. Complete Payment
```bash
POST http://localhost:8080/api/payments
Content-Type: application/json

{
  "reservationId": 1,
  "userId": "user123"
}
```

## 📝 Features Implemented

✅ **Comprehensive API Documentation**
- All 6 controller groups fully documented
- Detailed descriptions for each endpoint
- Request/response examples with schema definitions
- Parameter descriptions and constraints

✅ **Interactive Testing**
- Try API calls directly from Swagger UI
- No need for external tools
- Real-time request/response viewing

✅ **Production Ready**
- OpenAPI 3.0 specification
- Can be imported to any API client tool
- Auto-generated from code annotations

## 🎨 Swagger UI Features

- **Tag Sorting**: APIs organized by functionality
- **Operation Sorting**: Methods sorted alphabetically
- **Schema Visualization**: See request/response structures
- **Authorization**: Ready for security integration
- **Try It Out**: Execute requests with sample data

## 🔧 Configuration

The Swagger configuration is in:
```
src/main/java/com/example/concert_reservation/config/OpenApiConfig.java
```

Settings in `application.properties`:
```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.tags-sorter=alpha
springdoc.swagger-ui.operations-sorter=alpha
```

## 📦 What Was Added

1. **Dependency** (`build.gradle`):
   - `springdoc-openapi-starter-webmvc-ui:2.3.0`

2. **Configuration** (`OpenApiConfig.java`):
   - API metadata (title, description, version)
   - Contact information
   - Server configuration

3. **Controller Annotations**:
   - `@Tag` - API group descriptions
   - `@Operation` - Endpoint descriptions
   - `@ApiResponses` - Response documentation
   - `@Parameter` - Parameter descriptions
   - `@Schema` - DTO field descriptions

4. **DTO Annotations**:
   - `@Schema` on record fields for better documentation
   - Example values for all fields

## 🎯 Next Steps

1. **Explore the API**: Open http://localhost:8080/swagger-ui.html
2. **Test Endpoints**: Use "Try it out" button on any endpoint
3. **Export Spec**: Download OpenAPI JSON for Postman/Insomnia
4. **Add Security**: Integrate JWT/OAuth when ready
5. **Version API**: Add versioning strategy if needed

---

**API Documentation is ready! 🎉**

Visit: http://localhost:8080/swagger-ui.html
