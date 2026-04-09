# Fireblocks External Key Store Release Notes

EULA: <https://www.securosys.com/eula>

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
