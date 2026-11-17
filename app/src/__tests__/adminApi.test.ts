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

test('listShelters asks once when everything fits on one page', async () => {
  fakeApi.mockResolvedValue({ page: 0, size: 50, totalElements: 1, totalPages: 1, content: [{ id: 1, ...input }] });

  expect(await listShelters()).toEqual([{ id: 1, ...input }]);
  expect(fakeApi).toHaveBeenCalledTimes(1);
  expect(fakeApi).toHaveBeenCalledWith('/shelters?page=0&size=50');
});

test('listShelters gets every page, so shelters after the first 50 are there too', async () => {
  const first = Array.from({ length: 50 }, (_, i) => ({ id: i + 1, ...input }));
  const last = { id: 51, ...input, name: 'Zebra Rescue' };
  fakeApi
    .mockResolvedValueOnce({ page: 0, size: 50, totalElements: 51, totalPages: 2, content: first })
    .mockResolvedValueOnce({ page: 1, size: 50, totalElements: 51, totalPages: 2, content: [last] });

  const shelters = await listShelters();

  expect(shelters).toHaveLength(51);
  expect(shelters[50]).toEqual(last);
  expect(fakeApi).toHaveBeenNthCalledWith(1, '/shelters?page=0&size=50');
  expect(fakeApi).toHaveBeenNthCalledWith(2, '/shelters?page=1&size=50');
});

test('listShelters stops on an empty page even if totalPages says there are more', async () => {
  fakeApi
    .mockResolvedValueOnce({ page: 0, size: 50, totalElements: 99, totalPages: 9, content: [{ id: 1, ...input }] })
    .mockResolvedValue({ page: 1, size: 50, totalElements: 99, totalPages: 9, content: [] });

  expect(await listShelters()).toEqual([{ id: 1, ...input }]);
  expect(fakeApi).toHaveBeenCalledTimes(2);
});

test('listShelters returns an empty list when there are no shelters', async () => {
  fakeApi.mockResolvedValue({ page: 0, size: 50, totalElements: 0, totalPages: 0, content: [] });

  expect(await listShelters()).toEqual([]);
  expect(fakeApi).toHaveBeenCalledTimes(1);
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
