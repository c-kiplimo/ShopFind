# ShopFind

**Kenya's Social Commerce Search Engine** - Discover products from Instagram, TikTok, and Facebook sellers.

## 🎯 Overview

ShopFind is a comprehensive social commerce search platform that helps Kenyan buyers discover products from social media sellers across Instagram, TikTok, and Facebook. Built with a modern reactive architecture using Kotlin + Spring Boot WebFlux on the backend and Next.js on the frontend.

## ✨ Features

### For Buyers
- **Powerful Search**: Full-text search across all products with advanced filtering
- **Multi-Platform**: Discover products from Instagram, TikTok, and Facebook sellers
- **Smart Filters**: Filter by category, price range, location, platform, and more
- **Direct Contact**: One-click WhatsApp messaging to sellers
- **Product Discovery**: Featured products, new arrivals, and discounted items
- **Location-Based**: Find sellers in your area (Nairobi, Mombasa, Kisumu, etc.)

### For Sellers
- **Featured Listings**: Boost visibility with premium placement (KES 500/month)
- **Analytics Dashboard**: Track views, clicks, and engagement metrics
- **Multi-Channel**: List products from any social platform
- **M-Pesa Integration**: Easy payment for featured listings
- **Performance Metrics**: CTR, search appearances, average position

### Platform Features
- **Reactive Architecture**: Non-blocking, high-performance backend
- **Full-Text Search**: PostgreSQL-powered search with relevance ranking
- **RESTful API**: Well-documented API for frontend integration
- **Automated Scraping**: Python-based scrapers for social platforms
- **Real-time Analytics**: Track seller and product performance

## 🏗️ Architecture

### Backend (Kotlin + Spring Boot)

```
backend/
├── src/main/kotlin/com/shopfind/
│   ├── ShopFindApplication.kt
│   ├── domain/                      # Domain Layer (DDD)
│   │   ├── model/                   # Rich domain entities
│   │   │   ├── Product.kt          # Product aggregate
│   │   │   ├── Seller.kt           # Seller aggregate
│   │   │   ├── Payment.kt          # Payment entity
│   │   │   ├── FeaturedListing.kt  # Featured listing entity
│   │   │   └── Analytics.kt        # Analytics entity
│   │   └── specification/           # Specification pattern
│   │       ├── Specification.kt     # Base specification
│   │       └── ProductSpecifications.kt
│   ├── application/                 # Application Layer
│   │   └── service/                 # Business logic services
│   │       ├── ProductService.kt
│   │       ├── SellerService.kt
│   │       ├── PaymentService.kt
│   │       ├── FeaturedListingService.kt
│   │       ├── AnalyticsService.kt
│   │       └── MessageService.kt
│   ├── infrastructure/              # Infrastructure Layer
│   │   ├── repository/              # R2dbcEntityTemplate repos
│   │   │   ├── ProductRepository.kt
│   │   │   ├── SellerRepository.kt
│   │   │   ├── PaymentRepository.kt
│   │   │   ├── FeaturedListingRepository.kt
│   │   │   └── AnalyticsRepository.kt
│   │   └── config/                  # Configuration
│   │       └── WebConfig.kt
│   └── presentation/                # Presentation Layer
│       ├── controller/              # REST controllers
│       │   ├── ProductController.kt
│       │   └── SellerController.kt
│       └── dto/                     # Data Transfer Objects
│           ├── ProductDto.kt
│           └── SellerDto.kt
└── src/main/resources/
    ├── application.yml              # App configuration
    └── db/migration/                # Flyway migrations
        └── V1__initial_schema.sql
```

### Tech Stack

#### Backend
- **Kotlin 1.9.21** - Modern JVM language
- **Spring Boot 3.2.1** - Application framework
- **Spring WebFlux** - Reactive web framework
- **Spring Data R2DBC** - Reactive database access
- **R2dbcEntityTemplate** - For dynamic queries (not Spring Data repos)
- **PostgreSQL 15** - Primary database
- **Flyway** - Database migrations
- **Kotest** - BDD-style testing
- **Maven** - Build tool

#### Patterns & Principles
- **Domain-Driven Design (DDD)** - Rich domain models with business logic
- **Specification Pattern** - Composable, type-safe query building
- **Service-Oriented Architecture** - Clear separation of concerns
- **Reactive Programming** - Non-blocking, asynchronous operations
- **Repository Pattern** - Data access abstraction with R2dbcEntityTemplate

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **PostgreSQL 15** or higher
- **Maven 3.9+**
- **Docker** (optional, for PostgreSQL)

### Database Setup

1. **Start PostgreSQL** (using Docker):
```bash
docker run --name shopfind-postgres \
  -e POSTGRES_DB=shopfind \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:15
```

2. **Or use existing PostgreSQL**:
```sql
CREATE DATABASE shopfind;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE shopfind TO postgres;
```

### Backend Setup

1. **Clone the repository**:
```bash
git clone https://github.com/c-kiplimo/ShopFind-.git
cd ShopFind-/backend
```

2. **Configure database** (optional, edit `application.yml`):
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/shopfind
    username: postgres
    password: postgres
```

3. **Build the project**:
```bash
mvn clean install
```

4. **Run database migrations**:
```bash
mvn flyway:migrate
```

5. **Start the application**:
```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Verify Installation

```bash
# Health check
curl http://localhost:8080/actuator/health

# Get categories
curl http://localhost:8080/api/products/categories

# Search products
curl "http://localhost:8080/api/products/search?category=Clothing&page=0&size=10"
```

## 📡 API Endpoints

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products/search` | Search products with filters |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products/seller/{seller}` | Get products by seller |
| GET | `/api/products/category/{category}` | Get products by category |
| GET | `/api/products/new` | Get new products |
| GET | `/api/products/discount` | Get discounted products |
| GET | `/api/products/featured` | Get featured products |
| GET | `/api/products/categories` | Get all categories |
| GET | `/api/products/locations` | Get all locations |
| POST | `/api/products` | Create product |
| PATCH | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |
| POST | `/api/products/{id}/click` | Track click analytics |

### Sellers

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/sellers` | Get all sellers |
| GET | `/api/sellers/{username}` | Get seller by username |
| GET | `/api/sellers/featured` | Get featured sellers |
| GET | `/api/sellers/verified` | Get verified sellers |
| GET | `/api/sellers/top` | Get top sellers |
| GET | `/api/sellers/platform/{platform}` | Get sellers by platform |
| POST | `/api/sellers` | Create seller |
| PATCH | `/api/sellers/{username}` | Update seller |
| DELETE | `/api/sellers/{username}` | Delete seller |

### Example Requests

**Search Products:**
```bash
curl -X GET "http://localhost:8080/api/products/search?query=nike&category=Shoes&minPrice=5000&maxPrice=15000&location=Nairobi&platform=INSTAGRAM&page=0&size=20&sortBy=price&sortDirection=asc"
```

**Get Seller Profile:**
```bash
curl -X GET "http://localhost:8080/api/sellers/fashion_ke"
```

**Create Product:**
```bash
curl -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "seller": "fashion_ke",
    "productName": "Nike Air Max",
    "price": 8500,
    "category": "Shoes",
    "imageUrl": "https://example.com/image.jpg",
    "postUrl": "https://instagram.com/p/abc123",
    "platform": "INSTAGRAM",
    "location": "Nairobi",
    "whatsapp": "254712345678"
  }'
```

## 🗄️ Database Schema

### Core Tables

- **products** - Product listings with full-text search
- **sellers** - Seller profiles and stats
- **featured_listings** - Premium seller subscriptions
- **payments** - M-Pesa payment tracking
- **analytics** - Daily seller performance metrics

### Key Features

- **Full-text search** with PostgreSQL `tsvector` and GIN indexes
- **Automatic triggers** for search vector updates
- **Comprehensive indexing** for performance
- **Check constraints** for data integrity
- **Sample data** included for testing

## 🔧 Configuration

### Application Profiles

**Default** (`application.yml`):
- Port: 8080
- Database: `shopfind`
- Logging: DEBUG

**Dev** (`application-dev.yml`):
- Port: 8081
- Database: `shopfind_dev`

**Prod** (`application-prod.yml`):
- Port: from `$PORT`
- Database: from `$DATABASE_URL`
- Logging: WARN/INFO

### Environment Variables

```bash
# Production
export DATABASE_URL=r2dbc:postgresql://host:5432/shopfind
export DATABASE_JDBC_URL=jdbc:postgresql://host:5432/shopfind
export DB_USERNAME=username
export DB_PASSWORD=password
export PORT=8080
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ProductServiceTest

# Generate coverage report
mvn verify
```

## 📊 Monitoring

The application includes Spring Boot Actuator endpoints:

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`
- Info: `GET /actuator/info`

## 🚢 Deployment

### Docker

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
# Build
docker build -t shopfind-backend .

# Run
docker run -p 8080:8080 \
  -e DATABASE_URL=r2dbc:postgresql://host:5432/shopfind \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  shopfind-backend
```

### Heroku

```bash
heroku create shopfind-api
heroku addons:create heroku-postgresql:mini
git push heroku develop:master
```

## 🛠️ Development

### Build Commands

```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run locally
mvn spring-boot:run

# Package JAR
mvn package
```

### Code Quality

```bash
# Format code
mvn kotlin:format

# Static analysis
mvn kotlin:lint
```

## 📝 License

This project is licensed under the MIT License.

## 👥 Contributors

- **Kiplimo** - Initial implementation
- **Claude Sonnet 4.5** - AI Assistant

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Contact

- **GitHub**: [@c-kiplimo](https://github.com/c-kiplimo)
- **Email**: [Your email]

---

**ShopFind** - Connecting Kenyan buyers with social commerce sellers 🇰🇪
