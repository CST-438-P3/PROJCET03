import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { NearbyScreen } from '../NearbyScreen';

const pets = [
  { id: 1, shelter: { id: 1, name: 'Near Shelter' }, name: 'Luna', species: 'CAT' as const, status: 'AVAILABLE' as const, distanceMiles: 2 },
  { id: 2, shelter: { id: 2, name: 'Far Shelter' }, name: 'Bella', species: 'DOG' as const, status: 'AVAILABLE' as const, distanceMiles: 14 },
];

test('searches by ZIP and shows nearest results', async () => {
  const loadPets = jest.fn(async (_zip: string, distance: number) => pets.filter((pet) => pet.distanceMiles <= distance));
  const view = await render(<NearbyScreen loadPets={loadPets} />);

  await fireEvent.press(view.getByRole('button', { name: 'Search nearby pets' }));

  await waitFor(() => expect(view.getByText('Luna')).toBeTruthy());
  expect(view.queryByText('Bella')).toBeNull();
  expect(loadPets).toHaveBeenCalledWith('93933', 10);
  expect(view.getByText('2 mi')).toBeTruthy();
});

test('shows the empty state when no pets are in range', async () => {
  const view = await render(<NearbyScreen loadPets={async () => []} />);
  await fireEvent.press(view.getByRole('button', { name: 'Search nearby pets' }));
  expect(await view.findByText('No pets within 10 miles.')).toBeTruthy();
});

test('shows an error when the search fails', async () => {
  const view = await render(<NearbyScreen loadPets={async () => { throw new Error('network'); }} />);
  await fireEvent.press(view.getByRole('button', { name: 'Search nearby pets' }));
  expect(await view.findByRole('alert')).toHaveTextContent("We couldn't load nearby pets. Please try again.");
});

test('shows ten-result pages and loads the next page', async () => {
  const loadPets = jest.fn(async (_zip: string, _distance: number, _species?: undefined, page = 0) => ({
    page,
    size: 10,
    totalElements: 11,
    totalPages: 2,
    content: page === 0 ? [pets[0]] : [pets[1]],
  }));
  const view = await render(<NearbyScreen loadPets={loadPets} />);

  await fireEvent.press(view.getByRole('button', { name: 'Search nearby pets' }));
  expect(await view.findByText('Page 1 of 2 · 11 pets')).toBeTruthy();

  await fireEvent.press(view.getByLabelText('Next page'));
  await waitFor(() => expect(view.getByText('Page 2 of 2 · 11 pets')).toBeTruthy());
  expect(loadPets).toHaveBeenLastCalledWith('93933', 10, undefined, 1, 10);
});
