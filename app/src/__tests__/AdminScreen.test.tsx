import { act, fireEvent, render, screen } from '@testing-library/react-native';
import { BackHandler } from 'react-native';
import { AdminApi } from '../adminApi';
import { AdminScreen } from '../AdminScreen';
import { Shelter } from '../types';

const monterey: Shelter = { id: 1, name: 'Monterey Shelter', address: '1 Ocean Ave', zip: '93940', phone: '831-555-0100' };
const salinas: Shelter = { id: 2, name: 'Salinas Animal Services', address: '144 Hitchcock Rd', zip: '93901', email: 'hi@salinas.org' };

// A fake admin API. Each test changes only what it needs.
function fakeApi(changes: Partial<AdminApi> = {}): AdminApi {
  return {
    listShelters: jest.fn().mockResolvedValue([monterey, salinas]),
    createShelter: jest.fn(),
    updateShelter: jest.fn(),
    deleteShelter: jest.fn(),
    listStaff: jest.fn().mockResolvedValue([]),
    addStaff: jest.fn(),
    removeStaff: jest.fn(),
    ...changes,
  };
}

test('shows every shelter with its address', async () => {
  await render(<AdminScreen onBack={() => {}} api={fakeApi()} />);

  expect(await screen.findByText('Monterey Shelter')).toBeTruthy();
  expect(screen.getByText('2 shelters')).toBeTruthy();
  expect(screen.getByText('144 Hitchcock Rd · 93901')).toBeTruthy();
});

test('search keeps only matching shelters', async () => {
  await render(<AdminScreen onBack={() => {}} api={fakeApi()} />);
  await screen.findByText('Monterey Shelter');

  await fireEvent.changeText(screen.getByLabelText('Search shelters'), 'salinas');
  expect(screen.queryByText('Monterey Shelter')).toBeNull();
  expect(screen.getByText('Salinas Animal Services')).toBeTruthy();

  await fireEvent.changeText(screen.getByLabelText('Search shelters'), 'carmel');
  expect(screen.getByText('No shelters match "carmel".')).toBeTruthy();
});

test('no shelters says so', async () => {
  await render(<AdminScreen onBack={() => {}} api={fakeApi({ listShelters: jest.fn().mockResolvedValue([]) })} />);
  expect(await screen.findByText('No shelters yet. Add the first one.')).toBeTruthy();
});

test('if loading fails, try again loads the list', async () => {
  const listShelters = jest.fn().mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce([monterey]);
  await render(<AdminScreen onBack={() => {}} api={fakeApi({ listShelters })} />);

  expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't load the shelters. Please try again.");
  await fireEvent.press(screen.getByRole('button', { name: 'Try again' }));
  expect(await screen.findByText('Monterey Shelter')).toBeTruthy();
});

test('creating a shelter saves it and opens it', async () => {
  const carmel: Shelter = { id: 3, name: 'Carmel Pet Rescue', address: '5 Dolores St', zip: '93921', email: 'hi@carmel.org' };
  const api = fakeApi({ createShelter: jest.fn().mockResolvedValue(carmel) });
  await render(<AdminScreen onBack={() => {}} api={api} />);
  await screen.findByText('Monterey Shelter');

  await fireEvent.press(screen.getByRole('button', { name: 'New shelter' }));
  await fireEvent.changeText(screen.getByLabelText('Name'), 'Carmel Pet Rescue');
  await fireEvent.press(screen.getByRole('button', { name: 'Create shelter' }));

  expect(api.createShelter).toHaveBeenCalledWith(expect.objectContaining({ name: 'Carmel Pet Rescue' }));
  expect(await screen.findByRole('heading', { name: 'Carmel Pet Rescue' })).toBeTruthy();
  expect(api.listStaff).toHaveBeenCalledWith(3);
});

test('tapping a shelter opens it, and back returns to the list', async () => {
  const api = fakeApi();
  await render(<AdminScreen onBack={() => {}} api={api} />);

  await fireEvent.press(await screen.findByText('Salinas Animal Services'));
  expect(await screen.findByText('No staff yet. Only staff can post for this shelter.')).toBeTruthy();

  await fireEvent.press(screen.getByRole('button', { name: 'Back to shelters' }));
  expect(await screen.findByText('2 shelters')).toBeTruthy();
  expect(api.listShelters).toHaveBeenCalledTimes(2);
});

test('the back arrow goes back to search', async () => {
  const onBack = jest.fn();
  await render(<AdminScreen onBack={onBack} api={fakeApi()} />);

  await fireEvent.press(screen.getByRole('button', { name: 'Back to search' }));
  expect(onBack).toHaveBeenCalled();
});

test("Android's back button closes an open shelter first, then leaves admin", async () => {
  const addListener = jest.spyOn(BackHandler, 'addEventListener');
  // Presses back by calling the handler the screen gave to BackHandler most recently
  const pressBack = () => (addListener.mock.calls[addListener.mock.calls.length - 1][1] as () => boolean)();
  const onBack = jest.fn();
  await render(<AdminScreen onBack={onBack} api={fakeApi()} />);

  await fireEvent.press(await screen.findByText('Monterey Shelter'));
  expect(await screen.findByRole('heading', { name: 'Monterey Shelter' })).toBeTruthy();

  await act(async () => {
    expect(pressBack()).toBe(true);
  });
  expect(await screen.findByText('2 shelters')).toBeTruthy();
  expect(onBack).not.toHaveBeenCalled();

  await act(async () => {
    pressBack();
  });
  expect(onBack).toHaveBeenCalled();
  addListener.mockRestore();
});
