import { api } from './api';

export type Species = 'CAT' | 'DOG';

export interface NearbyPet {
  id: number;
  shelter: { id: number; name: string };
  name: string;
  species: Species;
  ageMonths?: number;
  description?: string;
  photoUrl?: string;
  status: 'AVAILABLE' | 'ADOPTED';
  distanceMiles: number;
}

export interface NearbyPage {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  content: NearbyPet[];
}

export function getNearbyPets(zip: string, distance: number, species?: Species, page = 0, size = 10): Promise<NearbyPage> {
  const params = new URLSearchParams({ zip, distance: String(distance), page: String(page), size: String(size) });
  if (species) params.set('species', species);
  return api<NearbyPage>(`/posts/nearby?${params.toString()}`);
}
