# ShopFind Frontend

Next.js 14 frontend for ShopFind - Kenya's Social Commerce Search Engine.

## Features

- **Product Search**: Full-text search with advanced filtering
- **Product Discovery**: Browse featured products, new arrivals, discounts
- **Category Filtering**: Filter by category, price, location
- **Seller Profiles**: View seller information and products
- **Direct Contact**: One-click WhatsApp integration
- **Responsive Design**: Mobile-first design with Tailwind CSS
- **Server-Side Rendering**: Fast page loads with Next.js SSR

## Tech Stack

- **Next.js 14** - React framework with App Router
- **TypeScript** - Type safety
- **Tailwind CSS** - Utility-first CSS framework
- **Axios** - HTTP client for API calls

## Getting Started

### Prerequisites

- Node.js 18 or higher
- npm or yarn
- ShopFind backend running on `localhost:8080`

### Installation

1. **Install dependencies**:
```bash
cd frontend
npm install
```

2. **Configure environment**:
```bash
cp .env.example .env.local
# Edit .env.local with your API URL
```

3. **Run development server**:
```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build for Production

```bash
npm run build
npm start
```

## Environment Variables

Create a `.env.local` file:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Project Structure

```
frontend/
├── app/
│   ├── layout.tsx          # Root layout with navigation
│   ├── page.tsx             # Homepage
│   ├── search/              # Search results page
│   ├── products/            # Product details page
│   ├── sellers/             # Seller profile page
│   └── globals.css          # Global styles
├── lib/
│   └── api.ts               # API client and types
├── components/              # Reusable components
├── public/                  # Static assets
├── next.config.js           # Next.js configuration
├── tailwind.config.ts       # Tailwind configuration
└── tsconfig.json            # TypeScript configuration
```

## Key Pages

### Homepage (`/`)
- Hero section with search bar
- Quick category filters
- Featured products grid
- New arrivals section

### Search Results (`/search`)
- Advanced filtering (category, price, location)
- Pagination
- Sort options
- Product grid with WhatsApp links

### Product Details (`/products/[id]`)
- Full product information
- Seller details
- Related products
- WhatsApp contact button

### Seller Profile (`/sellers/[username]`)
- Seller information
- All seller products
- Contact information
- Platform links

## API Integration

The frontend uses the ShopFind REST API through the `api.ts` client:

```typescript
import { productApi } from '@/lib/api'

// Search products
const results = await productApi.search({
  query: 'nike shoes',
  category: 'Shoes',
  minPrice: 5000,
  maxPrice: 15000,
  location: 'Nairobi',
  page: 0,
  size: 20
})

// Get product details
const product = await productApi.getById(123)

// Track WhatsApp click
await productApi.trackClick(123, 'whatsapp')
```

## Styling

### Tailwind CSS

Using Tailwind utility classes:

```tsx
<button className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-white rounded-lg">
  Search
</button>
```

### Custom Colors

Primary green color palette defined in `tailwind.config.ts`:

```typescript
primary: {
  50: '#f0fdf4',
  500: '#22c55e',
  600: '#16a34a',
  700: '#15803d',
}
```

## Components

### ProductCard

Reusable product card component:

```tsx
<ProductCard
  product={product}
  onWhatsAppClick={handleWhatsAppClick}
/>
```

Features:
- Product image
- Name and price
- Discount badge
- Seller link
- Category and location tags
- WhatsApp button

## Development

### Adding New Pages

Create a new folder in `app/`:

```
app/
└── new-page/
    └── page.tsx
```

### Adding New API Endpoints

Update `lib/api.ts`:

```typescript
export const myApi = {
  getData: async (): Promise<Data> => {
    const { data } = await api.get('/my-endpoint')
    return data
  }
}
```

## Deployment

### Vercel (Recommended)

```bash
vercel deploy
```

### Docker

```bash
docker build -t shopfind-frontend .
docker run -p 3000:3000 shopfind-frontend
```

### Static Export

```bash
npm run build
# Deploy the 'out' directory
```

## Performance

- **Server-Side Rendering** for fast initial load
- **Image Optimization** with Next.js Image component
- **Code Splitting** automatic with Next.js
- **Lazy Loading** for images and components

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

MIT License

## Support

- GitHub Issues: https://github.com/c-kiplimo/ShopFind/issues
- Email: [Your email]

---

**ShopFind** - Connecting Kenyan buyers with social commerce sellers 🇰🇪
