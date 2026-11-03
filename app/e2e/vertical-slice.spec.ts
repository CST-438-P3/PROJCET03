import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

// TODO(vertical slice): replace with the real slice once it exists. Sign in via the
// test-only token endpoint (Appendix I §6), walk the story, run axe on each page.
test('home page loads with no serious a11y violations', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('PROJECT03')).toBeVisible();

  const { violations } = await new AxeBuilder({ page }).analyze();
  const serious = violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
  expect(serious).toEqual([]);
});
