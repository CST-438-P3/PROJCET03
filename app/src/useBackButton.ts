import { useEffect } from 'react';
import { BackHandler } from 'react-native';

/** Makes Android's back button call onBack instead of closing the app, while the screen is open. */
export function useBackButton(onBack: () => void) {
  useEffect(() => {
    const listener = BackHandler.addEventListener('hardwareBackPress', () => {
      onBack();
      return true;
    });
    return () => listener.remove();
  }, [onBack]);
}

