# User Token Authentication — Design Spec

**Date:** 2026-04-20  
**Branch:** feature_search_in_theme  
**Status:** Approved

---

## Overview

Extend the existing `POST /api/v1/auth/login` endpoint to return a user-bound opaque token. Subsequent requests can authenticate via `Authorization: Bearer <token>` as an alternative to the existing session cookie. Existing session-based auth is not removed.

The new tokens are called **UserTokens** throughout the codebase to distinguish them from the static API tokens in `AuthorizationFilter`.

---

## 1. Data Model

New JPA entity `UserToken`, table `user_tokens`:

| Column | Type | Constraints |
|---|---|---|
| `token_id` | BIGINT | PK, auto-increment |
| `user_id` | BIGINT | FK → `viewer_users`, NOT NULL |
| `token_hash` | VARCHAR | NOT NULL, UNIQUE (SHA-256 hex of plaintext token) |
| `date_created` | DATETIME | NOT NULL |
| `expiration_date` | DATETIME | NULL = no expiry |

- Token plaintext = UUID v4; returned once at login, never persisted
- `token_hash` has an index for fast lookup
- Compound index on `(user_id, expiration_date)` for cleanup queries
- `UserToken.isExpired()` returns `true` if `expiration_date` is non-null and in the past
- Expired tokens for a user are cleaned up at their next login

---

## 2. DAO Layer

Four new methods added to `IDAO` interface and `JpaDao` implementation:

```java
Optional<UserToken> getUserTokenByTokenHash(String tokenHash);
boolean addUserToken(UserToken token) throws DAOException;
boolean deleteUserToken(UserToken token) throws DAOException;
void deleteExpiredUserTokensForUser(User user) throws DAOException;
```

- `getUserTokenByTokenHash` uses a TypedQuery on `token_hash`
- `deleteExpiredUserTokensForUser` deletes all tokens where `user_id = :userId AND expiration_date < NOW()`

---

## 3. Configuration

New key in `config_viewer.xml`:

```xml
<user>
    <authentication>
        <tokenExpirationDays>30</tokenExpirationDays>
    </authentication>
</user>
```

- Default: 30 days if not configured
- Value `0` or negative = tokens never expire (`expiration_date` stored as `null`)
- Read via `Configuration.getTokenExpirationDays()` returning `int`

---

## 4. Authentication Flow

### Login (`POST /api/v1/auth/login`)

1. Existing credential check (email/password, rate limiting) unchanged
2. On success:
   - Generate UUID v4 plaintext token
   - Compute SHA-256 hash
   - Compute `expiration_date` from `tokenExpirationDays` config (null if ≤ 0)
   - Persist `UserToken` with hash + dates
   - Delete expired tokens for this user
3. Return token plaintext in response body (only at this moment)

### Logout (`POST /api/v1/auth/logout`)

- New endpoint
- Expects `Authorization: Bearer <token>`
- Finds token by hash, deletes it
- Returns 200 in all cases (idempotent — unknown token also returns 200)

### `AuthenticationResponse` changes

- `sessionId` field **removed**
- New field `token` (String, `@JsonInclude(NON_NULL)`, only set on successful login)
- `retryAfterSeconds` remains (used for 429 responses)

---

## 5. Request Filter

Existing filters `UserLoggedInFilter` and `AdminLoggedInFilter` are extended. At the start of their `filter()` method:

1. Check for `Authorization: Bearer <token>` header
2. If present: compute SHA-256 hash, look up `UserToken` via DAO
3. Token valid and not expired → set user in request context, continue
4. Token expired → abort with 401 + `token_expired` message
5. Token unknown → abort with 401 + `invalid_token` message
6. No Bearer header → existing session check runs as before

`AdminLoggedInFilter` additionally verifies that the resolved user has admin privileges after successful token validation.

No new `@NameBinding` annotation is introduced. All endpoints already protected by these filters automatically support UserToken auth.

---

## 6. Error Handling

| Situation | HTTP Status | Response body |
|---|---|---|
| Token unknown / invalid | 401 | `{"status":"error","message":"invalid_token"}` |
| Token expired | 401 | `{"status":"error","message":"token_expired"}` |
| No Bearer header, no session | existing filter behavior | — |

The distinction between `invalid_token` and `token_expired` allows clients to show a targeted message to the user.

---

## 7. Testing

All tests written TDD (failing test first).

### `UserTokenTest` (unit)
- `isExpired()` returns `true` when `expiration_date` is in the past
- `isExpired()` returns `false` when `expiration_date` is in the future
- `isExpired()` returns `false` when `expiration_date` is null

### `AuthenticationEndpointTest` (extensions)
- Login response contains token on valid credentials
- Login response no longer contains sessionId
- Logout with valid token returns 200
- Logout with unknown token returns 200

### `UserLoggedInFilterTest` (new)
- Valid Bearer token → request passes through
- Expired Bearer token → 401 with `token_expired`
- Unknown Bearer token → 401 with `invalid_token`
- No Bearer header → existing session logic runs
