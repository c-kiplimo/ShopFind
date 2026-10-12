"""
Price Extractor

Extracts price information from text using regex patterns.
"""

import re
from typing import Optional, Tuple
from config.config import config


class PriceExtractor:
    """Extracts price from text content"""

    def __init__(self):
        self.patterns = config.PRICE_PATTERNS

    def extract(self, text: str) -> Optional[Tuple[float, Optional[float]]]:
        """
        Extract price from text.

        Args:
            text: Text containing price information

        Returns:
            Tuple of (price, original_price) or None if no price found
        """
        if not text:
            return None

        # Clean text
        text = text.replace(',', '')

        prices = []

        for pattern in self.patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            for match in matches:
                try:
                    price_str = match.replace(',', '')
                    price = float(price_str)
                    # Filter realistic prices (100 - 500,000 KES)
                    if 100 <= price <= 500000:
                        prices.append(price)
                except (ValueError, AttributeError):
                    continue

        if not prices:
            return None

        # If multiple prices found, assume first is current, second is original
        if len(prices) >= 2:
            current_price = min(prices)
            original_price = max(prices)
            if original_price > current_price:
                return (current_price, original_price)
            return (current_price, None)

        return (prices[0], None)

    def extract_from_caption(self, caption: str) -> Optional[Tuple[float, Optional[float]]]:
        """
        Extract price from Instagram/social media caption.

        Args:
            caption: Caption text

        Returns:
            Tuple of (price, original_price) or None
        """
        return self.extract(caption)
