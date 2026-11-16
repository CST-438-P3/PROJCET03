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

test('tapping a pet opens it', async () => {
  const onOpenPet = jest.fn();
  const view = await render(<NearbyScreen loadPets={async () => pets} onOpenPet={onOpenPet} />);
  await fireEvent.press(view.getByRole('button', { name: 'Search nearby pets' }));

  await fireEvent.press(await view.findByText('Luna'));

  expect(onOpenPet).toHaveBeenCalledWith(pets[0]);
});

test('the Admin button shows only when onOpenAdmin is given', async () => {
  const onOpenAdmin = jest.fn();
  const view = await render(<NearbyScreen loadPets={async () => []} onOpenAdmin={onOpenAdmin} />);

  await fireEvent.press(view.getByRole('button', { name: 'Admin' }));
  expect(onOpenAdmin).toHaveBeenCalled();

  await view.rerender(<NearbyScreen loadPets={async () => []} />);
  expect(view.queryByRole('button', { name: 'Admin' })).toBeNull();
});
