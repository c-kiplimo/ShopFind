"""
Phone Number Extractor

Extracts Kenyan phone numbers from text.
"""

import re
from typing import Optional
from config.config import config


class PhoneExtractor:
    """Extracts Kenyan phone numbers from text"""

    def __init__(self):
        self.patterns = config.PHONE_PATTERNS

    def extract(self, text: str) -> Optional[str]:
        """
        Extract phone number from text.

        Args:
            text: Text containing phone number

        Returns:
            Normalized phone number in 254XXXXXXXXX format or None
        """
        if not text:
            return None

        # Remove common separators
        cleaned_text = text.replace('-', '').replace('.', '').replace('(', '').replace(')', '')

        for pattern in self.patterns:
            matches = re.findall(pattern, cleaned_text)
            if matches:
                phone = matches[0].replace(' ', '')
                return self.normalize_phone(phone)

        return None

    def normalize_phone(self, phone: str) -> str:
        """
        Normalize phone number to 254XXXXXXXXX format.

        Args:
            phone: Raw phone number

        Returns:
            Normalized phone number
        """
        # Remove non-digits
        phone = re.sub(r'\D', '', phone)

        # Convert to 254XXXXXXXXX format
        if phone.startswith('254') and len(phone) == 12:
            return phone
        elif phone.startswith('0') and len(phone) == 10:
            return '254' + phone[1:]
        elif phone.startswith('+254'):
            return phone[1:]
        elif len(phone) == 9:
            return '254' + phone

        return phone

    def is_valid(self, phone: str) -> bool:
        """
        Check if phone number is valid Kenyan number.

        Args:
            phone: Phone number to validate

        Returns:
            True if valid, False otherwise
        """
        pattern = r'^254[0-9]{9}$'
        return bool(re.match(pattern, phone))
