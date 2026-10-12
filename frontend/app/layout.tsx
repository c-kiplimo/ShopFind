import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'ShopFind - Kenya\'s Social Commerce Search Engine',
  description: 'Discover products from Instagram, TikTok, and Facebook sellers across Kenya',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <nav className="bg-white border-b">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between h-16">
              <div className="flex items-center">
                <a href="/" className="text-2xl font-bold text-primary-600">
                  ShopFind
                </a>
                <span className="ml-2 text-sm text-gray-500">🇰🇪</span>
              </div>
              <div className="flex items-center space-x-4">
                <a href="/search" className="text-gray-700 hover:text-primary-600">
                  Browse
                </a>
                <a href="/sellers" className="text-gray-700 hover:text-primary-600">
                  Sellers
                </a>
                <a href="/about" className="text-gray-700 hover:text-primary-600">
                  About
                </a>
              </div>
            </div>
          </div>
        </nav>

        <main>{children}</main>

        <footer className="bg-gray-50 border-t mt-auto">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="text-center text-gray-600">
              <p>&copy; 2024 ShopFind. Connecting Kenyan buyers with social commerce sellers.</p>
              <p className="mt-2 text-sm">Made with ❤️ in Kenya</p>
            </div>
          </div>
        </footer>
      </body>
    </html>
  )
}
