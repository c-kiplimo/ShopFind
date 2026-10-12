"""
ShopFind Scraper - Main Entry Point

Orchestrates scraping from Instagram, TikTok, and Facebook.
"""

import sys
import argparse
from loguru import logger
from src.scrapers.instagram_scraper import InstagramScraper
from config.config import config


# Configure logger
logger.remove()
logger.add(
    sys.stdout,
    format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan> - <level>{message}</level>",
    level=config.LOG_LEVEL
)
logger.add(
    config.LOG_FILE,
    rotation="1 day",
    retention="7 days",
    level=config.LOG_LEVEL
)


def scrape_instagram(username: str, max_posts: int = None):
    """
    Scrape Instagram seller.

    Args:
        username: Instagram username
        max_posts: Maximum posts to scrape
    """
    logger.info(f"Starting Instagram scrape for @{username}")

    scraper = InstagramScraper()
    stats = scraper.scrape_and_save(username, max_posts)

    if stats['success']:
        logger.success(f"✓ Successfully scraped @{username}")
        logger.info(f"  - Total products found: {stats['total_products']}")
        logger.info(f"  - Products saved: {stats['saved_products']}")
        logger.info(f"  - Products failed: {stats['failed_products']}")
    else:
        logger.error(f"✗ Failed to scrape @{username}: {stats.get('error')}")


def scrape_multiple_sellers(usernames: list, platform: str = 'instagram', max_posts: int = None):
    """
    Scrape multiple sellers.

    Args:
        usernames: List of usernames
        platform: Platform name
        max_posts: Maximum posts per seller
    """
    total_scraped = 0
    total_saved = 0

    for username in usernames:
        try:
            if platform.lower() == 'instagram':
                scrape_instagram(username, max_posts)
                total_scraped += 1
            else:
                logger.warning(f"Platform {platform} not yet implemented")

        except Exception as e:
            logger.error(f"Error scraping @{username}: {str(e)}")
            continue

    logger.info(f"\n=== Scraping Complete ===")
    logger.info(f"Total sellers scraped: {total_scraped}")


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='ShopFind Social Media Scraper')

    parser.add_argument(
        'username',
        nargs='?',
        help='Username to scrape'
    )

    parser.add_argument(
        '--platform',
        choices=['instagram', 'tiktok', 'facebook'],
        default='instagram',
        help='Social media platform (default: instagram)'
    )

    parser.add_argument(
        '--max-posts',
        type=int,
        help='Maximum number of posts to scrape'
    )

    parser.add_argument(
        '--file',
        help='File containing list of usernames (one per line)'
    )

    parser.add_argument(
        '--list',
        nargs='+',
        help='List of usernames to scrape'
    )

    args = parser.parse_args()

    # Determine which usernames to scrape
    usernames = []

    if args.username:
        usernames.append(args.username)
    elif args.list:
        usernames.extend(args.list)
    elif args.file:
        try:
            with open(args.file, 'r') as f:
                usernames = [line.strip() for line in f if line.strip()]
        except Exception as e:
            logger.error(f"Error reading file: {str(e)}")
            sys.exit(1)
    else:
        # Default sellers for testing
        usernames = ['fashion_ke', 'techstore_ke', 'shoes_nairobi']
        logger.info("No username provided, using default test sellers")

    # Scrape sellers
    scrape_multiple_sellers(usernames, args.platform, args.max_posts)


if __name__ == '__main__':
    main()
