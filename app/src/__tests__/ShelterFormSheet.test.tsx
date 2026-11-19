import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { ApiError } from '../api';
import { fieldErrors, ShelterFormSheet } from '../ShelterFormSheet';

test('fieldErrors puts each API message under its own field', () => {
  expect(fieldErrors("Name is required; ZIP code must be 5 digits; Phone or email is required; Email must look like name@example.com"))
    .toEqual({
      name: 'Name is required',
      zip: 'ZIP code must be 5 digits',
      phone: 'Phone or email is required',
      email: 'Email must look like name@example.com',
    });
  expect(fieldErrors("We don't know ZIP code 10001")).toEqual({ zip: "We don't know ZIP code 10001" });
  expect(fieldErrors('Something else')).toEqual({ form: 'Something else' });
});

test('typing and saving sends what was typed', async () => {
  const onSubmit = jest.fn().mockResolvedValue(undefined);
  await render(<ShelterFormSheet title="New shelter" submitLabel="Create shelter" onSubmit={onSubmit} onClose={() => {}} />);

  await fireEvent.changeText(screen.getByLabelText('Name'), 'Carmel Pet Rescue');
  await fireEvent.changeText(screen.getByLabelText('Address'), '5 Dolores St');
  await fireEvent.changeText(screen.getByLabelText('ZIP code'), '93921');
  await fireEvent.changeText(screen.getByLabelText('Email'), 'hi@carmel.org');
  await fireEvent.press(screen.getByRole('button', { name: 'Create shelter' }));

  expect(onSubmit).toHaveBeenCalledWith({
    name: 'Carmel Pet Rescue', address: '5 Dolores St', zip: '93921', phone: '', email: 'hi@carmel.org',
  });
});

test('a 400 from the API shows each message under its field', async () => {
  const onSubmit = jest.fn().mockRejectedValue(
    new ApiError({ status: 400, detail: 'ZIP code must be 5 digits; Phone or email is required' }));
  await render(<ShelterFormSheet title="New shelter" submitLabel="Create shelter" onSubmit={onSubmit} onClose={() => {}} />);

  await fireEvent.press(screen.getByRole('button', { name: 'Create shelter' }));

  expect(await screen.findByText('ZIP code must be 5 digits')).toBeTruthy();
  expect(screen.getByText('Phone or email is required')).toBeTruthy();
  expect(screen.getByRole('button', { name: 'Create shelter' })).toBeEnabled();
});

test('any other failure shows a general error', async () => {
  const onSubmit = jest.fn().mockRejectedValue(new Error('network'));
  await render(<ShelterFormSheet title="New shelter" submitLabel="Create shelter" onSubmit={onSubmit} onClose={() => {}} />);

  await fireEvent.press(screen.getByRole('button', { name: 'Create shelter' }));

  expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't save the shelter. Please try again.");
});

test('editing starts with the shelter filled in, and close closes', async () => {
  const onClose = jest.fn();
  await render(
    <ShelterFormSheet
      title="Edit shelter"
      submitLabel="Save changes"
      initial={{ name: 'Monterey Shelter', address: '1 Ocean Ave', zip: '93940', phone: '831-555-0100', email: '' }}
      onSubmit={jest.fn()}
      onClose={onClose}
    />);

  expect(screen.getByLabelText('Name').props.value).toBe('Monterey Shelter');
  expect(screen.getByLabelText('Phone').props.value).toBe('831-555-0100');
  await fireEvent.press(screen.getByRole('button', { name: 'Close' }));
  await waitFor(() => expect(onClose).toHaveBeenCalled());
});
