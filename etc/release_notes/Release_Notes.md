# Fireblocks External Key Store Release Notes

EULA: <https://www.securosys.com/eula>

## 1.0.3 (2026-05-28)

Added:

- Added Fireblocks Key Link proof-of-ownership signing support for event-based onboarding requests.
- Added support for `CONFIGURATION_MANAGER` as a separate Fireblocks signing service.
- Added service-specific Fireblocks signature verification configuration under `custom-server.signatureServices`.
- Added support for multiple Fireblocks signature certificates and public keys per service.
- Added an optional `enabled` flag per Fireblocks signature service. The flag defaults to `true`.
- Added negative validation for mismatched service and request type combinations.

Improved:

- Aligned proof-of-ownership handling with the Fireblocks custom server API by processing both transaction signing and proof-of-ownership requests through `/v1/messagesToSign`.
- Dispatches signing requests by Fireblocks service name and request type:
  - `SIGNING_SERVICE` handles `KEY_LINK_TX_SIGN_REQUEST`.
  - `CONFIGURATION_MANAGER` handles `KEY_LINK_PROOF_OF_OWNERSHIP_REQUEST`.
- Isolated certificate verification per Fireblocks service so Configuration Manager certificates are not mixed with Signing Service certificates.
- Kept backward compatibility for the legacy single-service signature verification configuration.
- Updated configuration templates and documentation for the new service-specific signature verification setup.
- Sanitized production logging to avoid writing API credentials, Fireblocks transaction payloads, proof-of-ownership messages, signatures, key labels, and raw TSB request/response bodies to logs. 
Operational logs still include request IDs, service names, request types, status changes, and safe warnings for authorization, mTLS, service configuration, and signature verification failures.

Tests:

- Added integration coverage for proof-of-ownership requests through `/v1/messagesToSign`.
- Added verification coverage for multiple whitelisted certificates, service isolation, disabled service behavior, and legacy configuration compatibility.

## 1.0.2 (2026-03-18)

Added:

- Added a new HSM connectivity check endpoint (`/v1/hsmConnectionCheck`) that calls TSB `licenseInfo` and returns license flags.

Improved:

- Simplified API key requirements so only Fireblocks endpoints require authorization.
- Updated API documentation to reflect per-endpoint security requirements.

## 1.0.1 (2026-02-27)

Added:

 - Introduced an additional error handler for TSB responses.
 - Improved handling of TSB error payloads (errorCode, reason, message).
 - Enhanced error mapping to provide more accurate and meaningful application-level error responses.

Improved:

- Better propagation of original TSB error details in case of request failures.
- More consistent error response structure returned by the service.

## 1.0.0

- Initial release
