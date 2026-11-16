import { api } from './api';
import { Page, Shelter, ShelterInput, StaffMember } from './types';

// Every call goes through api(). All but listShelters need the ADMIN role.

/** The first 50 shelters, sorted by name (GET /shelters). 50 is the most the API gives in one page. */
export async function listShelters(): Promise<Shelter[]> {
  const page = await api<Page<Shelter>>('/shelters?page=0&size=50');
  return page.content;
}

/** Creates a shelter (POST /admin/shelters). */
export function createShelter(input: ShelterInput): Promise<Shelter> {
  return api<Shelter>('/admin/shelters', { method: 'POST', body: JSON.stringify(input) });
}

/** Replaces a shelter's details (PUT /admin/shelters/{shelterId}). */
export function updateShelter(shelterId: number, input: ShelterInput): Promise<Shelter> {
  return api<Shelter>(`/admin/shelters/${shelterId}`, { method: 'PUT', body: JSON.stringify(input) });
}

/** Deletes a shelter with its posts (DELETE /admin/shelters/{shelterId}). */
export async function deleteShelter(shelterId: number): Promise<void> {
  await api<void>(`/admin/shelters/${shelterId}`, { method: 'DELETE' });
}

/** The shelter's staff, sorted by email (GET /admin/shelters/{shelterId}/staff). */
export function listStaff(shelterId: number): Promise<StaffMember[]> {
  return api<StaffMember[]>(`/admin/shelters/${shelterId}/staff`);
}

/** Makes the user staff for the shelter (PUT /admin/shelters/{shelterId}/staff/{userId}). */
export async function addStaff(shelterId: number, userId: number): Promise<void> {
  await api<void>(`/admin/shelters/${shelterId}/staff/${userId}`, { method: 'PUT' });
}

/** Removes the user from the shelter's staff (DELETE /admin/shelters/{shelterId}/staff/{userId}). */
export async function removeStaff(shelterId: number, userId: number): Promise<void> {
  await api<void>(`/admin/shelters/${shelterId}/staff/${userId}`, { method: 'DELETE' });
}

/** All the admin calls together, so screens can take a fake set in tests. */
export const adminApi = { listShelters, createShelter, updateShelter, deleteShelter, listStaff, addStaff, removeStaff };
export type AdminApi = typeof adminApi;
