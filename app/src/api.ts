import { tokenStore } from './tokenStore';

/** Base URL of the API. EXPO_PUBLIC_* is bundled into the client: never put secrets here. */
export const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';

/** RFC 9457 Problem Details, as returned by the API for every error. */
export interface Problem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
}

/** Thrown for any non-2xx response. */
export class ApiError extends Error {
  constructor(public readonly problem: Problem) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`);
  }
}

/**
 * Calls the API with the stored JWT attached. Every screen goes through this,
 * on web and Android alike, so both platforms share one contract (openapi.yaml).
 */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = await tokenStore.get();
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (init.body) headers.set('Content-Type', 'application/json');

  const res = await fetch(`${API_URL}${path}`, { ...init, headers });
  if (res.status === 401) await tokenStore.clear();
  if (!res.ok) {
    const problem = await res.json().catch(() => ({ status: res.status }));
    throw new ApiError({ ...problem, status: res.status });
  }
  return res.status === 204 ? (undefined as T) : res.json();
}
