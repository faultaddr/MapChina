import type { MetadataRoute } from 'next';

export default function sitemap(): MetadataRoute.Sitemap {
  const baseUrl = 'https://mapchina.com';
  const locales = ['zh', 'en'];
  const pages = ['', '/attractions', '/community', '/about', '/download'];

  return locales.flatMap((locale) =>
    pages.map((page) => ({
      url: `${baseUrl}/${locale}${page}`,
      lastModified: new Date(),
      changeFrequency: page === '' ? 'weekly' : 'daily',
      priority: page === '' ? 1 : 0.8,
    })),
  );
}
