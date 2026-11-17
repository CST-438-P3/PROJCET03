import { api } from '../api';
import { getPost } from '../postsApi';

// Swap the real api() for a fake one, so these tests never hit a real server
jest.mock('../api', () => ({ api: jest.fn() }));
const fakeApi = api as jest.Mock;

test('getPost asks for that post and returns it', async () => {
  const mochi = { id: 7, shelter: { id: 1, name: 'Monterey Shelter' }, name: 'Mochi', species: 'CAT', status: 'AVAILABLE' };
  fakeApi.mockResolvedValue(mochi);

  const post = await getPost(7);

  expect(fakeApi).toHaveBeenCalledWith('/posts/7');
  expect(post).toEqual(mochi);
});
