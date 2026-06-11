# Phase 4: Crypto and steganography

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

- Migrate [`modules/crypto-core`](../../modules/crypto-core/build.gradle) only with strict test coverage for AES-GCM, RSA, hashing, signatures, key generation, key encoding, and file crypto workflows.
- Preserve byte-level behavior, exception semantics, and resource handling.
- Do not introduce coroutine-based crypto APIs unless a separate API design is approved.
- Migrate [`modules/stego-core`](../../modules/stego-core/build.gradle) after crypto-core validation.
- Preserve BMP capacity checks, header layout, payload encoding, password-based encryption integration, and oversized payload behavior.

Status: completed. [`modules/crypto-core`](../../modules/crypto-core/build.gradle) and [`modules/stego-core`](../../modules/stego-core/build.gradle) now use Kotlin JVM. Crypto services preserve AES-GCM/RSA/signature/hash/key/file workflow behavior, while encrypted payload model classes remain defensive-copy Kotlin classes rather than JVM records. Steganography preserves BMP capacity/header/payload behavior and password encrypt-then-hide integration.
