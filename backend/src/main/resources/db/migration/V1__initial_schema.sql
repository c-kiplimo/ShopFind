-- V1__initial_schema.sql
-- ShopFind Database Schema

-- Products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,

    -- Basic info
    seller VARCHAR(255) NOT NULL,
    product_name TEXT NOT NULL,
    caption TEXT,

    -- Pricing
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    currency VARCHAR(10) DEFAULT 'KES',

    -- Categorization
    category VARCHAR(100) NOT NULL,
    subcategory VARCHAR(100),

    -- Media
    image_url TEXT NOT NULL,
    post_url TEXT NOT NULL,

    -- Contact
    phone VARCHAR(20),
    whatsapp VARCHAR(20),
    email VARCHAR(255),

    -- Location
    location VARCHAR(255) DEFAULT 'Nairobi',
    area VARCHAR(100),

    -- Platform
    platform VARCHAR(50) DEFAULT 'instagram',

    -- Metadata
    scraped_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,

    -- Search optimization
    search_vector tsvector,

    -- Constraints
    CONSTRAINT products_price_positive CHECK (price >= 0),
    CONSTRAINT products_platform_valid CHECK (platform IN ('instagram', 'tiktok', 'facebook'))
);

-- Sellers table
CREATE TABLE sellers (
    username VARCHAR(255) PRIMARY KEY,
    platform VARCHAR(50) NOT NULL,

    -- Contact
    phone VARCHAR(20),
    whatsapp VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),

    -- Profile
    bio TEXT,
    profile_pic_url TEXT,
    follower_count INTEGER DEFAULT 0,

    -- Location
    location VARCHAR(255),
    area VARCHAR(100),

    -- Stats
    total_products INTEGER DEFAULT 0,
    avg_price DECIMAL(10,2),
    total_views INTEGER DEFAULT 0,
    total_clicks INTEGER DEFAULT 0,

    -- Features
    verified BOOLEAN DEFAULT false,
    featured BOOLEAN DEFAULT false,
    featured_until DATE,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_scraped TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT sellers_platform_valid CHECK (platform IN ('instagram', 'tiktok', 'facebook'))
);

-- Featured listings table (for paid sellers)
CREATE TABLE featured_listings (
    id BIGSERIAL PRIMARY KEY,
    seller_username VARCHAR(255) REFERENCES sellers(username) ON DELETE CASCADE,

    -- Subscription
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true,

    -- Payment
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'KES',
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255) UNIQUE,

    -- Auto-renewal
    auto_renew BOOLEAN DEFAULT false,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT featured_listings_amount_positive CHECK (amount >= 0),
    CONSTRAINT featured_listings_dates_valid CHECK (expires_at > started_at)
);

-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,

    -- Seller
    seller_username VARCHAR(255) REFERENCES sellers(username) ON DELETE SET NULL,

    -- M-Pesa details
    phone_number VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    merchant_request_id VARCHAR(255),
    checkout_request_id VARCHAR(255),

    -- Status
    status VARCHAR(50) DEFAULT 'pending',
    result_code VARCHAR(10),
    result_desc TEXT,

    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    -- Constraints
    CONSTRAINT payments_amount_positive CHECK (amount >= 0),
    CONSTRAINT payments_status_valid CHECK (status IN ('pending', 'completed', 'failed', 'cancelled'))
);

-- Analytics table (seller dashboard)
CREATE TABLE analytics (
    id BIGSERIAL PRIMARY KEY,
    seller_username VARCHAR(255) REFERENCES sellers(username) ON DELETE CASCADE,

    -- Metrics
    date DATE NOT NULL,
    views INTEGER DEFAULT 0,
    clicks INTEGER DEFAULT 0,
    whatsapp_clicks INTEGER DEFAULT 0,
    instagram_clicks INTEGER DEFAULT 0,

    -- Engagement
    searches_found INTEGER DEFAULT 0,
    avg_position DECIMAL(5,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(seller_username, date)
);

-- Indexes for performance
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_location ON products(location);
CREATE INDEX idx_products_seller ON products(seller);
CREATE INDEX idx_products_platform ON products(platform);
CREATE INDEX idx_products_active ON products(is_active);
CREATE INDEX idx_products_category_price ON products(category, price);

CREATE INDEX idx_sellers_featured ON sellers(featured, featured_until);
CREATE INDEX idx_sellers_location ON sellers(location);
CREATE INDEX idx_sellers_platform ON sellers(platform);

CREATE INDEX idx_featured_listings_seller ON featured_listings(seller_username);
CREATE INDEX idx_featured_listings_active ON featured_listings(is_active, expires_at);

CREATE INDEX idx_payments_seller ON payments(seller_username);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created ON payments(created_at);

CREATE INDEX idx_analytics_seller_date ON analytics(seller_username, date);

-- Full-text search index
CREATE INDEX idx_products_search ON products USING GIN(search_vector);

-- Function to update search vector
CREATE OR REPLACE FUNCTION update_search_vector() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.product_name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.caption, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.category, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.seller, '')), 'D');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Trigger for search vector update
CREATE TRIGGER tsvector_update BEFORE INSERT OR UPDATE
ON products FOR EACH ROW EXECUTE FUNCTION update_search_vector();

-- Function to update last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated() RETURNS trigger AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Trigger for last_updated
CREATE TRIGGER products_last_updated BEFORE UPDATE
ON products FOR EACH ROW EXECUTE FUNCTION update_last_updated();

-- Sample data for testing
INSERT INTO sellers (username, platform, phone, whatsapp, location, bio, verified) VALUES
('fashion_ke', 'instagram', '254712345678', '254712345678', 'Nairobi', 'Best fashion in Kenya', true),
('techstore_ke', 'instagram', '254723456789', '254723456789', 'Nairobi', 'Quality electronics', false),
('shoes_nairobi', 'tiktok', '254734567890', '254734567890', 'Nairobi', 'Footwear specialist', false);

INSERT INTO products (seller, product_name, caption, price, category, image_url, post_url, phone, whatsapp, location, platform) VALUES
('fashion_ke', 'Cuban Shirt', 'Premium cuban collar shirt - KSH 1500', 1500.00, 'Clothing', 'https://example.com/image1.jpg', 'https://instagram.com/p/abc123', '254712345678', '254712345678', 'Nairobi', 'instagram'),
('techstore_ke', 'iPhone 13', 'iPhone 13 128GB - KSH 65000', 65000.00, 'Electronics', 'https://example.com/image2.jpg', 'https://instagram.com/p/def456', '254723456789', '254723456789', 'Nairobi', 'instagram'),
('shoes_nairobi', 'Nike Air Max', 'Original Nike Air Max - KSH 8500', 8500.00, 'Shoes', 'https://example.com/image3.jpg', 'https://tiktok.com/@shoes/video/123', '254734567890', '254734567890', 'Nairobi', 'tiktok');
