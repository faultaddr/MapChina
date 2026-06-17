import type { Metadata } from 'next';
import { Noto_Serif_SC, Noto_Sans_SC, Playfair_Display, Inter } from 'next/font/google';
import './globals.css';

const notoSerif = Noto_Serif_SC({
  subsets: ['latin'],
  weight: ['600', '700'],
  variable: '--font-noto-serif',
  display: 'swap',
});

const notoSans = Noto_Sans_SC({
  subsets: ['latin'],
  weight: ['400', '500'],
  variable: '--font-noto-sans',
  display: 'swap',
});

const playfair = Playfair_Display({
  subsets: ['latin'],
  weight: ['600', '700'],
  variable: '--font-playfair',
  display: 'swap',
});

const inter = Inter({
  subsets: ['latin'],
  weight: ['400', '500'],
  variable: '--font-inter',
  display: 'swap',
});

export const metadata: Metadata = {
  title: 'MapChina',
  description: '用地图点亮你的中国足迹',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html suppressHydrationWarning>
      <body className={`${notoSerif.variable} ${notoSans.variable} ${playfair.variable} ${inter.variable} font-body bg-bg text-ink antialiased`}>
        {children}
      </body>
    </html>
  );
}
