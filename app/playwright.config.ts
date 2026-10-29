import { defineConfig } from '@playwright/test';

// Expects the API already running (docker compose up). Serves the exported web build.
export default defineConfig({
  testDir: './e2e',
  use: { baseURL: 'http://localhost:8081' },
  webServer: {
    command: 'npx expo export --platform web && npx serve dist -l 8081 -s',
    url: 'http://localhost:8081',
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
});
