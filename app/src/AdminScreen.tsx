import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AdminApi, adminApi } from './adminApi';
import { addressLine, initials, tintFor } from './adminFormat';
import { adminStyles } from './adminStyles';
import { AdminShelterScreen } from './AdminShelterScreen';
import { ShelterFormSheet } from './ShelterFormSheet';
import { useBackButton } from './useBackButton';
import { color, font } from './theme';
import { Shelter } from './types';

const LOAD_ERROR = "We couldn't load the shelters. Please try again.";

type AdminScreenProps = {
  onBack: () => void;
  // Tests pass a fake here. The real app uses the API.
  api?: AdminApi;
};

/** The admin home: every shelter, a search box and a button to add a shelter (S10). */
export function AdminScreen({ onBack, api = adminApi }: AdminScreenProps) {
  const [shelters, setShelters] = useState<Shelter[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [creating, setCreating] = useState(false);
  const [open, setOpen] = useState<Shelter | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setShelters(await api.listShelters());
    } catch {
      setError(LOAD_ERROR);
    }
  }, [api]);

  useEffect(() => {
    void load();
  }, [load]);

  // From a shelter, back goes to the list, which reloads in case it changed.
  const closeShelter = useCallback(() => {
    setOpen(null);
    void load();
  }, [load]);

  // One handler for Android's back button, for both the list and an open shelter.
  useBackButton(open ? closeShelter : onBack);

  if (open) {
    return <AdminShelterScreen shelter={open} api={api} onBack={closeShelter} />;
  }

  const words = query.trim().toLowerCase();
  const shown = (shelters ?? []).filter((shelter) =>
    `${shelter.name} ${shelter.address} ${shelter.zip}`.toLowerCase().includes(words));

  return (
    <SafeAreaView edges={['top']} role="main" style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.hero}>
          <Pressable role="button" accessibilityLabel="Back to search" onPress={onBack} style={styles.back}>
            <Text style={styles.backArrow}>←</Text>
          </Pressable>
          <Text role="heading" aria-level={1} style={styles.title}>Admin</Text>
          <Text style={styles.lead}>Shelters and the people who can post for them.</Text>
          <TextInput
            accessibilityLabel="Search shelters"
            value={query}
            onChangeText={setQuery}
            placeholder="Search shelters"
            placeholderTextColor={color.muted}
            style={styles.search}
          />
        </View>

        <View style={styles.body}>
          {error && (
            <View style={styles.center}>
              <Text role="alert" style={styles.error}>{error}</Text>
              <Pressable role="button" onPress={() => void load()} style={styles.retry}>
                <Text style={styles.retryText}>Try again</Text>
              </Pressable>
            </View>
          )}
          {!error && !shelters && <ActivityIndicator accessibilityLabel="Loading shelters" color={color.ink} style={styles.center} />}
          {shelters && (
            <>
              <Text style={styles.section}>
                {shelters.length === 1 ? '1 shelter' : `${shelters.length} shelters`}
              </Text>
              {shelters.length === 0 && <Text style={styles.empty}>No shelters yet. Add the first one.</Text>}
              {shelters.length > 0 && shown.length === 0 && (
                <Text style={styles.empty}>No shelters match "{query.trim()}".</Text>
              )}
              {shown.map((shelter) => (
                <Pressable key={shelter.id} role="button" onPress={() => setOpen(shelter)} style={[adminStyles.card, styles.card]}>
                  <View style={[styles.avatar, { backgroundColor: tintFor(shelter.id) }]}>
                    <Text style={styles.avatarText}>{initials(shelter.name)}</Text>
                  </View>
                  <View style={styles.grow}>
                    <Text style={styles.name}>{shelter.name}</Text>
                    <Text style={styles.meta}>{addressLine(shelter.address, shelter.zip)}</Text>
                  </View>
                  <Text style={styles.chevron}>›</Text>
                </Pressable>
              ))}
            </>
          )}
        </View>
      </ScrollView>

      <Pressable role="button" accessibilityLabel="New shelter" onPress={() => setCreating(true)} style={styles.fab}>
        <Text style={styles.fabPlus}>+</Text>
        <Text style={styles.fabText}>New shelter</Text>
      </Pressable>

      {creating && (
        <ShelterFormSheet
          title="New shelter"
          submitLabel="Create shelter"
          onClose={() => setCreating(false)}
          onSubmit={async (input) => {
            const created = await api.createShelter(input);
            setCreating(false);
            await load();
            setOpen(created);
          }}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.background },
  content: { width: '100%', maxWidth: 720, alignSelf: 'center', paddingBottom: 110 },
  hero: {
    backgroundColor: color.brand,
    paddingHorizontal: 22,
    paddingTop: 12,
    paddingBottom: 26,
    borderBottomLeftRadius: 28,
    borderBottomRightRadius: 28,
    gap: 12,
  },
  back: { width: 44, height: 44, justifyContent: 'center', marginLeft: -4 },
  backArrow: { color: color.ink, fontSize: 26, lineHeight: 30 },
  title: { color: color.ink, fontFamily: font.extraBold, fontSize: 34, lineHeight: 38 },
  lead: { color: color.onSurfaceDisabled, fontFamily: font.regular, fontSize: 15 },
  search: {
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
    borderRadius: 14,
    paddingVertical: 11,
    paddingHorizontal: 14,
    fontSize: 15,
    fontFamily: font.regular,
    color: color.ink,
  },
  body: { padding: 18, gap: 12 },
  section: { color: color.muted, fontFamily: font.semiBold, fontSize: 13, letterSpacing: 0.6, textTransform: 'uppercase' },
  empty: { color: color.ink, fontFamily: font.regular, fontSize: 16, paddingVertical: 12 },
  card: { padding: 14, flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: { width: 46, height: 46, borderRadius: 15, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: color.ink, fontFamily: font.extraBold, fontSize: 17 },
  grow: { flex: 1 },
  name: { color: color.ink, fontFamily: font.semiBold, fontSize: 17 },
  meta: { color: color.muted, fontFamily: font.regular, fontSize: 13, marginTop: 2 },
  chevron: { color: color.muted, fontSize: 22 },
  center: { alignItems: 'center', gap: 12, paddingVertical: 24 },
  error: { color: color.ink, fontFamily: font.semiBold, fontSize: 16, textAlign: 'center' },
  retry: { minHeight: 44, paddingHorizontal: 22, borderRadius: 22, backgroundColor: color.ink, justifyContent: 'center' },
  retryText: { color: color.surface, fontFamily: font.semiBold, fontSize: 15 },
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 24,
    height: 54,
    paddingHorizontal: 22,
    borderRadius: 27,
    backgroundColor: color.ink,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    shadowColor: color.ink,
    shadowOpacity: 0.3,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 8 },
    elevation: 6,
  },
  fabPlus: { color: color.surface, fontSize: 22, lineHeight: 24 },
  fabText: { color: color.surface, fontFamily: font.semiBold, fontSize: 16 },
});

