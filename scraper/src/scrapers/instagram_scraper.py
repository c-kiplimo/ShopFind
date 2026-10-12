"""
Instagram Scraper

Scrapes product data from Instagram profiles.
"""

from typing import List, Dict, Optional
import instaloader
from loguru import logger
from .base_scraper import BaseScraper
from config.config import config
from src.extractors.price_extractor import PriceExtractor
from src.extractors.phone_extractor import PhoneExtractor
from src.extractors.category_classifier import CategoryClassifier


class InstagramScraper(BaseScraper):
    """Scraper for Instagram platform"""

    def __init__(self):
        super().__init__(platform='INSTAGRAM')
        self.loader = instaloader.Instaloader()
        self.price_extractor = PriceExtractor()
        self.phone_extractor = PhoneExtractor()
        self.category_classifier = CategoryClassifier()

        # Login if credentials provided
        if config.INSTAGRAM_USERNAME and config.INSTAGRAM_PASSWORD:
            try:
                self.loader.login(
                    config.INSTAGRAM_USERNAME,
                    config.INSTAGRAM_PASSWORD
                )
                logger.info("Logged in to Instagram")
            except Exception as e:
                logger.warning(f"Instagram login failed: {str(e)}")

    def scrape_seller(self, username: str) -> Dict:
        """
        Scrape Instagram profile for seller information.

        Args:
            username: Instagram username

        Returns:
            Seller data dictionary
        """
        try:
            profile = instaloader.Profile.from_username(
                self.loader.context,
                username
            )

            # Extract phone from bio
            phone = self.phone_extractor.extract(profile.biography)

            # Extract location from bio (simple heuristic)
            location = self._extract_location(profile.biography)

            seller_data = {
                'username': username,
                'platform': self.platform,
                'bio': profile.biography,
                'profilePicUrl': profile.profile_pic_url,
                'followerCount': profile.followers,
                'phone': phone,
                'whatsapp': phone,  # Assume WhatsApp same as phone
                'location': location or 'Nairobi',
                'verified': profile.is_verified
            }

            logger.info(f"Scraped seller: {username}")
            return seller_data

        except Exception as e:
            logger.error(f"Error scraping seller {username}: {str(e)}")
            return None

    def scrape_products(self, username: str, max_posts: int = None) -> List[Dict]:
        """
        Scrape products from Instagram profile.

        Args:
            username: Instagram username
            max_posts: Maximum number of posts to scrape

        Returns:
            List of product dictionaries
        """
        max_posts = max_posts or config.INSTAGRAM_MAX_POSTS
        products = []

        try:
            profile = instaloader.Profile.from_username(
                self.loader.context,
                username
            )

            logger.info(f"Scraping products from @{username}")

            for idx, post in enumerate(profile.get_posts()):
                if idx >= max_posts:
                    break

                try:
                    product = self._extract_product_from_post(post, username)
                    if product:
                        products.append(product)
                        logger.info(f"Extracted product: {product['productName']}")

                except Exception as e:
                    logger.error(f"Error extracting product from post: {str(e)}")
                    continue

                self.delay_request()

            logger.info(f"Scraped {len(products)} products from @{username}")
            return products

        except Exception as e:
            logger.error(f"Error scraping products from {username}: {str(e)}")
            return products

    def _extract_product_from_post(self, post, username: str) -> Optional[Dict]:
        """
        Extract product data from Instagram post.

        Args:
            post: Instaloader Post object
            username: Seller username

        Returns:
            Product dictionary or None
        """
        caption = post.caption or ""

        # Extract price
        price_data = self.price_extractor.extract(caption)
        if not price_data:
            return None  # Skip posts without price

        price, original_price = price_data

        # Extract product name (first line or first 50 chars of caption)
        product_name = self._extract_product_name(caption)
        if not product_name:
            return None

        # Classify category
        category = self.category_classifier.classify(caption + " " + product_name)

        # Extract phone
        phone = self.phone_extractor.extract(caption)

        # Get image URL
        image_url = post.url

        # Get post URL
        post_url = f"https://instagram.com/p/{post.shortcode}"

        product_data = {
            'seller': username,
            'productName': product_name,
            'caption': caption[:500] if len(caption) > 500 else caption,
            'price': price,
            'originalPrice': original_price,
            'currency': 'KES',
            'category': category,
            'imageUrl': image_url,
            'postUrl': post_url,
            'phone': phone,
            'whatsapp': phone,
            'location': 'Nairobi',
            'platform': self.platform
        }

        return product_data

    def _extract_product_name(self, caption: str) -> Optional[str]:
        """
        Extract product name from caption.

        Args:
            caption: Post caption

        Returns:
            Product name or None
        """
        if not caption:
            return None

        # Get first line or first 50 characters
        lines = caption.split('\n')
        first_line = lines[0].strip()

        # Clean up common prefixes
        first_line = first_line.replace('SOLD', '').replace('sold', '')
        first_line = first_line.replace('NEW', '').replace('new', '')
        first_line = first_line.strip('- ').strip()

        if len(first_line) > 100:
            first_line = first_line[:100]

        return first_line if first_line else None

    def _extract_location(self, bio: str) -> Optional[str]:
        """
        Extract location from bio.

        Args:
            bio: Profile biography

        Returns:
            Location string or None
        """
        if not bio:
            return None

        # Common Kenyan cities
        cities = ['Nairobi', 'Mombasa', 'Kisumu', 'Nakuru', 'Eldoret', 'Thika', 'Malindi']

        bio_lower = bio.lower()
        for city in cities:
            if city.lower() in bio_lower:
                return city

        return None

    def scrape_and_save(self, username: str, max_posts: int = None) -> Dict:
        """
        Scrape seller and products, then save to backend.

        Args:
            username: Instagram username
            max_posts: Maximum posts to scrape

        Returns:
            Dictionary with statistics
        """
        logger.info(f"Starting scrape for @{username}")

        # Scrape seller
        seller_data = self.scrape_seller(username)
        if not seller_data:
            return {'success': False, 'error': 'Failed to scrape seller'}

        # Save seller
        self.save_seller(seller_data)

        # Scrape products
        products = self.scrape_products(username, max_posts)

        # Save products
        saved_count = self.save_products_batch(products)

        stats = {
            'success': True,
            'username': username,
            'total_products': len(products),
            'saved_products': saved_count,
            'failed_products': len(products) - saved_count
        }

        logger.info(f"Scrape complete: {stats}")
        return stats
