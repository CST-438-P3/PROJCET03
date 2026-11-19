import { useCallback, useEffect, useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, BackHandler, Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ApiError } from './api';
import { FavoriteButton } from './FavoriteButton';
import { getPost } from './postsApi';
import { color, font } from './theme';
import { Post } from './types';

const LOAD_ERROR = "We couldn't load this pet. Please try again.";

type PostDetailProps = {
  postId: number;
  // Distance from the list the user came from. Used when the post itself has no distanceMiles.
  distanceMiles?: number;
  onBack: () => void;
  // Tests pass a fake here. The real app uses the API.
  loadPost?: (postId: number) => Promise<Post>;
};

/** Turns months into "Under 1 month", "5 months", "1 year" or "2 years 3 months". */
export function formatAge(months: number): string {
  if (months < 1) return 'Under 1 month';
  const years = Math.floor(months / 12);
  const rest = months % 12;
  const monthText = `${rest} ${rest === 1 ? 'month' : 'months'}`;
  if (years === 0) return monthText;
  const yearText = `${years} ${years === 1 ? 'year' : 'years'}`;
  return rest === 0 ? yearText : `${yearText} ${monthText}`;
}

/** "SMALL" -> "Small". */
function titleCase(word: string): string {
  return word.charAt(0) + word.slice(1).toLowerCase();
}

/** One pet's page: photo, facts, shelter and about (S6). */
export function PostDetailScreen({ postId, distanceMiles, onBack, loadPost = getPost }: PostDetailProps) {
  const [post, setPost] = useState<Post | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [gone, setGone] = useState(false);
  const [photoFailed, setPhotoFailed] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      setPost(await loadPost(postId));
    } catch (e) {
      if (e instanceof ApiError && e.problem.status === 404) {
        setGone(true);
      } else {
        setError(LOAD_ERROR);
      }
    }
  }, [postId, loadPost]);

  useEffect(() => {
    void load();
  }, [load]);

  // Android's back button goes back to search instead of closing the app.
  useEffect(() => {
    const listener = BackHandler.addEventListener('hardwareBackPress', () => {
      onBack();
      return true;
    });
    return () => listener.remove();
  }, [onBack]);

  if (gone) {
    return (
      <View role="main" style={[styles.screen, styles.center]}>
        <Text role="heading" aria-level={1} style={styles.goneTitle}>This post is no longer available</Text>
        <Text style={styles.goneText}>The shelter may have removed it.</Text>
        <Pressable role="button" onPress={onBack} style={styles.darkButton}>
          <Text style={styles.darkButtonText}>Back to search</Text>
        </Pressable>
      </View>
    );
  }

  if (error) {
    return (
      <View role="main" style={[styles.screen, styles.center]}>
        <Text role="alert" style={styles.error}>{error}</Text>
        <Pressable role="button" onPress={() => void load()} style={styles.darkButton}>
          <Text style={styles.darkButtonText}>Try again</Text>
        </Pressable>
        <Pressable role="button" onPress={onBack} style={styles.lightButton}>
          <Text style={styles.lightButtonText}>Back to search</Text>
        </Pressable>
      </View>
    );
  }

  if (!post) {
    return (
      <View role="main" style={[styles.screen, styles.center]}>
        <ActivityIndicator accessibilityLabel="Loading pet" color={color.ink} size="large" />
      </View>
    );
  }

  const adopted = post.status === 'ADOPTED';
  const miles = post.distanceMiles ?? distanceMiles;
  const subtitle = [post.breed, post.sex && post.sex !== 'UNKNOWN' ? titleCase(post.sex) : undefined]
    .filter(Boolean)
    .join(' · ');
  const facts = [
    { label: 'Species', value: post.species === 'CAT' ? 'Cat' : 'Dog' },
    { label: 'Age', value: post.ageMonths === undefined ? 'Unknown' : formatAge(post.ageMonths) },
    { label: 'Size', value: post.size ? titleCase(post.size) : 'Unknown' },
  ];

  return (
    // Keeps the back and heart buttons below the phone's status bar.
    <SafeAreaView edges={['top']} role="main" style={styles.screen}>
      {/* Dark status bar icons, so they show on the white page. */}
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.photoBox}>
          {post.photoUrl && !photoFailed ? (
            <Image
              source={{ uri: post.photoUrl }}
              accessibilityLabel={`Photo of ${post.name}`}
              onError={() => setPhotoFailed(true)}
              style={[styles.photo, adopted && styles.photoAdopted]}
            />
          ) : (
            <View style={[styles.photo, styles.noPhoto]}>
              <Text style={styles.noPhotoText}>No photo yet</Text>
            </View>
          )}
          <View style={styles.photoButtons}>
            <Pressable role="button" accessibilityLabel="Back to search" onPress={onBack} style={styles.roundButton}>
              <Text style={styles.backArrow}>←</Text>
            </Pressable>
            <FavoriteButton postId={post.id} petName={post.name} startsSaved={post.favorite ?? false} />
          </View>
        </View>

        <View style={styles.body}>
          <View style={[styles.chip, adopted && styles.chipAdopted]}>
            <Text style={[styles.chipText, adopted && styles.chipTextAdopted]}>{adopted ? 'Adopted' : 'Available'}</Text>
          </View>
          <Text role="heading" aria-level={1} style={styles.name}>{post.name}</Text>
          {!!subtitle && <Text style={styles.subtitle}>{subtitle}</Text>}

          <View style={styles.facts}>
            {facts.map((fact) => (
              <View key={fact.label} style={styles.fact}>
                <Text style={styles.factLabel}>{fact.label}</Text>
                <Text style={styles.factValue}>{fact.value}</Text>
              </View>
            ))}
          </View>

          <View style={styles.card}>
            <Text style={styles.cardTitle}>{post.shelter.name}</Text>
            {miles !== undefined && <Text style={styles.cardText}>{miles} mi away</Text>}
          </View>

          {!!post.description && (
            <View style={styles.about}>
              <Text role="heading" aria-level={2} style={styles.sectionTitle}>About {post.name}</Text>
              <Text style={styles.description}>{post.description}</Text>
            </View>
          )}

          {adopted && (
            <View style={styles.adoptedNote}>
              <Text style={styles.adoptedText}>{post.name} found a home</Text>
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.surface },
  center: { alignItems: 'center', justifyContent: 'center', padding: 24, gap: 16 },
  content: { width: '100%', maxWidth: 720, alignSelf: 'center', paddingBottom: 32 },
  photoBox: { position: 'relative' },
  photo: { width: '100%', height: 330, backgroundColor: color.surfaceActive },
  photoAdopted: { opacity: 0.5 },
  noPhoto: { alignItems: 'center', justifyContent: 'center' },
  noPhotoText: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  photoButtons: {
    position: 'absolute',
    top: 16,
    left: 16,
    right: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  // 48 x 48 so it's easy to tap, the same size as the heart.
  roundButton: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: color.ink,
    backgroundColor: color.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backArrow: { color: color.ink, fontSize: 24, lineHeight: 28 },
  body: { padding: 24, gap: 14 },
  chip: { alignSelf: 'flex-start', backgroundColor: color.brand, borderRadius: 999, paddingVertical: 4, paddingHorizontal: 12 },
  chipAdopted: { backgroundColor: color.surfaceDisabled },
  chipText: { color: color.ink, fontFamily: font.semiBold, fontSize: 14 },
  chipTextAdopted: { color: color.onSurfaceDisabled },
  name: { color: color.ink, fontFamily: font.extraBold, fontSize: 36, lineHeight: 42 },
  subtitle: { color: color.ink, fontFamily: font.regular, fontSize: 17 },
  facts: { flexDirection: 'row', gap: 10 },
  fact: { flex: 1, backgroundColor: color.surfaceActive, borderRadius: 14, padding: 12, gap: 4 },
  factLabel: { color: color.ink, fontFamily: font.regular, fontSize: 13 },
  factValue: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  card: { borderColor: color.ink, borderWidth: 1, borderRadius: 16, padding: 16, gap: 4 },
  cardTitle: { color: color.ink, fontFamily: font.semiBold, fontSize: 17 },
  cardText: { color: color.ink, fontFamily: font.regular, fontSize: 15 },
  about: { gap: 8 },
  sectionTitle: { color: color.ink, fontFamily: font.extraBold, fontSize: 22 },
  description: { color: color.ink, fontFamily: font.regular, fontSize: 16, lineHeight: 23 },
  adoptedNote: { backgroundColor: color.surfaceDisabled, borderRadius: 24, padding: 14, alignItems: 'center' },
  adoptedText: { color: color.onSurfaceDisabled, fontFamily: font.semiBold, fontSize: 16 },
  goneTitle: { color: color.ink, fontFamily: font.extraBold, fontSize: 28, textAlign: 'center' },
  goneText: { color: color.ink, fontFamily: font.regular, fontSize: 16, textAlign: 'center' },
  error: { color: color.ink, fontFamily: font.semiBold, fontSize: 17, textAlign: 'center' },
  darkButton: { minHeight: 48, minWidth: 200, alignItems: 'center', justifyContent: 'center', borderRadius: 24, backgroundColor: color.ink, paddingHorizontal: 24 },
  darkButtonText: { color: color.surface, fontFamily: font.semiBold, fontSize: 17 },
  lightButton: { minHeight: 48, minWidth: 200, alignItems: 'center', justifyContent: 'center', borderRadius: 24, borderWidth: 1, borderColor: color.ink, paddingHorizontal: 24 },
  lightButtonText: { color: color.ink, fontFamily: font.semiBold, fontSize: 17 },
});
