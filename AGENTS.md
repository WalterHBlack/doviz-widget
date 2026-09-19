# Working agreement

- Before each change round, commit and push the current safe project state to GitHub. Verify the remote commit before editing. Do not proceed when the backup fails.
- Never commit credentials, local SDK paths, private signing keys, or build outputs.
- Explain meaningful changes in simple Turkish, including which file owns the behavior. Keep learning notes in `docs/OGRENME.md` aligned with the code.
- Validate currency math and API parsing with unit tests. Run `assembleDebug`, `testDebugUnitTest`, and `lintDebug` before delivery.
- Clearly distinguish daily reference rates from live bank buy/sell rates. Never fabricate a rate when the service is unavailable.
