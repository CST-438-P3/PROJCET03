import { act, fireEvent, render, screen } from '@testing-library/react-native';
import { ApiError } from '../api';
import { AdminApi } from '../adminApi';
import { AdminShelterScreen } from '../AdminShelterScreen';
import { Shelter, StaffMember } from '../types';

const monterey: Shelter = { id: 1, name: 'Monterey Shelter', address: '1 Ocean Ave', zip: '93940', phone: '831-555-0100' };
const jacqueline: StaffMember = { id: 12, email: 'jacqueline@csumb.edu', displayName: 'Jacqueline Lim' };
const dana: StaffMember = { id: 15, email: 'dana@monterey.org' };

// A fake admin API. Each test changes only what it needs.
function fakeApi(changes: Partial<AdminApi> = {}): AdminApi {
  return {
    listShelters: jest.fn(),
    createShelter: jest.fn(),
    updateShelter: jest.fn(),
    deleteShelter: jest.fn().mockResolvedValue(undefined),
    listStaff: jest.fn().mockResolvedValue([jacqueline]),
    addStaff: jest.fn().mockResolvedValue(undefined),
    removeStaff: jest.fn().mockResolvedValue(undefined),
    ...changes,
  };
}

afterEach(() => {
  jest.useRealTimers();
});

test('shows the shelter and its staff', async () => {
  await render(<AdminShelterScreen shelter={monterey} onBack={() => {}} api={fakeApi()} />);

  expect(screen.getByRole('heading', { name: 'Monterey Shelter' })).toBeTruthy();
  expect(screen.getByText('1 Ocean Ave · 93940')).toBeTruthy();
  expect(screen.getByText('☎ 831-555-0100')).toBeTruthy();
  expect(await screen.findByText('jacqueline@csumb.edu')).toBeTruthy();
  expect(screen.getByText('JL')).toBeTruthy();
  expect(screen.getByText('User 12')).toBeTruthy();
});

test('adding staff by user ID shows a toast and reloads the staff, and the toast goes away', async () => {
  jest.useFakeTimers();
  const listStaff = jest.fn().mockResolvedValueOnce([jacqueline]).mockResolvedValueOnce([dana, jacqueline]);
  const api = fakeApi({ listStaff });
  await render(<AdminShelterScreen shelter={monterey} onBack={() => {}} api={api} />);
  await screen.findByText('jacqueline@csumb.edu');

  await fireEvent.changeText(screen.getByLabelText('User ID'), ' 15 ');
  await fireEvent.press(screen.getByRole('button', { name: 'Add staff' }));

  expect(api.addStaff).toHaveBeenCalledWith(1, 15);
  expect(await screen.findByText('User 15 can now post for Monterey Shelter')).toBeTruthy();
  expect(await screen.findByText('dana@monterey.org')).toBeTruthy();
  expect(screen.getByLabelText('User ID').props.value).toBe('');

  await act(async () => {
    jest.advanceTimersByTime(3000);
  });
  expect(screen.queryByText('User 15 can now post for Monterey Shelter')).toBeNull();
});

test('a user ID that is not a number is not sent', async () => {
  const api = fakeApi();
  await render(<AdminShelterScreen shelter={monterey} onBack={() => {}} api={api} />);

  await fireEvent.changeText(screen.getByLabelText('User ID'), 'dana');
  await fireEvent.press(screen.getByRole('button', { name: 'Add staff' }));

  expect(screen.getByText('Enter a user ID, numbers only')).toBeTruthy();
  expect(api.addStaff).not.toHaveBeenCalled();
});

test('an unknown user shows the API message', async () => {
  const api = fakeApi({ addStaff: jest.fn().mockRejectedValue(new ApiError({ status: 404, detail: 'User not found' })) });
  await render(<AdminShelterScreen shelter={monterey} onBack={() => {}} api={api} />);

  await fireEvent.changeText(screen.getByLabelText('User ID'), '999');
  await fireEvent.press(screen.getByRole('button', { name: 'Add staff' }));

  expect(await screen.findByText('User not found')).toBeTruthy();
});

test('removing staff calls the API and reloads', async () => {
  const listStaff = jest.fn().mockResolvedValueOnce([jacqueline]).mockResolvedValueOnce([]);
  const api = fakeApi({ listStaff });
  await render(<AdminShelterScreen shelter={monterey} onBack={() => {}} api={api} />);

  await fireEvent.press(await screen.findByRole('button', { name: 'Remove jacqueline@csumb.edu' }));

  expect(api.removeStaff).toHaveBeenCalledWith(1, 12);
  expect(await screen.findByText('jacqueline@csumb.edu can no longer post for Monterey Shelter')).toBeTruthy();
  expect(await screen.findByText('No staff yet. Only staff can post for this shelter.')).toBeTruthy();
});

test('if the staff list fails, try again loads it', async () => {
  const listStaff = jest.fn().mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce([jacqueline]);
  await render(<AdminShelterScreen shelter={monterey} onBack={() => {}} api={fakeApi({ listStaff })} />);

  expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't load the staff. Please try again.");
  await fireEvent.press(screen.getByRole('button', { name: 'Try again' }));
  expect(await screen.findByText('jacqueline@csumb.edu')).toBeTruthy();
});

test('delete asks first, cancel keeps the shelter, delete goes back', async () => {
  const onBack = jest.fn();
  const api = fakeApi();
  await render(<AdminShelterScreen shelter={monterey} onBack={onBack} api={api} />);

  await fireEvent.press(screen.getByRole('button', { name: 'Delete shelter' }));
  expect(screen.getByText('Delete Monterey Shelter? Its posts are deleted too.')).toBeTruthy();
  await fireEvent.press(screen.getByRole('button', { name: 'Cancel' }));
  expect(api.deleteShelter).not.toHaveBeenCalled();

  await fireEvent.press(screen.getByRole('button', { name: 'Delete shelter' }));
  await fireEvent.press(screen.getByRole('button', { name: 'Yes, delete' }));
  expect(api.deleteShelter).toHaveBeenCalledWith(1);
  expect(onBack).toHaveBeenCalled();
});

test('editing saves and shows the new details', async () => {
  const updated = { ...monterey, name: 'Monterey Rescue', email: 'hi@monterey.org' };
  const api = fakeApi({ updateShelter: jest.fn().mockResolvedValue(updated) });
  await render(<AdminShelterScreen shelter={monterey} onBack={() => {}} api={api} />);

  await fireEvent.press(screen.getByRole('button', { name: 'Edit shelter' }));
  await fireEvent.changeText(screen.getByLabelText('Name'), 'Monterey Rescue');
  await fireEvent.press(screen.getByRole('button', { name: 'Save changes' }));

  expect(api.updateShelter).toHaveBeenCalledWith(1, {
    name: 'Monterey Rescue', address: '1 Ocean Ave', zip: '93940', phone: '831-555-0100', email: '',
  });
  expect(await screen.findByRole('heading', { name: 'Monterey Rescue' })).toBeTruthy();
  expect(screen.getByText('✉ hi@monterey.org')).toBeTruthy();
});
