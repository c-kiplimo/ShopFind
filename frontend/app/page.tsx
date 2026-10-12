'use client'

import { useState, useEffect } from 'react'
import { productApi, Product } from '@/lib/api'

export default function Home() {
  const [searchQuery, setSearchQuery] = useState('')
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([])
  const [newProducts, setNewProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadProducts()
  }, [])

  const loadProducts = async () => {
    try {
      const [featured, newProds] = await Promise.all([
        productApi.getFeatured(12),
        productApi.getNew(12)
      ])
      setFeaturedProducts(featured)
      setNewProducts(newProds)
    } catch (error) {
      console.error('Error loading products:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchQuery.trim()) {
      window.location.href = `/search?q=${encodeURIComponent(searchQuery)}`
    }
  }

  const handleWhatsAppClick = async (product: Product) => {
    if (product.whatsappLink) {
      await productApi.trackClick(product.id!, 'whatsapp')
      window.open(product.whatsappLink, '_blank')
    }
  }

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="bg-gradient-to-r from-primary-500 to-primary-700 text-white py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h1 className="text-4xl md:text-6xl font-bold mb-4">
              Discover Products from Social Sellers
            </h1>
            <p className="text-xl md:text-2xl mb-8">
              Find products from Instagram, TikTok, and Facebook sellers across Kenya
            </p>

            {/* Search Bar */}
            <form onSubmit={handleSearch} className="max-w-2xl mx-auto">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search for products, categories, or sellers..."
                  className="flex-1 px-6 py-4 rounded-lg text-gray-900 focus:outline-none focus:ring-2 focus:ring-primary-300"
                />
                <button
                  type="submit"
                  className="px-8 py-4 bg-primary-600 hover:bg-primary-700 rounded-lg font-semibold transition-colors"
                >
                  Search
                </button>
              </div>
            </form>

            {/* Quick Filters */}
            <div className="mt-6 flex flex-wrap justify-center gap-2">
              {['Clothing', 'Shoes', 'Electronics', 'Beauty', 'Accessories'].map((cat) => (
                <a
                  key={cat}
                  href={`/search?category=${cat}`}
                  className="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-full text-sm transition-colors"
                >
                  {cat}
                </a>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Featured Products */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold mb-8">Featured Products</h2>

          {loading ? (
            <div className="text-center py-12">
              <p className="text-gray-500">Loading products...</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {featuredProducts.map((product) => (
                <ProductCard key={product.id} product={product} onWhatsAppClick={handleWhatsAppClick} />
              ))}
            </div>
          )}
        </div>
      </section>

      {/* New Arrivals */}
      <section className="py-16 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold mb-8">New Arrivals</h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {newProducts.map((product) => (
              <ProductCard key={product.id} product={product} onWhatsAppClick={handleWhatsAppClick} />
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}

function ProductCard({ product, onWhatsAppClick }: { product: Product; onWhatsAppClick: (p: Product) => void }) {
  return (
    <div className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow overflow-hidden">
      <a href={`/products/${product.id}`}>
        <img
          src={product.imageUrl}
          alt={product.productName}
          className="w-full h-48 object-cover"
        />
      </a>
      <div className="p-4">
        <a href={`/products/${product.id}`}>
          <h3 className="font-semibold text-gray-900 mb-2 hover:text-primary-600">
            {product.productName}
          </h3>
        </a>

        <div className="flex items-center gap-2 mb-2">
          <span className="text-2xl font-bold text-primary-600">
            {product.currency} {product.price.toLocaleString()}
          </span>
          {product.hasDiscount && product.originalPrice && (
            <span className="text-sm text-gray-500 line-through">
              {product.originalPrice.toLocaleString()}
            </span>
          )}
        </div>

        {product.hasDiscount && (
          <span className="inline-block px-2 py-1 bg-red-100 text-red-600 text-xs font-semibold rounded mb-2">
            {product.discountPercentage}% OFF
          </span>
        )}

        <p className="text-sm text-gray-600 mb-2">
          by <a href={`/sellers/${product.seller}`} className="text-primary-600 hover:underline">
            @{product.seller}
          </a>
        </p>

        <div className="flex items-center gap-2 text-xs text-gray-500 mb-3">
          <span>{product.category}</span>
          <span>•</span>
          <span>{product.location}</span>
          <span>•</span>
          <span>{product.platform}</span>
        </div>

        <button
          onClick={() => onWhatsAppClick(product)}
          className="w-full px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg font-semibold transition-colors"
        >
          WhatsApp Seller
        </button>
      </div>
    </div>
  )
}
