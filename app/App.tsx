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
import { LoginScreen } from './src/LoginScreen';
import { NearbyScreen } from './src/NearbyScreen';
import { getNearbyPets, NearbyPet } from './src/nearbyApi';
import { PostDetailScreen } from './src/PostDetailScreen';

async function signIn(): Promise<void> { }

export default function App() {
  const [signedIn, setSignedIn] = useState(false);
  const [openPet, setOpenPet] = useState<NearbyPet | null>(null);
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
      {/* Search stays mounted under the detail page, so going back keeps the results. */}
      {signedIn && (
        <View style={{ flex: 1, display: openPet ? 'none' : 'flex' }}>
          <NearbyScreen loadPets={getNearbyPets} onOpenPet={setOpenPet} />
        </View>
      )}
      {signedIn && openPet && (
        <PostDetailScreen postId={openPet.id} distanceMiles={openPet.distanceMiles} onBack={() => setOpenPet(null)} />
      )}
      <StatusBar style="light" />
    </SafeAreaProvider>
  );
}
