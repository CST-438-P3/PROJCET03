import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import {
  BricolageGrotesque_400Regular,
  BricolageGrotesque_600SemiBold,
  BricolageGrotesque_800ExtraBold,
} from '@expo-google-fonts/bricolage-grotesque';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ApiError } from './src/api';
import { LoginScreen } from './src/LoginScreen';

// ponytail: UI only, every sign-in fails until the PKCE flow and POST /auth/{provider} are wired.
async function signIn(): Promise<void> {
  throw new ApiError({ status: 501, detail: "Sign-in isn't connected yet." });
}

export default function App() {
  const [loaded, error] = useFonts({
    BricolageGrotesque_400Regular,
    BricolageGrotesque_600SemiBold,
    BricolageGrotesque_800ExtraBold,
  });
  // If the font fails to load, carry on with the system face rather than a blank screen.
  if (!loaded && !error) return null;

  return (
    <SafeAreaProvider>
      <LoginScreen onSignIn={signIn} />
      <StatusBar style="light" />
    </SafeAreaProvider>
  );
}
