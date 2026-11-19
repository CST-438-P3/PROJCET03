import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { useState } from 'react';
import {
  BricolageGrotesque_400Regular,
  BricolageGrotesque_600SemiBold,
  BricolageGrotesque_800ExtraBold,
} from '@expo-google-fonts/bricolage-grotesque';
import { View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { FavoritesScreen } from './src/FavoritesScreen';
import { LoginScreen } from './src/LoginScreen';
import { NearbyScreen } from './src/NearbyScreen';
import { getNearbyPets } from './src/nearbyApi';
import { PostDetailScreen } from './src/PostDetailScreen';

async function signIn(): Promise<void> { }

// The pet whose detail page is open. Search results have a distance, favorites don't.
type OpenPet = { id: number; distanceMiles?: number };

export default function App() {
  const [signedIn, setSignedIn] = useState(false);
  const [openPet, setOpenPet] = useState<OpenPet | null>(null);
  const [favoritesOpen, setFavoritesOpen] = useState(false);
  const [loaded, error] = useFonts({
    BricolageGrotesque_400Regular,
    BricolageGrotesque_600SemiBold,
    BricolageGrotesque_800ExtraBold,
  });
  // If the font fails to load, carry on with the system face rather than a blank screen.
  if (!loaded && !error) return null;

  return (
    <SafeAreaProvider>
      {!signedIn && <LoginScreen onSignIn={async () => { await signIn(); setSignedIn(true); }} />}
      {/* Search stays mounted under the other pages, so going back keeps the results. */}
      {signedIn && (
        <View style={{ flex: 1, display: openPet || favoritesOpen ? 'none' : 'flex' }}>
          <NearbyScreen loadPets={getNearbyPets} onOpenPet={setOpenPet} onOpenFavorites={() => setFavoritesOpen(true)} />
        </View>
      )}

      {/* Favorites closes while a pet is open, so it reloads when you come back and a removed heart is gone. */}
      {signedIn && favoritesOpen && !openPet && (
        <FavoritesScreen onBack={() => setFavoritesOpen(false)} onOpenPet={(post) => setOpenPet({ id: post.id })} />
      )}
      {signedIn && openPet && (
        <PostDetailScreen postId={openPet.id} distanceMiles={openPet.distanceMiles} onBack={() => setOpenPet(null)} />
      )}
      <StatusBar style="light" />
    </SafeAreaProvider>
  );
}