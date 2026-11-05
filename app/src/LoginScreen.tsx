import { useState } from 'react';
import {
  ActivityIndicator,
  Image,
  Pressable,
  PressableStateCallbackType,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';
import { ApiError } from './api';
import { color, font } from './theme';

export type Provider = 'github' | 'google';

const FALLBACK_ERROR = "We couldn't sign you in. Please try again.";

// Listed as CC0: https://www.rawpixel.com/image/6039717/photo-image-public-domain-cat-cute
const CAT_PHOTO = require('../assets/login-cat.webp');

/** Width where the photo moves from above the content to beside it. */
const WIDE = 900;
/** Room kept for the content, so the sign-in buttons fit the first screen on phones. */
const CONTENT_HEIGHT = 400;

function GitHubLogo() {
  return (
    <Svg width={24} height={24} viewBox="0 0 16 16">
      <Path
        fill={color.ink}
        d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"
      />
    </Svg>
  );
}

function GoogleLogo() {
  return (
    <Svg width={24} height={24} viewBox="0 0 48 48">
      <Path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <Path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <Path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <Path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </Svg>
  );
}

const PROVIDERS: { id: Provider; name: string; Logo: () => React.JSX.Element }[] = [
  { id: 'github', name: 'GitHub', Logo: GitHubLogo },
  { id: 'google', name: 'Google', Logo: GoogleLogo },
];

// `hovered` and `focused` are only set on web and are missing from the native types.
type ButtonState = PressableStateCallbackType & { hovered?: boolean; focused?: boolean };

/** The first screen. `onSignIn` resolves when signed in and throws to show an error. */
export function LoginScreen({ onSignIn }: { onSignIn: (provider: Provider) => Promise<void> }) {
  const [pending, setPending] = useState<Provider | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { width, height } = useWindowDimensions();
  const wide = width >= WIDE;

  async function signIn(provider: Provider) {
    setPending(provider);
    setError(null);
    try {
      await onSignIn(provider);
    } catch (e) {
      // The API's detail carries the PRD's copy, e.g. "This email is registered with GitHub".
      setError((e instanceof ApiError && e.problem.detail) || FALLBACK_ERROR);
    } finally {
      setPending(null);
    }
  }

  const photo = (
    <Image
      source={CAT_PHOTO}
      role="img"
      accessibilityLabel="A fluffy cat with blue eyes glaring grumpily at the camera"
      resizeMode="cover"
      style={
        wide
          ? styles.photoWide
          : [styles.photoNarrow, { height: Math.max(160, Math.min(height * 0.5, height - CONTENT_HEIGHT)) }]
      }
    />
  );

  return (
    <View role="main" style={[styles.screen, wide && styles.screenWide]}>
      <ScrollView style={styles.scroller} contentContainerStyle={styles.scroll}>
        {!wide && photo}
        <SafeAreaView edges={['bottom']} style={[styles.panel, wide ? styles.panelWide : styles.panelNarrow]}>
          <View style={styles.content}>
            <Text role="heading" aria-level={1} style={styles.wordmark}>
              Furever Near
            </Text>

            <View style={styles.pitch}>
              <Text style={styles.headline}>Pets you can actually visit.</Text>
              <Text style={styles.description}>
                See adoptable pets within the distance you choose.
              </Text>
            </View>

            {error && (
              <Text role="alert" style={styles.error}>
                {error}
              </Text>
            )}

            <View style={styles.buttons}>
              {PROVIDERS.map(({ id, name, Logo }) => {
                const disabled = pending !== null;
                return (
                  <Pressable
                    key={id}
                    role="button"
                    disabled={disabled}
                    aria-busy={pending === id}
                    onPress={() => void signIn(id)}
                    style={(state) => {
                      const { pressed, hovered, focused } = state as ButtonState;
                      return [
                        styles.button,
                        (pressed || hovered) && styles.buttonActive,
                        pressed && styles.buttonPressed,
                        focused && styles.buttonFocused,
                        disabled && styles.buttonDisabled,
                      ];
                    }}
                  >
                    {pending === id ? <ActivityIndicator color={color.onSurfaceDisabled} /> : <Logo />}
                    <Text style={[styles.buttonLabel, disabled && styles.buttonLabelDisabled]}>
                      {pending === id ? `Opening ${name}…` : `Sign in with ${name}`}
                    </Text>
                  </Pressable>
                );
              })}
            </View>
          </View>
        </SafeAreaView>
      </ScrollView>
      {wide && photo}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.brand },
  screenWide: { flexDirection: 'row' },
  scroller: { flex: 1 },
  scroll: { flexGrow: 1 },
  photoNarrow: { width: '100%' },
  photoWide: { width: '50%', height: '100%' },
  panel: { flexGrow: 1, backgroundColor: color.brand, paddingHorizontal: 24, paddingVertical: 28 },
  panelNarrow: { marginTop: -28, borderTopLeftRadius: 28, borderTopRightRadius: 28 },
  panelWide: { justifyContent: 'center', paddingHorizontal: 48 },
  content: { width: '100%', maxWidth: 440, alignSelf: 'center', gap: 20 },
  wordmark: { color: color.ink, fontFamily: font.extraBold, fontSize: 22, lineHeight: 28 },
  pitch: { gap: 8 },
  headline: { color: color.ink, fontFamily: font.extraBold, fontSize: 40, lineHeight: 44 },
  description: { color: color.ink, fontFamily: font.regular, fontSize: 17, lineHeight: 24 },
  error: {
    backgroundColor: color.ink,
    color: color.surface,
    fontFamily: font.semiBold,
    fontSize: 16,
    lineHeight: 24,
    borderRadius: 16,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  buttons: { gap: 12 },
  button: {
    minHeight: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 28,
    backgroundColor: color.surface,
  },
  buttonActive: { backgroundColor: color.surfaceActive },
  buttonPressed: { transform: [{ scale: 0.97 }] },
  buttonFocused: { outlineColor: color.ink, outlineStyle: 'solid', outlineWidth: 3, outlineOffset: 2 },
  buttonDisabled: { backgroundColor: color.surfaceDisabled },
  buttonLabel: { color: color.ink, fontFamily: font.semiBold, fontSize: 18, lineHeight: 24 },
  buttonLabelDisabled: { color: color.onSurfaceDisabled },
});
