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

interface NearbyPage { content: NearbyPet[] }

export function getNearbyPets(zip: string, distance: number, species?: Species): Promise<NearbyPet[]> {
  const params = new URLSearchParams({ zip, distance: String(distance) });
  if (species) params.set('species', species);
  return api<NearbyPage>(`/posts/nearby?${params.toString()}`).then((page) => page.content);
}
