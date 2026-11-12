import { api } from './api';

// Every call goes through api()

/** One pet post, as the API sends it back in the favorites list. */
export interface FavoritePost {
  id: number;
  shelter: { id: number; name: string };
  name: string;
  species: 'CAT' | 'DOG';
  ageMonths?: number;
  description?: string;
  photoUrl?: string;
  status: 'AVAILABLE' | 'ADOPTED';
}

interface FavoritesPage {
  content: FavoritePost[];
}

/** Gets the list of posts the current user has saved (GET /favorites). */
export async function getFavorites(): Promise<FavoritePost[]> {
  const favoritesPage = await api<FavoritesPage>('/favorites');
  return favoritesPage.content;
}

/**
 * Saves a post to the user's favorites (PUT /favorites/{postId}).
 *  PUT so it won't make any duplicates.
 */
export async function saveFavorite(postId: number): Promise<void> {
  await api<void>(`/favorites/${postId}`, { method: 'PUT' });
}

/** Removes a post from the user's favorites (DELETE /favorites/{postId}). */
export async function removeFavorite(postId: number): Promise<void> {
  await api<void>(`/favorites/${postId}`, { method: 'DELETE' });
}