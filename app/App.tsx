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
import { NearbyPet } from './src/nearbyApi';

// Temporary demo session and fixtures keep the vertical slice runnable while OAuth
// credentials and the external pet API key are pending.
const DEMO_PETS: NearbyPet[] = [
  { id: 1, shelter: { id: 1, name: 'Monterey County Animal Services' }, name: 'Luna', species: 'CAT', ageMonths: 24, description: 'A gentle lap cat who loves sunny windows.', status: 'AVAILABLE', distanceMiles: 0 },
  { id: 2, shelter: { id: 2, name: 'Salinas Valley Shelter' }, name: 'Max', species: 'DOG', ageMonths: 36, description: 'A playful dog looking for an active home.', status: 'AVAILABLE', distanceMiles: 5.1 },
  { id: 3, shelter: { id: 3, name: 'Santa Cruz Animal Care' }, name: 'Bella', species: 'DOG', ageMonths: 18, description: 'A friendly dog who enjoys walks.', status: 'AVAILABLE', distanceMiles: 31.2 },
];

async function signIn(): Promise<void> { }
async function loadDemoPets(_zip: string, distance: number): Promise<NearbyPet[]> {
  return DEMO_PETS.filter((pet) => pet.distanceMiles <= distance);
}

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
      {signedIn ? <NearbyScreen loadPets={loadDemoPets} /> : <LoginScreen onSignIn={async () => { await signIn(); setSignedIn(true); }} />}
      <StatusBar style="light" />
    </SafeAreaProvider>
  );
}
