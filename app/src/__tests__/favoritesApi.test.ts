import { api } from '../api';
import { getFavorites, removeFavorite, saveFavorite } from '../favoritesApi';

// Swap the real api() for a fake one, so these tests never hit a real server
jest.mock('../api', () => ({ api: jest.fn() }));
const fakeApi = api as jest.Mock;

beforeEach(() => {
  fakeApi.mockReset();
});

test('getFavorites calls GET /favorites and returns the posts inside the page', async () => {
  const savedPost = {
    id: 1,
    shelter: { id: 1, name: 'Monterey County Animal Services' },
    name: 'Luna',
    species: 'CAT',
    status: 'AVAILABLE',
  };
  fakeApi.mockResolvedValue({ content: [savedPost] });

  const favorites = await getFavorites();

  expect(fakeApi).toHaveBeenCalledWith('/favorites');
  expect(favorites).toEqual([savedPost]);
});

test('saveFavorite sends a PUT for that post', async () => {
  fakeApi.mockResolvedValue(undefined);

  await saveFavorite(7);

  expect(fakeApi).toHaveBeenCalledWith('/favorites/7', { method: 'PUT' });
});

test('removeFavorite sends a DELETE for that post', async () => {
  fakeApi.mockResolvedValue(undefined);

  await removeFavorite(7);

  expect(fakeApi).toHaveBeenCalledWith('/favorites/7', { method: 'DELETE' });
});