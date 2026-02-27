# Fireblocks External Key Store Release Notes

EULA: https://www.securosys.com/eula

## Fireblocks External Key Store 1.0.1

### Added

 - Introduced an additional error handler for TSB responses.
 - Improved handling of TSB error payloads (errorCode, reason, message).
 - Enhanced error mapping to provide more accurate and meaningful application-level error responses.

### Improved

- Better propagation of original TSB error details in case of request failures.
- More consistent error response structure returned by the service.

No breaking changes were introduced in this release.