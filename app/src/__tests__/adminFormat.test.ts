import { addressLine, initials, tintFor } from '../adminFormat';

test('initials take the first letters of two words, or two letters of one', () => {
  expect(initials('Monterey Shelter')).toBe('MS');
  expect(initials('jacqueline.lim@csumb.edu')).toBe('JL');
  expect(initials('zoe@example.com')).toBe('ZO');
  expect(initials('')).toBe('?');
});

test('the same id always gets the same tint', () => {
  expect(tintFor(4)).toBe(tintFor(7));
  expect(tintFor(4)).not.toBe(tintFor(5));
});

test('addressLine puts the ZIP after the address', () => {
  expect(addressLine('1 Ocean Ave', '93940')).toBe('1 Ocean Ave · 93940');
});
