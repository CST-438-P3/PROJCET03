import { api } from '../api';
import { getFavorites, removeFavorite, saveFavorite } from '../favoritesApi';

// Swap the real api() for a fake one, so these tests never hit a real server
jest.mock('../api', () => ({ api: jest.fn() }));
const fakeApi = api as jest.Mock;

beforeEach(() => {
  fakeApi.mockReset();
});

// A fake page with one saved post, shaped like the API's PostPage
const pageWithLuna = {
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  content: [
    {
      id: 1,
      shelter: { id: 1, name: 'Monterey County Animal Services' },
      name: 'Luna',
      species: 'CAT',
      status: 'AVAILABLE',
      favorite: true,
    },
  ],
};

test('getFavorites asks for the first page by default and returns the whole page', async () => {
  fakeApi.mockResolvedValue(pageWithLuna);

  const favoritesPage = await getFavorites();

  expect(fakeApi).toHaveBeenCalledWith('/favorites?page=0&size=20');
  // The page info is kept, so a screen can see if there are more pages
  expect(favoritesPage).toEqual(pageWithLuna);
});

test('getFavorites can ask for a later page and a different page size', async () => {
  fakeApi.mockResolvedValue({ ...pageWithLuna, page: 2, size: 5 });

  const favoritesPage = await getFavorites(2, 5);

  expect(fakeApi).toHaveBeenCalledWith('/favorites?page=2&size=5');
  expect(favoritesPage.page).toBe(2);
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