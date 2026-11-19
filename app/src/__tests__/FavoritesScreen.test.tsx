import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { FavoritesScreen } from '../FavoritesScreen';
import { Page, Post } from '../types';

// Two saved pets, like GET /favorites returns them
const luna: Post = {
  id: 1,
  shelter: { id: 1, name: 'Monterey Shelter' },
  name: 'Luna',
  species: 'CAT',
  status: 'AVAILABLE',
  favorite: true,
};
const max: Post = {
  id: 2,
  shelter: { id: 1, name: 'Monterey Shelter' },
  name: 'Max',
  species: 'DOG',
  status: 'ADOPTED',
  favorite: true,
};

// Builds a fake page of results
function pageOf(content: Post[], pageNumber: number, totalPages: number): Page<Post> {
  return { page: pageNumber, size: 20, totalElements: content.length, totalPages, content };
}

test('shows every saved pet with its species and shelter', async () => {
  const loadFavorites = jest.fn().mockResolvedValue(pageOf([luna, max], 0, 1));
  await render(<FavoritesScreen onBack={() => {}} onOpenPet={() => {}} loadFavorites={loadFavorites} />);

  expect(await screen.findByText('Luna')).toBeTruthy();
  expect(loadFavorites).toHaveBeenCalledWith(0, 20);
  expect(screen.getByText('Cat · Monterey Shelter')).toBeTruthy();
  expect(screen.getByText('Max')).toBeTruthy();
  // Max was adopted, so his card says so
  expect(screen.getByText('Adopted')).toBeTruthy();
  // Only one page, so there's nothing more to load
  expect(screen.queryByRole('button', { name: 'Load more' })).toBeNull();
});

test('no favorites shows a hint about the heart', async () => {
  await render(
    <FavoritesScreen onBack={() => {}} onOpenPet={() => {}} loadFavorites={async () => pageOf([], 0, 0)} />,
  );

  expect(await screen.findByText('No favorites yet. Tap the heart on a pet to save it here.')).toBeTruthy();
});

test('tapping a pet opens it', async () => {
  const onOpenPet = jest.fn();
  await render(
    <FavoritesScreen onBack={() => {}} onOpenPet={onOpenPet} loadFavorites={async () => pageOf([luna], 0, 1)} />,
  );

  await fireEvent.press(await screen.findByRole('button', { name: 'Open Luna' }));

  expect(onOpenPet).toHaveBeenCalledWith(luna);
});

test('load more adds the next page under the first one', async () => {
  const loadFavorites = jest.fn()
    .mockResolvedValueOnce(pageOf([luna], 0, 2))
    .mockResolvedValueOnce(pageOf([max], 1, 2));
  await render(<FavoritesScreen onBack={() => {}} onOpenPet={() => {}} loadFavorites={loadFavorites} />);

  await fireEvent.press(await screen.findByRole('button', { name: 'Load more' }));

  expect(await screen.findByText('Max')).toBeTruthy();
  expect(loadFavorites).toHaveBeenLastCalledWith(1, 20);
  // Luna from the first page is still there
  expect(screen.getByText('Luna')).toBeTruthy();
  // That was the last page, so the button goes away
  await waitFor(() => expect(screen.queryByRole('button', { name: 'Load more' })).toBeNull());
});

test('if loading fails, it shows an error and try again works', async () => {
  const loadFavorites = jest.fn()
    .mockRejectedValueOnce(new Error('network'))
    .mockResolvedValueOnce(pageOf([luna], 0, 1));
  await render(<FavoritesScreen onBack={() => {}} onOpenPet={() => {}} loadFavorites={loadFavorites} />);

  expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't load your favorites. Please try again.");

  await fireEvent.press(screen.getByRole('button', { name: 'Try again' }));

  expect(await screen.findByText('Luna')).toBeTruthy();
  expect(loadFavorites).toHaveBeenLastCalledWith(0, 20);
  expect(screen.queryByRole('alert')).toBeNull();
});

test('the back button goes back', async () => {
  const onBack = jest.fn();
  await render(
    <FavoritesScreen onBack={onBack} onOpenPet={() => {}} loadFavorites={async () => pageOf([], 0, 0)} />,
  );

  await fireEvent.press(screen.getByRole('button', { name: 'Back to search' }));

  expect(onBack).toHaveBeenCalled();
});