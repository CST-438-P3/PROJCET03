// Shared types that match the schemas in api/src/main/resources/static/openapi.yaml.
// Favorites, Nearby and Posts all get posts back from the API

// CAT or DOG, the Species schema.
export type Species = 'CAT' | 'DOG';

// AVAILABLE or ADOPTED, the PostStatus schema.
export type PostStatus = 'AVAILABLE' | 'ADOPTED';
export type Sex = 'MALE' | 'FEMALE' | 'UNKNOWN';
export type Size = 'SMALL' | 'MEDIUM' | 'LARGE';

// MALE, FEMALE or UNKNOWN, the Sex schema.
export type Sex = 'MALE' | 'FEMALE' | 'UNKNOWN';

// SMALL, MEDIUM or LARGE, the Size schema.
export type Size = 'SMALL' | 'MEDIUM' | 'LARGE';

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
  breed?: string;
  sex?: Sex;
  size?: Size;
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

// A shelter, the Shelter schema.
export interface Shelter {
  id: number;
  name: string;
  address: string;
  zip: string;
  phone?: string;
  email?: string;
}

// What an admin types to create or edit a shelter, the ShelterCreate schema.
export interface ShelterInput {
  name: string;
  address: string;
  zip: string;
  phone: string;
  email: string;
}

// A user who is staff for a shelter, the StaffMember schema.
export interface StaffMember {
  id: number;
  email: string;
  displayName?: string;
}
