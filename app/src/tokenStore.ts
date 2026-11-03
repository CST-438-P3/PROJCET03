import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

const KEY = 'api_token';

// Web keeps the token in memory only (lost on reload). Revisit in the web-token-storage ADR.
let memory: string | null = null;

/** Where the API JWT lives: SecureStore on Android/iOS, memory on web. */
export const tokenStore = {
  async get(): Promise<string | null> {
    return Platform.OS === 'web' ? memory : SecureStore.getItemAsync(KEY);
  },
  async set(token: string): Promise<void> {
    if (Platform.OS === 'web') memory = token;
    else await SecureStore.setItemAsync(KEY, token);
  },
  async clear(): Promise<void> {
    if (Platform.OS === 'web') memory = null;
    else await SecureStore.deleteItemAsync(KEY);
  },
};
