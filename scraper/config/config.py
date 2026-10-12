"""
ShopFind Scraper Configuration

Manages scraper settings, database connections, and API endpoints.
"""

import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()


class Config:
    """Main configuration class"""

    # Database Configuration
    DB_HOST = os.getenv('DB_HOST', 'localhost')
    DB_PORT = os.getenv('DB_PORT', '5432')
    DB_NAME = os.getenv('DB_NAME', 'shopfind')
    DB_USER = os.getenv('DB_USER', 'postgres')
    DB_PASSWORD = os.getenv('DB_PASSWORD', 'postgres')

    # Database URL
    DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

    # API Configuration
    API_BASE_URL = os.getenv('API_BASE_URL', 'http://localhost:8080/api')
    API_TIMEOUT = int(os.getenv('API_TIMEOUT', '30'))

    # Scraper Configuration
    SCRAPER_USER_AGENT = os.getenv(
        'SCRAPER_USER_AGENT',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    )
    SCRAPER_DELAY = int(os.getenv('SCRAPER_DELAY', '3'))  # Seconds between requests
    SCRAPER_MAX_RETRIES = int(os.getenv('SCRAPER_MAX_RETRIES', '3'))

    # Selenium Configuration
    SELENIUM_HEADLESS = os.getenv('SELENIUM_HEADLESS', 'True').lower() == 'true'
    SELENIUM_DRIVER_PATH = os.getenv('SELENIUM_DRIVER_PATH', None)

    # Instagram Configuration
    INSTAGRAM_USERNAME = os.getenv('INSTAGRAM_USERNAME', '')
    INSTAGRAM_PASSWORD = os.getenv('INSTAGRAM_PASSWORD', '')
    INSTAGRAM_MAX_POSTS = int(os.getenv('INSTAGRAM_MAX_POSTS', '50'))

    # Categories
    CATEGORIES = [
        'Clothing',
        'Shoes',
        'Electronics',
        'Beauty',
        'Accessories',
        'Home & Living',
        'Bags',
        'Watches',
        'Jewelry',
        'Sports',
        'Other'
    ]

    # Category Keywords for Classification
    CATEGORY_KEYWORDS = {
        'Clothing': ['shirt', 'dress', 'jeans', 'trousers', 'pants', 'skirt', 'top', 'blouse', 'sweater', 'jacket', 'coat', 'hoodie', 'tshirt', 't-shirt'],
        'Shoes': ['shoes', 'sneakers', 'boots', 'sandals', 'heels', 'flats', 'loafers', 'slippers', 'nike', 'adidas', 'vans'],
        'Electronics': ['phone', 'laptop', 'tablet', 'iphone', 'samsung', 'macbook', 'airpods', 'earphones', 'headphones', 'charger', 'ps5', 'xbox'],
        'Beauty': ['makeup', 'perfume', 'lipstick', 'foundation', 'mascara', 'skincare', 'cream', 'lotion', 'serum', 'cosmetics'],
        'Accessories': ['belt', 'hat', 'scarf', 'gloves', 'sunglasses', 'tie', 'wallet', 'phone case'],
        'Home & Living': ['furniture', 'decor', 'bed', 'table', 'chair', 'lamp', 'curtain', 'rug', 'pillow'],
        'Bags': ['bag', 'backpack', 'handbag', 'purse', 'clutch', 'tote', 'satchel', 'sling'],
        'Watches': ['watch', 'smartwatch', 'rolex', 'casio', 'seiko', 'timex'],
        'Jewelry': ['necklace', 'bracelet', 'earrings', 'ring', 'chain', 'pendant', 'gold', 'silver'],
        'Sports': ['gym', 'fitness', 'yoga', 'sports', 'athletic', 'running', 'workout']
    }

    # Price Extraction Patterns
    PRICE_PATTERNS = [
        r'KES?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)',
        r'(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)\s*KES?',
        r'KSH?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)',
        r'(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)\s*KSH?',
        r'@?\s*(\d{1,3}(?:,\d{3})*)',
        r'Price:?\s*(\d{1,3}(?:,\d{3})*)',
    ]

    # Phone Number Patterns
    PHONE_PATTERNS = [
        r'254\d{9}',
        r'0\d{9}',
        r'\+254\d{9}',
        r'254\s*\d{3}\s*\d{3}\s*\d{3}',
        r'0\d{3}\s*\d{3}\s*\d{3}',
    ]

    # Logging
    LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')
    LOG_FILE = os.getenv('LOG_FILE', 'scraper.log')

    # Scheduler
    SCHEDULER_ENABLED = os.getenv('SCHEDULER_ENABLED', 'True').lower() == 'true'
    SCHEDULER_INTERVAL_HOURS = int(os.getenv('SCHEDULER_INTERVAL_HOURS', '24'))


# Export config instance
config = Config()
