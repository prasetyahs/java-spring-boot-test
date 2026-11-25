# Spring Boot Microservices — Category & Product CRUD

Microservices CRUD berbasis **Spring Boot 4.2.0 (Java 17)** dengan **PostgreSQL** dan **Docker Compose**.

## Arsitektur & Ketentuan

Project ini terdiri dari 3 service dalam satu Docker bridge network (`app-network`):

| Service | Container | Port | Deskripsi |
|---|---|---|---|
| `postgres` | `postgres-db` | `5432` | Database PostgreSQL 16 (`postgres:16-alpine`) |
| `category-service` | `category-service` | `8081` | REST API CRUD Category |
| `product-service` | `product-service` | `8082` | REST API CRUD Product + integrasi ke `category-service` |

### Implementasi Ketentuan
- **Spring IoC**: Constructor-based Dependency Injection melalui interface (`CategoryService` -> `CategoryServiceImpl`, `ProductService` -> `ProductServiceImpl`), serta konfigurasi `@Configuration` + `@Bean` (`RestClientConfig` -> `CategoryClient`).
- **Java Stream**: Transformasi DTO, filtering & sorting dinamis (`stream().sorted(...).map(...).toList()`), mapping lookup kategori (`Collectors.toMap`), grouping (`Collectors.groupingBy`), dan agregasi summary (`mapToInt`, `reduce`, `min`, `max`).
- **Intermediate Native SQL Query**: Parameterized PostgreSQL native SQL (`@Query(..., nativeQuery = true)`) dengan explicit `CAST`, pencarian `LOWER(...) LIKE`, `GROUP BY` + `HAVING` + aggregate functions (`COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `COALESCE`), CTE (`WITH ... AS`) + Window Function (`DENSE_RANK() OVER`), serta `@Modifying` native `UPDATE`/`DELETE`.
- **Containerization & Microservices**: Multi-stage `Dockerfile` (non-root user, BuildKit Gradle cache, healthcheck), `docker-compose.yml` dengan `depends_on: condition: service_healthy`, dan komunikasi antar-service via HTTP (`RestClient`).

### Dependencies (Minimal)
Masing-masing service (`category/build.gradle` & `product/build.gradle`) hanya menggunakan dependency yang diperlukan:
- `org.springframework.boot:spring-boot-starter-webmvc`
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.postgresql:postgresql` (`runtimeOnly`)

---

## Prasyarat

- **WSL 2** dengan **Docker Engine** & **Docker Compose** (`docker compose`)
- *(Opsional untuk run lokal tanpa Docker)*: **JDK 17**

---

## How to Build & Run

### 1. Menggunakan Docker Compose (Direkomendasikan)

Masuk ke root directory project di WSL:

```bash
cd /mnt/d/test-be-springboot
```

Build image dan jalankan semua service di background:

```bash
docker compose up -d --build
```

Build ulang tanpa menjalankan container:

```bash
docker compose build
```

Build ulang satu service saja (misalnya `product-service`):

```bash
docker compose up -d --build product-service
```

Cek status container (pastikan semua berstatus `healthy`):

```bash
docker compose ps
```

Lihat log aplikasi:

```bash
docker compose logs -f
# atau per service:
docker compose logs -f category-service
docker compose logs -f product-service
```

Menghentikan service:

```bash
# Hentikan container (data PostgreSQL tetap tersimpan di volume)
docker compose down

# Hentikan container sekaligus hapus volume database
docker compose down -v
```

### 2. Build & Run Lokal dengan Gradle

Jalankan PostgreSQL via Docker terlebih dahulu:

```bash
docker compose up -d postgres
```

Lalu build & jalankan masing-masing service:

```bash
# Category Service (port 8081)
cd /mnt/d/test-be-springboot/category
./gradlew bootJar -x test
./gradlew bootRun

# Product Service (port 8082) — di terminal terpisah
cd /mnt/d/test-be-springboot/product
./gradlew bootJar -x test
./gradlew bootRun
```

---

## Konfigurasi Environment Variable

Semua environment variable di `docker-compose.yml` sudah memiliki nilai default (opsional bisa dioverride lewat file `.env`):

| Variable | Default | Keterangan |
|---|---|---|
| `POSTGRES_USER` | `postgres` | Username PostgreSQL |
| `POSTGRES_PASSWORD` | `postgres` | Password PostgreSQL |
| `POSTGRES_DB` | `postgres` | Nama database PostgreSQL |
| `POSTGRES_PORT` | `5432` | Port host PostgreSQL |
| `CATEGORY_SERVICE_PORT` | `8081` | Port host `category-service` |
| `PRODUCT_SERVICE_PORT` | `8082` | Port host `product-service` |

---

## List Endpoint

### Format Response Standar

Semua endpoint (sukses maupun error) menggunakan format response seragam:

```json
{
  "status": true,
  "message": "Category created successfully",
  "data": { }
}
```

Contoh response error:

```json
{
  "status": false,
  "message": "Category not found with id: 99",
  "data": null
}
```

### Category Service — Base URL: `http://localhost:8081`

| Method | Endpoint | Query Params | Deskripsi |
|---|---|---|---|
| `POST` | `/api/categories` | - | Membuat category baru (`201 Created`) |
| `GET` | `/api/categories` | `keyword`, `active` (`true`/`false`), `sortBy` (`name`/`oldest`) | List & filter category (Native SQL + Java Stream) |
| `GET` | `/api/categories/{id}` | - | Detail category berdasarkan ID |
| `PUT` | `/api/categories/{id}` | - | Update category (Native SQL `UPDATE`) |
| `DELETE` | `/api/categories/{id}` | - | Hapus category (Native SQL `DELETE`, `200 OK`) |
| `GET` | `/api/categories/stats` | - | Statistik jumlah category berdasarkan status `active` (Native SQL `GROUP BY`) |
| `GET` | `/api/categories/grouped` | - | Mengelompokkan category berdasarkan status `active` (Java Stream `groupingBy`) |

#### Contoh Request Category
```bash
# Create
curl -X POST http://localhost:8081/api/categories \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Electronics",
    "description": "Electronic devices and accessories",
    "active": true
  }'

# Get All + Filter & Sort
curl "http://localhost:8081/api/categories?keyword=elec&active=true&sortBy=name"

# Get by ID
curl http://localhost:8081/api/categories/1

# Update
curl -X PUT http://localhost:8081/api/categories/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Consumer Electronics",
    "description": "Updated description",
    "active": true
  }'

# Stats & Grouped
curl http://localhost:8081/api/categories/stats
curl http://localhost:8081/api/categories/grouped

# Delete
curl -X DELETE http://localhost:8081/api/categories/1
```

---

### Product Service — Base URL: `http://localhost:8082`

> Saat `POST` dan `PUT`, `product-service` otomatis memvalidasi `categoryId` ke `category-service` melalui `RestClient` dan memperkaya response dengan `categoryName`.

| Method | Endpoint | Query Params | Deskripsi |
|---|---|---|---|
| `POST` | `/api/products` | - | Membuat product baru (`201 Created`, validasi `categoryId` ke `category-service`) |
| `GET` | `/api/products` | `keyword`, `categoryId`, `minPrice`, `maxPrice`, `inStockOnly`, `sortBy` (`priceAsc`/`priceDesc`/`name`) | List & multi-filter product (Native SQL + Java Stream + enrich `categoryName`) |
| `GET` | `/api/products/{id}` | - | Detail product berdasarkan ID |
| `PUT` | `/api/products/{id}` | - | Update product (validasi `categoryId` + Native SQL `UPDATE`) |
| `DELETE` | `/api/products/{id}` | - | Hapus product (Native SQL `DELETE`, `200 OK`) |
| `GET` | `/api/products/stats/by-category` | `minProductCount` (default: `1`) | Statistik harga & stok per category (Native SQL `GROUP BY` + `HAVING`) |
| `GET` | `/api/products/top-expensive` | `topN` (default: `3`) | Top N product termahal per category (Native SQL CTE + `DENSE_RANK()`) |
| `GET` | `/api/products/summary` | - | Ringkasan total produk, total stok, avg/min/max price, & grouping per category (Java Stream) |

#### Contoh Request Product
```bash
# Create (pastikan categoryId sudah ada di category-service)
curl -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 14",
    "description": "16GB RAM, 512GB SSD",
    "price": 15000000,
    "stock": 10,
    "categoryId": 1
  }'

# Get All + Multi-Filter & Sort
curl "http://localhost:8082/api/products?categoryId=1&minPrice=1000000&inStockOnly=true&sortBy=priceDesc"

# Get by ID
curl http://localhost:8082/api/products/1

# Update
curl -X PUT http://localhost:8082/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro 14 M3",
    "description": "24GB RAM, 1TB SSD",
    "price": 18500000,
    "stock": 8,
    "categoryId": 1
  }'

# Intermediate Native SQL: GROUP BY + HAVING
curl "http://localhost:8082/api/products/stats/by-category?minProductCount=1"

# Intermediate Native SQL: CTE + Window Function (DENSE_RANK)
curl "http://localhost:8082/api/products/top-expensive?topN=2"

# Java Stream Aggregation Summary
curl http://localhost:8082/api/products/summary

# Delete
curl -X DELETE http://localhost:8082/api/products/1
```
