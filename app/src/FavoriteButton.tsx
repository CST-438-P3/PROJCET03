import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { color, font } from './theme';
import { removeFavorite, saveFavorite } from './favoritesApi';

// The props for the button
type FavoriteButtonProps = {
  postId: number;
  petName: string;
 // Whether the pet is already saved.
  startsSaved?: boolean;
  // These let the tests pass in fake data version.
  // In the real app we leave them out and the real API functions are used.
  savePost?: (postId: number) => Promise<void>;
  removePost?: (postId: number) => Promise<void>;
};

// A heart button. Tap it to save a pet to favorites, tap it again to remove it (S8).
export function FavoriteButton({
  postId,
  petName,
  startsSaved = false,
  savePost = saveFavorite,
  removePost = removeFavorite,
}: FavoriteButtonProps) {
  const [isSaved, setIsSaved] = useState(startsSaved);
  const [isWaiting, setIsWaiting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // useState only reads startsSaved the first time the button shows up.
  // If the screen reloads its list and the post's favorite value changes,
  // this copies the new value in so the heart stays up to date.
  useEffect(() => {
    setIsSaved(startsSaved);
  }, [startsSaved]);

  async function handlePress() {
    setIsWaiting(true);
    setErrorMessage(null);

    try {
      if (isSaved) {
        await removePost(postId);
        setIsSaved(false);
      } else {
        await savePost(postId);
        setIsSaved(true);
      }
    } catch {
      // The heart stays how it was, so it always matches what the server has
      setErrorMessage("Couldn't update favorites. Please try again.");
    } finally {
      setIsWaiting(false);
    }
  }

  // Screen readers read this out, so it says what tapping will do
  const buttonLabel = isSaved
    ? `Remove ${petName} from favorites`
    : `Save ${petName} to favorites`;

  // A filled heart when saved, an empty heart when not
  const heartSymbol = isSaved ? '♥' : '♡';

  return (
    <View style={styles.wrapper}>
      <Pressable
        role="button"
        accessibilityLabel={buttonLabel}
        accessibilityState={{ selected: isSaved, disabled: isWaiting }}
        disabled={isWaiting}
        onPress={() => void handlePress()}
        style={[styles.button, isSaved && styles.buttonSaved]}
      >
        <Text style={[styles.heart, isSaved && styles.heartSaved]}>{heartSymbol}</Text>
      </Pressable>

      {errorMessage && (
        <Text role="alert" style={styles.error}>
          {errorMessage}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { alignItems: 'flex-end', gap: 6 },
  // 48 x 48 so it's easy to tap on a phone
  button: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: color.ink,
    backgroundColor: color.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonSaved: { backgroundColor: color.ink },
  heart: { color: color.ink, fontSize: 24, lineHeight: 28 },
  heartSaved: { color: color.surface },
  error: { color: color.ink, fontFamily: font.semiBold, fontSize: 14 },
});