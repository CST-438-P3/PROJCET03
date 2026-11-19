/**
 * The roles in the API token's "roles" claim, e.g. ["ADMIN"]. Empty if there is no token or it can't be read.
 * This only decides what the app shows. The API checks the role again on every admin request.
 */
export function rolesFromToken(token: string | null): string[] {
  if (!token) return [];
  try {
    // The middle part of a JWT is base64url JSON.
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const claims = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')));
    return Array.isArray(claims.roles) ? claims.roles : [];
  } catch {
    return [];
  }
}
