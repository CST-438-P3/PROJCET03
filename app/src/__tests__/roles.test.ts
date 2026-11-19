import { rolesFromToken } from '../roles';

// Builds a token like the API's: header.payload.signature, with base64url JSON in the middle
function tokenWith(claims: object): string {
  const payload = btoa(JSON.stringify(claims)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `eyJhbGciOiJIUzI1NiJ9.${payload}.signature`;
}

test('reads the roles from the token', () => {
  expect(rolesFromToken(tokenWith({ sub: '1', roles: ['ADMIN'] }))).toEqual(['ADMIN']);
  expect(rolesFromToken(tokenWith({ sub: '2', roles: ['USER'] }))).toEqual(['USER']);
});

test('no token, a token without roles, or a broken token means no roles', () => {
  expect(rolesFromToken(null)).toEqual([]);
  expect(rolesFromToken(tokenWith({ sub: '1' }))).toEqual([]);
  expect(rolesFromToken('not-a-token')).toEqual([]);
});
