"""
Base Scraper

Abstract base class for all platform scrapers.
"""

from abc import ABC, abstractmethod
from typing import List, Dict, Optional
import time
import requests
from loguru import logger
from config.config import config


class BaseScraper(ABC):
    """Abstract base class for scrapers"""

    def __init__(self, platform: str):
        self.platform = platform
        self.api_url = config.API_BASE_URL
        self.delay = config.SCRAPER_DELAY
        self.max_retries = config.SCRAPER_MAX_RETRIES

    @abstractmethod
    def scrape_seller(self, username: str) -> Dict:
        """
        Scrape seller profile.

        Args:
            username: Seller username

        Returns:
            Seller data dictionary
        """
        pass

    @abstractmethod
    def scrape_products(self, username: str, max_posts: int = 50) -> List[Dict]:
        """
        Scrape products from seller.

        Args:
            username: Seller username
            max_posts: Maximum number of posts to scrape

        Returns:
            List of product dictionaries
        """
        pass

    def save_seller(self, seller_data: Dict) -> Optional[Dict]:
        """
        Save seller to backend API.

        Args:
            seller_data: Seller data dictionary

        Returns:
            API response or None if failed
        """
        try:
            url = f"{self.api_url}/sellers"
            response = requests.post(
                url,
                json=seller_data,
                timeout=config.API_TIMEOUT
            )

            if response.status_code in [200, 201]:
                logger.info(f"Saved seller: {seller_data['username']}")
                return response.json()
            elif response.status_code == 409:
                # Seller already exists, update instead
                logger.info(f"Seller exists, updating: {seller_data['username']}")
                return self.update_seller(seller_data)
            else:
                logger.error(f"Failed to save seller: {response.status_code} - {response.text}")
                return None

        except Exception as e:
            logger.error(f"Error saving seller: {str(e)}")
            return None

    def update_seller(self, seller_data: Dict) -> Optional[Dict]:
        """
        Update existing seller.

        Args:
            seller_data: Seller data dictionary

        Returns:
            API response or None if failed
        """
        try:
            username = seller_data['username']
            url = f"{self.api_url}/sellers/{username}"
            response = requests.patch(
                url,
                json=seller_data,
                timeout=config.API_TIMEOUT
            )

            if response.status_code == 200:
                logger.info(f"Updated seller: {username}")
                return response.json()
            else:
                logger.error(f"Failed to update seller: {response.status_code}")
                return None

        except Exception as e:
            logger.error(f"Error updating seller: {str(e)}")
            return None

    def save_product(self, product_data: Dict) -> Optional[Dict]:
        """
        Save product to backend API.

        Args:
            product_data: Product data dictionary

        Returns:
            API response or None if failed
        """
        try:
            url = f"{self.api_url}/products"
            response = requests.post(
                url,
                json=product_data,
                timeout=config.API_TIMEOUT
            )

            if response.status_code in [200, 201]:
                logger.info(f"Saved product: {product_data['productName']}")
                return response.json()
            else:
                logger.error(f"Failed to save product: {response.status_code} - {response.text}")
                return None

        except Exception as e:
            logger.error(f"Error saving product: {str(e)}")
            return None

    def save_products_batch(self, products: List[Dict]) -> int:
        """
        Save multiple products to backend API.

        Args:
            products: List of product dictionaries

        Returns:
            Number of successfully saved products
        """
        success_count = 0

        for product in products:
            if self.save_product(product):
                success_count += 1
            time.sleep(0.5)  # Rate limiting

        return success_count

    def delay_request(self):
        """Add delay between requests to avoid rate limiting"""
        time.sleep(self.delay)
