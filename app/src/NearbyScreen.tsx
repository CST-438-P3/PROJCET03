import { useState } from 'react';
import { ActivityIndicator, Image, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { color, font } from './theme';
import { getNearbyPets, NearbyPage, NearbyPet } from './nearbyApi';
import { ApiError } from './api';

type Loader = (zip: string, distance: number, species?: undefined, page?: number, size?: number) => Promise<NearbyPet[] | NearbyPage>;

type NearbyScreenProps = { loadPets?: Loader; onOpenPet?: (pet: NearbyPet) => void; onOpenAdmin?: () => void };

export function NearbyScreen({ loadPets = getNearbyPets, onOpenPet, onOpenAdmin }: NearbyScreenProps) {
  const [zip, setZip] = useState('93933');
  const [distance, setDistance] = useState('10');
  const [pets, setPets] = useState<NearbyPet[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  async function search(nextPage = 0) {
    setLoading(true);
    setError(null);
    try {
      const result = nextPage === 0
        ? await loadPets(zip, Number(distance))
        : await loadPets(zip, Number(distance), undefined, nextPage, 10);
      if (Array.isArray(result)) {
        setPets(result.slice(0, 10));
        setTotalPages(result.length > 10 ? Math.ceil(result.length / 10) : result.length ? 1 : 0);
        setTotalElements(result.length);
      } else {
        setPets(result.content);
        setPage(result.page);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
      }
      setPage(nextPage);
      setSearched(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "We couldn't load nearby pets. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <View role="main" style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        {onOpenAdmin && <Pressable role="button" onPress={onOpenAdmin} style={styles.adminButton}><Text style={styles.adminText}>Admin</Text></Pressable>}
        <Text role="heading" aria-level={1} style={styles.title}>Pets near you</Text>
        <Text style={styles.subtitle}>Find cats and dogs you can actually visit.</Text>
        <View style={styles.form}>
          <View style={styles.field}>
            <Text style={styles.label}>ZIP code</Text>
            <TextInput accessibilityLabel="ZIP code" value={zip} onChangeText={setZip} keyboardType="number-pad" maxLength={5} style={styles.input} />
          </View>
          <View style={styles.field}>
            <Text style={styles.label}>Distance (miles)</Text>
            <TextInput accessibilityLabel="Distance in miles" value={distance} onChangeText={setDistance} keyboardType="number-pad" style={styles.input} />
          </View>
          <Pressable role="button" accessibilityLabel="Search nearby pets" onPress={() => void search()} disabled={loading} style={styles.button}>
            {loading ? <ActivityIndicator color={color.surface} /> : <Text style={styles.buttonText}>Search</Text>}
          </Pressable>
        </View>
        {error && <Text role="alert" style={styles.error}>{error}</Text>}
        {searched && !loading && pets.length === 0 && <Text style={styles.empty}>No pets within {distance} miles.</Text>}
        <View accessibilityLabel="Nearby pet results" style={styles.results}>
          {pets.map((pet) => (
            <Pressable key={pet.id} role="button" disabled={!onOpenPet} onPress={() => onOpenPet?.(pet)} style={styles.card}>
              {pet.photoUrl && <Image accessibilityLabel={`${pet.name} photo`} source={{ uri: pet.photoUrl }} style={styles.photo} />}
              <View style={styles.cardTop}>
                <Text style={styles.petName}>{pet.name}</Text>
                <Text style={styles.distance}>{pet.distanceMiles} mi</Text>
              </View>
              <Text style={styles.meta}>{pet.species === 'CAT' ? 'Cat' : 'Dog'} · {pet.shelter.name}</Text>
              {!!pet.description && <Text style={styles.description}>{pet.description}</Text>}
            </Pressable>
          ))}
        </View>
        {searched && totalPages > 1 && (
          <View style={styles.pagination}>
            <Pressable accessibilityLabel="Previous page" onPress={() => void search(page - 1)} disabled={loading || page === 0} style={[styles.pageButton, page === 0 && styles.pageButtonDisabled]}>
              <Text style={styles.pageButtonText}>Previous</Text>
            </Pressable>
            <Text accessibilityLabel={`Page ${page + 1} of ${totalPages}`} style={styles.pageText}>Page {page + 1} of {totalPages} · {totalElements} pets</Text>
            <Pressable accessibilityLabel="Next page" onPress={() => void search(page + 1)} disabled={loading || page >= totalPages - 1} style={[styles.pageButton, page >= totalPages - 1 && styles.pageButtonDisabled]}>
              <Text style={styles.pageButtonText}>Next</Text>
            </Pressable>
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.brand },
  content: { width: '100%', maxWidth: 720, alignSelf: 'center', padding: 24, gap: 16 },
  title: { color: color.ink, fontFamily: font.extraBold, fontSize: 40, lineHeight: 46 },
  subtitle: { color: color.ink, fontFamily: font.regular, fontSize: 18 },
  form: { backgroundColor: color.surface, borderRadius: 20, padding: 16, gap: 12 },
  field: { gap: 6 },
  label: { color: color.ink, fontFamily: font.semiBold, fontSize: 15 },
  input: { borderColor: color.ink, borderWidth: 1, borderRadius: 10, padding: 12, fontSize: 17, color: color.ink },
  button: { minHeight: 48, alignItems: 'center', justifyContent: 'center', borderRadius: 24, backgroundColor: color.ink },
  buttonText: { color: color.surface, fontFamily: font.semiBold, fontSize: 17 },
  error: { backgroundColor: color.ink, color: color.surface, borderRadius: 12, padding: 12 },
  empty: { color: color.ink, fontFamily: font.semiBold, fontSize: 18, paddingVertical: 20 },
  results: { gap: 12 },
  adminButton: { alignSelf: 'flex-end', minHeight: 40, paddingHorizontal: 16, borderRadius: 20, backgroundColor: color.surface, justifyContent: 'center' },
  adminText: { color: color.ink, fontFamily: font.semiBold, fontSize: 15 },
  card: { backgroundColor: color.surface, borderRadius: 18, padding: 18, gap: 8 },
  photo: { width: '100%', height: 180, borderRadius: 12, backgroundColor: color.surfaceActive },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  petName: { color: color.ink, fontFamily: font.extraBold, fontSize: 24 },
  distance: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  meta: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  description: { color: color.ink, fontFamily: font.regular, fontSize: 15, lineHeight: 21 },
  pagination: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  pageButton: { backgroundColor: color.ink, borderRadius: 18, paddingVertical: 10, paddingHorizontal: 14 },
  pageButtonDisabled: { backgroundColor: color.surfaceDisabled },
  pageButtonText: { color: color.surface, fontFamily: font.semiBold },
  pageText: { color: color.ink, fontFamily: font.semiBold, textAlign: 'center', flex: 1 },
});
