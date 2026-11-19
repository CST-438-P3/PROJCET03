import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, BackHandler, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { color, font } from './theme';
import { getFavorites } from './favoritesApi';
import { Page, Post } from './types';

// How many favorites to ask for at a time
const PAGE_SIZE = 20;

const LOAD_ERROR = "We couldn't load your favorites. Please try again.";

type FavoritesScreenProps = {
  onBack: () => void;
  // Opens a pet's detail page (S6)
  onOpenPet: (post: Post) => void;
  // Tests pass a fake here. The real app uses the API.
  loadFavorites?: (pageNumber: number, pageSize: number) => Promise<Page<Post>>;
};

/** The "My favorites" list: every pet the user saved with the heart (S8). */
export function FavoritesScreen({ onBack, onOpenPet, loadFavorites = getFavorites }: FavoritesScreenProps) {
  const [savedPets, setSavedPets] = useState<Post[]>([]);
  // The page we'll ask for next, and how many pages there are in total
  const [nextPage, setNextPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [hasLoadedOnce, setHasLoadedOnce] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Loads one page and adds it under what's already shown.
  // pageNumber 0 means start over, so the list is replaced instead of added to.
  const loadPage = useCallback(async (pageNumber: number) => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const favoritesPage = await loadFavorites(pageNumber, PAGE_SIZE);
      setSavedPets((shownSoFar) =>
        pageNumber === 0 ? favoritesPage.content : [...shownSoFar, ...favoritesPage.content]);
      setNextPage(pageNumber + 1);
      setTotalPages(favoritesPage.totalPages);
      setHasLoadedOnce(true);
    } catch {
      setErrorMessage(LOAD_ERROR);
    } finally {
      setIsLoading(false);
    }
  }, [loadFavorites]);

  // Load the first page when the screen opens
  useEffect(() => {
    void loadPage(0);
  }, [loadPage]);

  // Android's back button goes back to search instead of closing the app
  useEffect(() => {
    const listener = BackHandler.addEventListener('hardwareBackPress', () => {
      onBack();
      return true;
    });
    return () => listener.remove();
  }, [onBack]);

  const hasMorePages = nextPage < totalPages;
  const hasNoFavorites = hasLoadedOnce && savedPets.length === 0;

  return (
    <View role="main" style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <Pressable role="button" onPress={onBack} style={styles.backButton}>
          <Text style={styles.backButtonText}>Back to search</Text>
        </Pressable>

        <Text role="heading" aria-level={1} style={styles.title}>My favorites</Text>

        {errorMessage && (
          <View style={styles.errorBox}>
            <Text role="alert" style={styles.errorText}>{errorMessage}</Text>
            <Pressable role="button" onPress={() => void loadPage(hasLoadedOnce ? nextPage : 0)} style={styles.darkButton}>
              <Text style={styles.darkButtonText}>Try again</Text>
            </Pressable>
          </View>
        )}

        {hasNoFavorites && (
          <Text style={styles.empty}>No favorites yet. Tap the heart on a pet to save it here.</Text>
        )}

        <View accessibilityLabel="Favorite pets" style={styles.list}>
          {savedPets.map((pet) => (
            <Pressable
              key={pet.id}
              role="button"
              accessibilityLabel={`Open ${pet.name}`}
              onPress={() => onOpenPet(pet)}
              style={styles.card}
            >
              <View style={styles.cardTop}>
                <Text style={styles.petName}>{pet.name}</Text>
                {pet.status === 'ADOPTED' && <Text style={styles.adopted}>Adopted</Text>}
              </View>
              <Text style={styles.meta}>{pet.species === 'CAT' ? 'Cat' : 'Dog'} · {pet.shelter.name}</Text>
            </Pressable>
          ))}
        </View>

        {isLoading && <ActivityIndicator accessibilityLabel="Loading favorites" color={color.ink} size="large" />}

        {!isLoading && hasMorePages && (
          <Pressable role="button" onPress={() => void loadPage(nextPage)} style={styles.darkButton}>
            <Text style={styles.darkButtonText}>Load more</Text>
          </Pressable>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.brand },
  content: { width: '100%', maxWidth: 720, alignSelf: 'center', padding: 24, gap: 16 },
  backButton: {
    alignSelf: 'flex-start',
    minHeight: 48,
    justifyContent: 'center',
    borderRadius: 24,
    borderWidth: 1,
    borderColor: color.ink,
    backgroundColor: color.surface,
    paddingHorizontal: 20,
  },
  backButtonText: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  title: { color: color.ink, fontFamily: font.extraBold, fontSize: 40, lineHeight: 46 },
  errorBox: { backgroundColor: color.surface, borderRadius: 18, padding: 18, gap: 12, alignItems: 'center' },
  errorText: { color: color.ink, fontFamily: font.semiBold, fontSize: 17, textAlign: 'center' },
  empty: { color: color.ink, fontFamily: font.semiBold, fontSize: 18, paddingVertical: 20 },
  list: { gap: 12 },
  card: { backgroundColor: color.surface, borderRadius: 18, padding: 18, gap: 8 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  petName: { color: color.ink, fontFamily: font.extraBold, fontSize: 24 },
  adopted: {
    color: color.onSurfaceDisabled,
    backgroundColor: color.surfaceDisabled,
    fontFamily: font.semiBold,
    fontSize: 14,
    borderRadius: 999,
    paddingVertical: 4,
    paddingHorizontal: 12,
    overflow: 'hidden',
  },
  meta: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  darkButton: {
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 24,
    backgroundColor: color.ink,
    paddingHorizontal: 24,
  },
  darkButtonText: { color: color.surface, fontFamily: font.semiBold, fontSize: 17 },
});