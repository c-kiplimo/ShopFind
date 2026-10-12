/**
 * API Client for ShopFind Backend
 */

import axios from 'axios';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Types
export interface Product {
  id: number;
  seller: string;
  productName: string;
  caption?: string;
  price: number;
  originalPrice?: number;
  currency: string;
  category: string;
  subcategory?: string;
  imageUrl: string;
  postUrl: string;
  phone?: string;
  whatsapp?: string;
  whatsappLink?: string;
  email?: string;
  location: string;
  area?: string;
  platform: string;
  scrapedDate: string;
  lastUpdated: string;
  isActive: boolean;
  hasDiscount: boolean;
  discountPercentage?: number;
  isNew: boolean;
}

export interface SearchParams {
  query?: string;
  category?: string;
  subcategory?: string;
  minPrice?: number;
  maxPrice?: number;
  location?: string;
  area?: string;
  platform?: string;
  seller?: string;
  activeOnly?: boolean;
  newOnly?: boolean;
  withDiscount?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export interface SearchResponse {
  products: Product[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface Seller {
  username: string;
  platform: string;
  phone?: string;
  whatsapp?: string;
  whatsappLink?: string;
  email?: string;
  website?: string;
  bio?: string;
  profilePicUrl?: string;
  followerCount: number;
  location?: string;
  area?: string;
  totalProducts: number;
  avgPrice?: number;
  totalViews: number;
  totalClicks: number;
  engagementRate?: number;
  verified: boolean;
  featured: boolean;
  featuredUntil?: string;
  isFeaturedNow: boolean;
  featuredDaysRemaining?: number;
  platformLink: string;
}

// API Methods
export const productApi = {
  search: async (params: SearchParams): Promise<SearchResponse> => {
    const { data } = await api.get('/products/search', { params });
    return data;
  },

  getById: async (id: number): Promise<Product> => {
    const { data } = await api.get(`/products/${id}`);
    return data;
  },

  getBySeller: async (seller: string): Promise<Product[]> => {
    const { data } = await api.get(`/products/seller/${seller}`);
    return data;
  },

  getByCategory: async (category: string): Promise<Product[]> => {
    const { data } = await api.get(`/products/category/${category}`);
    return data;
  },

  getNew: async (limit: number = 20): Promise<Product[]> => {
    const { data } = await api.get('/products/new', { params: { limit } });
    return data;
  },

  getDiscount: async (limit: number = 20): Promise<Product[]> => {
    const { data } = await api.get('/products/discount', { params: { limit } });
    return data;
  },

  getFeatured: async (limit: number = 20): Promise<Product[]> => {
    const { data } = await api.get('/products/featured', { params: { limit } });
    return data;
  },

  getCategories: async (): Promise<string[]> => {
    const { data } = await api.get('/products/categories');
    return data;
  },

  getLocations: async (): Promise<string[]> => {
    const { data } = await api.get('/products/locations');
    return data;
  },

  trackClick: async (id: number, type: string): Promise<void> => {
    await api.post(`/products/${id}/click`, null, { params: { type } });
  },
};

export const sellerApi = {
  getAll: async (page: number = 0, size: number = 20): Promise<Seller[]> => {
    const { data } = await api.get('/sellers', { params: { page, size } });
    return data;
  },

  getByUsername: async (username: string): Promise<Seller> => {
    const { data } = await api.get(`/sellers/${username}`);
    return data;
  },

  getFeatured: async (): Promise<Seller[]> => {
    const { data } = await api.get('/sellers/featured');
    return data;
  },

  getVerified: async (): Promise<Seller[]> => {
    const { data } = await api.get('/sellers/verified');
    return data;
  },

  getTop: async (limit: number = 10): Promise<Seller[]> => {
    const { data } = await api.get('/sellers/top', { params: { limit } });
    return data;
  },
};

export default api;
