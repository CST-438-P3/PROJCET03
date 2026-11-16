import { fireEvent, render, screen } from '@testing-library/react-native';
import { BackHandler } from 'react-native';
import { ApiError } from '../api';
import { formatAge, PostDetailScreen } from '../PostDetailScreen';
import { Post } from '../types';

// A full post, like GET /posts/{postId} returns
const mochi: Post = {
  id: 7,
  shelter: { id: 1, name: 'Monterey Shelter' },
  name: 'Mochi',
  species: 'CAT',
  breed: 'Ragdoll',
  sex: 'FEMALE',
  size: 'SMALL',
  ageMonths: 8,
  description: 'Calm and loves laps.',
  photoUrl: 'https://example.com/mochi.jpg',
  status: 'AVAILABLE',
};

test('shows the pet, its facts and its shelter', async () => {
  const loadPost = jest.fn().mockResolvedValue(mochi);
  await render(<PostDetailScreen postId={7} distanceMiles={2} onBack={() => {}} loadPost={loadPost} />);

  expect(await screen.findByRole('heading', { name: 'Mochi' })).toBeTruthy();
  expect(loadPost).toHaveBeenCalledWith(7);
  expect(screen.getByText('Available')).toBeTruthy();
  expect(screen.getByText('Ragdoll · Female')).toBeTruthy();
  expect(screen.getByText('Cat')).toBeTruthy();
  expect(screen.getByText('8 months')).toBeTruthy();
  expect(screen.getByText('Small')).toBeTruthy();
  expect(screen.getByText('Monterey Shelter')).toBeTruthy();
  expect(screen.getByText('2 mi away')).toBeTruthy();
  expect(screen.getByText('Calm and loves laps.')).toBeTruthy();
  expect(screen.getByLabelText('Photo of Mochi')).toBeTruthy();
  expect(screen.getByRole('button', { name: 'Save Mochi to favorites' })).toBeTruthy();
});

test('a post with only the required fields still shows', async () => {
  const bare: Post = { id: 8, shelter: { id: 1, name: 'Monterey Shelter' }, name: 'Max', species: 'DOG', sex: 'UNKNOWN', status: 'AVAILABLE' };
  await render(<PostDetailScreen postId={8} onBack={() => {}} loadPost={async () => bare} />);

  expect(await screen.findByRole('heading', { name: 'Max' })).toBeTruthy();
  expect(screen.getByText('No photo yet')).toBeTruthy();
  expect(screen.getAllByText('Unknown')).toHaveLength(2);
  expect(screen.queryByText(/mi away/)).toBeNull();
  expect(screen.queryByText('About Max')).toBeNull();
});

test('an adopted pet says it found a home', async () => {
  await render(<PostDetailScreen postId={7} onBack={() => {}} loadPost={async () => ({ ...mochi, status: 'ADOPTED' })} />);

  expect(await screen.findByText('Adopted')).toBeTruthy();
  expect(screen.getByText('Mochi found a home')).toBeTruthy();
  expect(screen.queryByText('Available')).toBeNull();
});

test('a removed post says it is no longer available, and back works', async () => {
  const onBack = jest.fn();
  const notFound = new ApiError({ status: 404, detail: 'This post is no longer available' });
  await render(<PostDetailScreen postId={99} onBack={onBack} loadPost={async () => { throw notFound; }} />);

  expect(await screen.findByRole('heading', { name: 'This post is no longer available' })).toBeTruthy();
  await fireEvent.press(screen.getByRole('button', { name: 'Back to search' }));
  expect(onBack).toHaveBeenCalled();
});

test('if loading fails, try again loads the pet', async () => {
  const loadPost = jest.fn().mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(mochi);
  await render(<PostDetailScreen postId={7} onBack={() => {}} loadPost={loadPost} />);

  expect(await screen.findByRole('alert')).toHaveTextContent("We couldn't load this pet. Please try again.");
  await fireEvent.press(screen.getByRole('button', { name: 'Try again' }));

  expect(await screen.findByRole('heading', { name: 'Mochi' })).toBeTruthy();
  expect(loadPost).toHaveBeenCalledTimes(2);
});

test('the back arrow goes back', async () => {
  const onBack = jest.fn();
  await render(<PostDetailScreen postId={7} onBack={onBack} loadPost={async () => mochi} />);

  await fireEvent.press(await screen.findByRole('button', { name: 'Back to search' }));
  expect(onBack).toHaveBeenCalled();
});

test('formatAge reads like a person would say it', () => {
  expect(formatAge(0)).toBe('Under 1 month');
  expect(formatAge(1)).toBe('1 month');
  expect(formatAge(12)).toBe('1 year');
  expect(formatAge(15)).toBe('1 year 3 months');
  expect(formatAge(26)).toBe('2 years 2 months');
});

test('the distance from the API wins over the one from the list', async () => {
  await render(<PostDetailScreen postId={7} distanceMiles={2} onBack={() => {}} loadPost={async () => ({ ...mochi, distanceMiles: 5 })} />);

  expect(await screen.findByText('5 mi away')).toBeTruthy();
  expect(screen.queryByText('2 mi away')).toBeNull();
});

test('a photo that fails to load shows the no photo box', async () => {
  await render(<PostDetailScreen postId={7} onBack={() => {}} loadPost={async () => mochi} />);

  fireEvent(await screen.findByLabelText('Photo of Mochi'), 'error');

  expect(await screen.findByText('No photo yet')).toBeTruthy();
});

test("Android's back button goes back to search", async () => {
  const addListener = jest.spyOn(BackHandler, 'addEventListener');
  const onBack = jest.fn();
  await render(<PostDetailScreen postId={7} onBack={onBack} loadPost={async () => mochi} />);

  // Press back by calling what the screen gave to BackHandler
  const pressBack = addListener.mock.calls[0][1] as () => boolean;
  expect(pressBack()).toBe(true);
  expect(onBack).toHaveBeenCalled();
  addListener.mockRestore();
});
