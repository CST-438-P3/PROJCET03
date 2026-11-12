import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { useState } from 'react';
import {
  BricolageGrotesque_400Regular,
  BricolageGrotesque_600SemiBold,
  BricolageGrotesque_800ExtraBold,
} from '@expo-google-fonts/bricolage-grotesque';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { LoginScreen } from './src/LoginScreen';
import { NearbyScreen } from './src/NearbyScreen';
import { getNearbyPets } from './src/nearbyApi';

async function signIn(): Promise<void> { }

export default function App() {
  const [signedIn, setSignedIn] = useState(false);
  const [loaded, error] = useFonts({
    BricolageGrotesque_400Regular,
    BricolageGrotesque_600SemiBold,
    BricolageGrotesque_800ExtraBold,
  });
  // If the font fails to load, carry on with the system face rather than a blank screen.
  if (!loaded && !error) return null;

  return (
    <SafeAreaProvider>
      {signedIn ? <NearbyScreen loadPets={getNearbyPets} /> : <LoginScreen onSignIn={async () => { await signIn(); setSignedIn(true); }} />}
      <StatusBar style="light" />
    </SafeAreaProvider>
  );
}
