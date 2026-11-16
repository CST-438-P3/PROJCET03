import { api } from '../api';
import { addStaff, createShelter, deleteShelter, listShelters, listStaff, removeStaff, updateShelter } from '../adminApi';

// Swap the real api() for a fake one, so these tests never hit a real server
jest.mock('../api', () => ({ api: jest.fn() }));
const fakeApi = api as jest.Mock;

const input = { name: 'Monterey Shelter', address: '1 Ocean Ave', zip: '93940', phone: '', email: 'hi@monterey.org' };

beforeEach(() => {
  fakeApi.mockReset();
  fakeApi.mockResolvedValue(undefined);
});

test('listShelters asks for the first 50 and returns the list', async () => {
  fakeApi.mockResolvedValue({ page: 0, size: 50, totalElements: 1, totalPages: 1, content: [{ id: 1, ...input }] });

  expect(await listShelters()).toEqual([{ id: 1, ...input }]);
  expect(fakeApi).toHaveBeenCalledWith('/shelters?page=0&size=50');
});

test('create, update and delete use the admin shelter paths', async () => {
  await createShelter(input);
  await updateShelter(3, input);
  await deleteShelter(3);

  expect(fakeApi).toHaveBeenCalledWith('/admin/shelters', { method: 'POST', body: JSON.stringify(input) });
  expect(fakeApi).toHaveBeenCalledWith('/admin/shelters/3', { method: 'PUT', body: JSON.stringify(input) });
  expect(fakeApi).toHaveBeenCalledWith('/admin/shelters/3', { method: 'DELETE' });
});

test('staff calls use the shelter and user ids', async () => {
  await listStaff(3);
  await addStaff(3, 15);
  await removeStaff(3, 15);

  expect(fakeApi).toHaveBeenCalledWith('/admin/shelters/3/staff');
  expect(fakeApi).toHaveBeenCalledWith('/admin/shelters/3/staff/15', { method: 'PUT' });
  expect(fakeApi).toHaveBeenCalledWith('/admin/shelters/3/staff/15', { method: 'DELETE' });
});
