import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('signed-in user sees only pets within the chosen distance', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Furever Near' })).toBeVisible();
  await page.getByRole('button', { name: 'Sign in with GitHub' }).click();
  await expect(page.getByRole('heading', { name: 'Pets near you' })).toBeVisible();
  await page.getByRole('button', { name: 'Search nearby pets' }).click();
  await expect(page.getByText('Luna')).toBeVisible();
  await expect(page.getByText('Max')).toBeVisible();
  await expect(page.getByText('Bella')).toHaveCount(0);

  const { violations } = await new AxeBuilder({ page }).analyze();
  const serious = violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
  expect(serious).toEqual([]);
});
