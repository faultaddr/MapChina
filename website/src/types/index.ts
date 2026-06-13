export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  meta: { total: number; page: number; limit: number };
}

export interface Attraction {
  id: string;
  name: string;
  regionId: string;
  level: string;
  latitude: number;
  longitude: number;
  description: string;
  imageUrl?: string;
}

export interface Journal {
  id: string;
  title: string;
  content: string;
  author: { id: string; nickname: string; avatar?: string };
  regionId: string;
  images: string[];
  createdAt: string;
}
