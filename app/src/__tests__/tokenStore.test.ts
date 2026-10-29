import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { tokenStore } from '../tokenStore';

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(async () => 'native-token'),
  setItemAsync: jest.fn(async () => {}),
  deleteItemAsync: jest.fn(async () => {}),
}));

const onPlatform = (os: typeof Platform.OS) =>
  Object.defineProperty(Platform, 'OS', { get: () => os, configurable: true });

test('web keeps the token in memory, never in SecureStore', async () => {
  onPlatform('web');
  await tokenStore.set('t1');
  expect(await tokenStore.get()).toBe('t1');
  await tokenStore.clear();
  expect(await tokenStore.get()).toBeNull();
  expect(SecureStore.setItemAsync).not.toHaveBeenCalled();
});

test('native uses SecureStore', async () => {
  onPlatform('android');
  await tokenStore.set('t2');
  expect(SecureStore.setItemAsync).toHaveBeenCalledWith('api_token', 't2');
  expect(await tokenStore.get()).toBe('native-token');
  await tokenStore.clear();
  expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('api_token');
});
