import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { useEffect, useState } from 'react';
import {
  BricolageGrotesque_400Regular,
  BricolageGrotesque_600SemiBold,
  BricolageGrotesque_800ExtraBold,
} from '@expo-google-fonts/bricolage-grotesque';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { View } from 'react-native';
import { AdminScreen } from './src/AdminScreen';
import { LoginScreen } from './src/LoginScreen';
import { NearbyScreen } from './src/NearbyScreen';
import { getNearbyPets, NearbyPet } from './src/nearbyApi';
import { PostDetailScreen } from './src/PostDetailScreen';
import { rolesFromToken } from './src/roles';
import { tokenStore } from './src/tokenStore';

async function signIn(): Promise<void> { }

export default function App() {
  const [signedIn, setSignedIn] = useState(false);
  const [openPet, setOpenPet] = useState<NearbyPet | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);
  useEffect(() => {
    if (signedIn) void tokenStore.get().then((token) => setIsAdmin(rolesFromToken(token).includes('ADMIN')));
  }, [signedIn]);
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
      {signedIn && <View style={{ flex: 1, display: openPet || adminOpen ? 'none' : 'flex' }}><NearbyScreen loadPets={getNearbyPets} onOpenPet={setOpenPet} onOpenAdmin={isAdmin ? () => setAdminOpen(true) : undefined} /></View>}
      {signedIn && adminOpen && <AdminScreen onBack={() => setAdminOpen(false)} />}
      {signedIn && openPet && <PostDetailScreen postId={openPet.id} distanceMiles={openPet.distanceMiles} onBack={() => setOpenPet(null)} />}
      <StatusBar style="light" />
    </SafeAreaProvider>
  );
}
