import { api } from './api';
import { Page, Post } from './types';

// Every call goes through api()

/**
 * Gets one page of the posts the current user has saved (GET /favorites).
 * Returns the whole page.
 * pageNumber starts at 0. pageSize is how many posts per page (the API maybe allows 1 - 50).
 */
export async function getFavorites(pageNumber = 0, pageSize = 20): Promise<Page<Post>> {
  const query = new URLSearchParams({
    page: String(pageNumber),
    size: String(pageSize),
  });
  return api<Page<Post>>(`/favorites?${query.toString()}`);
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