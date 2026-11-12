import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { FavoriteButton } from '../FavoriteButton';

// Finds the heart button by what a screen reader would say
const heartButton = (label: string) => screen.getByRole('button', { name: label });

test('tapping the heart saves the pet, and tapping again removes it', async () => {
  const fakeSave = jest.fn().mockResolvedValue(undefined);
  const fakeRemove = jest.fn().mockResolvedValue(undefined);
  await render(<FavoriteButton postId={1} petName="Luna" savePost={fakeSave} removePost={fakeRemove} />);

  // First tap: save
  await fireEvent.press(heartButton('Save Luna to favorites'));
  expect(fakeSave).toHaveBeenCalledWith(1);
  await waitFor(() => expect(heartButton('Remove Luna from favorites')).toBeTruthy());

  // Second tap: remove
  await fireEvent.press(heartButton('Remove Luna from favorites'));
  expect(fakeRemove).toHaveBeenCalledWith(1);
  await waitFor(() => expect(heartButton('Save Luna to favorites')).toBeTruthy());
});

test('a pet that starts saved shows the filled heart', async () => {
  await render(<FavoriteButton postId={2} petName="Max" startsSaved />);

  expect(heartButton('Remove Max from favorites')).toBeTruthy();
  expect(screen.getByText('♥')).toBeTruthy();
});

test('the button is disabled while waiting for the server', async () => {
  // A save that doesn't finish until we call finishSaving()
  let finishSaving = () => {};
  const slowSave = jest.fn(() => new Promise<void>((resolve) => { finishSaving = resolve; }));
  await render(<FavoriteButton postId={3} petName="Bella" savePost={slowSave} />);

  await fireEvent.press(heartButton('Save Bella to favorites'));
  expect(heartButton('Save Bella to favorites')).toBeDisabled();

  finishSaving();
  await waitFor(() => expect(heartButton('Remove Bella from favorites')).toBeEnabled());
});

test('if saving fails, it shows an error and the heart stays empty', async () => {
  const failingSave = jest.fn().mockRejectedValue(new Error('network'));
  await render(<FavoriteButton postId={4} petName="Luna" savePost={failingSave} />);

  await fireEvent.press(heartButton('Save Luna to favorites'));

  expect(await screen.findByRole('alert')).toHaveTextContent("Couldn't update favorites. Please try again.");
  expect(heartButton('Save Luna to favorites')).toBeEnabled();
});