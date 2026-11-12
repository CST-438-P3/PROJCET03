import { useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { color, font } from './theme';
import { getNearbyPets, NearbyPet } from './nearbyApi';

type Loader = (zip: string, distance: number) => Promise<NearbyPet[]>;

export function NearbyScreen({ loadPets = getNearbyPets }: { loadPets?: Loader }) {
  const [zip, setZip] = useState('93933');
  const [distance, setDistance] = useState('10');
  const [pets, setPets] = useState<NearbyPet[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function search() {
    setLoading(true);
    setError(null);
    try {
      setPets(await loadPets(zip, Number(distance)));
      setSearched(true);
    } catch {
      setError("We couldn't load nearby pets. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <View role="main" style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
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
            <View key={pet.id} style={styles.card}>
              <View style={styles.cardTop}>
                <Text style={styles.petName}>{pet.name}</Text>
                <Text style={styles.distance}>{pet.distanceMiles} mi</Text>
              </View>
              <Text style={styles.meta}>{pet.species === 'CAT' ? 'Cat' : 'Dog'} · {pet.shelter.name}</Text>
              {!!pet.description && <Text style={styles.description}>{pet.description}</Text>}
            </View>
          ))}
        </View>
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
  card: { backgroundColor: color.surface, borderRadius: 18, padding: 18, gap: 8 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  petName: { color: color.ink, fontFamily: font.extraBold, fontSize: 24 },
  distance: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  meta: { color: color.ink, fontFamily: font.semiBold, fontSize: 16 },
  description: { color: color.ink, fontFamily: font.regular, fontSize: 15, lineHeight: 21 },
});
