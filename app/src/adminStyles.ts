import { StyleSheet } from 'react-native';
import { color, font } from './theme';

/** Styles the admin screens share, so the cards and text boxes look the same everywhere. */
export const adminStyles = StyleSheet.create({
  // A white card that floats a little above the page.
  card: {
    backgroundColor: color.surface,
    borderRadius: 20,
    shadowColor: color.ink,
    shadowOpacity: 0.06,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    elevation: 2,
  },
  // A soft grey text box with no hard border.
  input: {
    backgroundColor: color.background,
    borderWidth: 1.5,
    borderColor: 'transparent',
    borderRadius: 14,
    paddingVertical: 12,
    paddingHorizontal: 14,
    fontSize: 16,
    fontFamily: font.regular,
    color: color.ink,
  },
  inputFocused: { backgroundColor: color.surface, borderColor: color.brand },
  inputError: { backgroundColor: color.dangerSurface, borderColor: color.dangerLine },
  errorText: { color: color.danger, fontFamily: font.semiBold, fontSize: 13 },
});

