import { api, ApiError } from '../api';
import { tokenStore } from '../tokenStore';

jest.mock('../tokenStore', () => {
  let t: string | null = null;
  return {
    tokenStore: {
      get: jest.fn(async () => t),
      set: jest.fn(async (v: string) => { t = v; }),
      clear: jest.fn(async () => { t = null; }),
    },
  };
});

const mockFetch = (status: number, body?: unknown) =>
  (globalThis.fetch = jest.fn().mockResolvedValue({
    ok: status < 400,
    status,
    json: async () => body,
  }) as jest.Mock);

beforeEach(async () => { await tokenStore.clear(); });

test('attaches the bearer token', async () => {
  await tokenStore.set('abc');
  const fetch = mockFetch(200, { id: 1 });
  await expect(api('/users/me')).resolves.toEqual({ id: 1 });
  expect(fetch.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer abc');
});

test('returns undefined on 204', async () => {
  mockFetch(204);
  await expect(api('/users/me', { method: 'DELETE' })).resolves.toBeUndefined();
});

test('throws ApiError with the problem detail', async () => {
  mockFetch(409, { title: 'Conflict', detail: 'Registered with GitHub' });
  await expect(api('/auth/google', { method: 'POST', body: '{}' })).rejects.toThrow(
    new ApiError({ status: 409, detail: 'Registered with GitHub' }),
  );
});

test('clears the token on 401', async () => {
  await tokenStore.set('expired');
  mockFetch(401, { title: 'Unauthorized' });
  await expect(api('/users/me')).rejects.toBeInstanceOf(ApiError);
  expect(await tokenStore.get()).toBeNull();
});
