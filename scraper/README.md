# ShopFind Scraper

Python-based scraper for extracting product data from Instagram, TikTok, and Facebook sellers.

## Features

- **Instagram Scraping**: Extract products from Instagram profiles
- **Price Extraction**: Automatically detect prices in KES
- **Phone Extraction**: Extract Kenyan phone numbers
- **Category Classification**: AI-powered product categorization
- **Batch Processing**: Scrape multiple sellers efficiently
- **API Integration**: Automatic sync with ShopFind backend

## Installation

### Prerequisites

- Python 3.10 or higher
- ShopFind backend running on `localhost:8080`
- PostgreSQL database

### Setup

1. **Install dependencies**:
```bash
cd scraper
pip install -r requirements.txt
```

2. **Configure environment**:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. **Test connection**:
```bash
python main.py --help
```

## Usage

### Scrape Single Seller

```bash
# Scrape Instagram seller
python main.py fashion_ke

# Limit number of posts
python main.py fashion_ke --max-posts 20
```

### Scrape Multiple Sellers

```bash
# From command line
python main.py --list fashion_ke techstore_ke shoes_nairobi

# From file
python main.py --file sellers.txt
```

### Platform Options

```bash
# Instagram (default)
python main.py username --platform instagram

# TikTok (coming soon)
python main.py username --platform tiktok

# Facebook (coming soon)
python main.py username --platform facebook
```

## Architecture

```
scraper/
├── src/
│   ├── scrapers/
│   │   ├── base_scraper.py       # Abstract base class
│   │   ├── instagram_scraper.py  # Instagram implementation
│   │   ├── tiktok_scraper.py     # TikTok (coming soon)
│   │   └── facebook_scraper.py   # Facebook (coming soon)
│   └── extractors/
│       ├── price_extractor.py    # Price extraction
│       ├── phone_extractor.py    # Phone extraction
│       └── category_classifier.py # Category classification
├── config/
│   └── config.py                  # Configuration
├── main.py                        # Entry point
└── requirements.txt               # Dependencies
```

## Components

### Price Extractor
Extracts prices from text using regex patterns:
- Supports formats: `KES 1500`, `1500 KES`, `KSH 1500`, `@1500`
- Detects original vs. sale prices
- Filters unrealistic prices

### Phone Extractor
Extracts Kenyan phone numbers:
- Normalizes to `254XXXXXXXXX` format
- Supports formats: `0712345678`, `+254712345678`, `254712345678`
- Validates Kenyan number patterns

### Category Classifier
Classifies products into categories:
- 11 categories: Clothing, Shoes, Electronics, Beauty, etc.
- Keyword-based matching
- Confidence scoring

### Instagram Scraper
Scrapes Instagram profiles:
- Profile information (bio, followers, profile pic)
- Product posts with images
- Automatic price and phone extraction
- Rate limiting and error handling

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | shopfind |
| `API_BASE_URL` | Backend API URL | http://localhost:8080/api |
| `INSTAGRAM_USERNAME` | Instagram login | (optional) |
| `INSTAGRAM_PASSWORD` | Instagram password | (optional) |
| `INSTAGRAM_MAX_POSTS` | Max posts per seller | 50 |
| `SCRAPER_DELAY` | Delay between requests (seconds) | 3 |
| `LOG_LEVEL` | Logging level | INFO |

### Categories

Pre-configured categories with keyword matching:
- Clothing
- Shoes
- Electronics
- Beauty & Cosmetics
- Accessories
- Home & Living
- Bags & Purses
- Watches
- Jewelry
- Sports & Fitness
- Other

## Examples

### Example Output

```
2024-01-15 10:30:15 | INFO     | Scraping products from @fashion_ke
2024-01-15 10:30:18 | INFO     | Extracted product: Cuban Collar Shirt
2024-01-15 10:30:21 | INFO     | Extracted product: Denim Jeans
2024-01-15 10:30:24 | INFO     | Scraped 25 products from @fashion_ke
2024-01-15 10:30:30 | SUCCESS  | ✓ Successfully scraped @fashion_ke
  - Total products found: 25
  - Products saved: 24
  - Products failed: 1
```

### Example Sellers File

```txt
# sellers.txt
fashion_ke
techstore_ke
shoes_nairobi
beauty_kenya
electronics_nairobi
```

## Error Handling

The scraper includes comprehensive error handling:
- Retry logic for network failures
- Graceful degradation when data is missing
- Detailed logging of errors
- Continues on individual product failures

## Rate Limiting

Respects platform rate limits:
- Configurable delay between requests
- Automatic backoff on rate limit errors
- Login support for higher limits (Instagram)

## Logging

Structured logging with:
- Console output with colors
- File logging with rotation
- Configurable log levels
- Detailed error traces

## Troubleshooting

### Common Issues

**Instagram Login Failed**:
- Verify credentials in `.env`
- Try without login (anonymous access)
- Check Instagram security settings

**No Products Found**:
- Verify seller has public posts
- Check if posts contain prices
- Review extraction patterns in config

**API Connection Failed**:
- Ensure backend is running
- Check `API_BASE_URL` in `.env`
- Verify network connectivity

## Development

### Adding New Platform

1. Create scraper class inheriting from `BaseScraper`
2. Implement `scrape_seller()` and `scrape_products()`
3. Add platform-specific extraction logic
4. Register in `main.py`

### Testing

```bash
# Run tests
pytest tests/

# Test specific component
pytest tests/test_extractors.py
```

## Roadmap

- [x] Instagram scraper
- [ ] TikTok scraper
- [ ] Facebook scraper
- [ ] Automated scheduling
- [ ] Duplicate detection
- [ ] Image download and hosting
- [ ] Advanced ML classification
- [ ] Sentiment analysis
- [ ] Multi-language support

## License

MIT License

## Support

For issues and questions:
- GitHub Issues: https://github.com/c-kiplimo/ShopFind/issues
- Email: [Your email]
