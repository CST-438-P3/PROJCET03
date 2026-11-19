import { color } from './theme';

/** Soft background colors for the letter avatars, picked by id so a shelter or person keeps the same one. */
const TINTS = [color.surfaceActive, '#FFE9D6', '#E4F5E1'];

/** Up to two capital letters for an avatar: "Monterey Shelter" -> "MS", "zoe@example.com" -> "ZO". */
export function initials(text: string): string {
  const words = text.split('@')[0].split(/[\s._-]+/).filter(Boolean);
  const letters = words.length > 1 ? words[0][0] + words[1][0] : (words[0] ?? '?').slice(0, 2);
  return letters.toUpperCase();
}

/** The avatar background for this id. */
export function tintFor(id: number): string {
  return TINTS[id % TINTS.length];
}

/** "1 Ocean Ave · 93940". */
export function addressLine(address: string, zip: string): string {
  return `${address} · ${zip}`;
}
