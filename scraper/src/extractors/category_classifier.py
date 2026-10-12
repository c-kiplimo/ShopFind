"""
Category Classifier

Classifies products into categories based on text content.
"""

from typing import Optional
from config.config import config


class CategoryClassifier:
    """Classifies products into categories"""

    def __init__(self):
        self.categories = config.CATEGORIES
        self.keywords = config.CATEGORY_KEYWORDS

    def classify(self, text: str) -> str:
        """
        Classify product into category based on text.

        Args:
            text: Product name or caption

        Returns:
            Category name or 'Other' if no match
        """
        if not text:
            return 'Other'

        text_lower = text.lower()

        # Count keyword matches for each category
        matches = {}
        for category, keywords in self.keywords.items():
            count = sum(1 for keyword in keywords if keyword in text_lower)
            if count > 0:
                matches[category] = count

        # Return category with most matches
        if matches:
            return max(matches, key=matches.get)

        return 'Other'

    def classify_with_confidence(self, text: str) -> tuple[str, float]:
        """
        Classify product with confidence score.

        Args:
            text: Product name or caption

        Returns:
            Tuple of (category, confidence)
        """
        if not text:
            return ('Other', 0.0)

        text_lower = text.lower()
        total_words = len(text_lower.split())

        if total_words == 0:
            return ('Other', 0.0)

        # Count keyword matches for each category
        matches = {}
        for category, keywords in self.keywords.items():
            count = sum(1 for keyword in keywords if keyword in text_lower)
            if count > 0:
                matches[category] = count

        if not matches:
            return ('Other', 0.0)

        # Get category with most matches
        best_category = max(matches, key=matches.get)
        match_count = matches[best_category]

        # Calculate confidence (percentage of matched words)
        confidence = min(match_count / total_words, 1.0)

        return (best_category, confidence)
