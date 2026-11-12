// Shared types that match the schemas in api/src/main/resources/static/openapi.yaml.
// Favorites, Nearby and Posts all get posts back from the API

// CAT or DOG, the Species schema.
export type Species = 'CAT' | 'DOG';

// AVAILABLE or ADOPTED, the PostStatus schema.
export type PostStatus = 'AVAILABLE' | 'ADOPTED';

// The shelter's id and name, shown on each post. The ShelterSummary schema.
export interface ShelterSummary {
  id: number;
  name: string;
}


export interface Post {
  id: number;
  shelter: ShelterSummary;
  name: string;
  species: Species;
  ageMonths?: number;
  description?: string;
  photoUrl?: string;
  status: PostStatus;
  // Miles from the user's chosenZIP code.
  distanceMiles?: number;
  // True if the current user saved this post to their favorites
  favorite?: boolean;
  createdAt?: string;
}

/**
 * One page of results, the PageInfo schema plus its content list.
 * "T" is whatever the list holds
 */
export interface Page<T> {
  // Current page, starting at 0
  page: number;
  // How many things fit on one page
  size: number;
  // How many items there are in total, across every page
  totalElements: number;
  // How many pages there are in total
  totalPages: number;
  content: T[];
}