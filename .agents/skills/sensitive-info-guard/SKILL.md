---
name: sensitive-info-guard
description: >-
  Guards against leaking, committing, or exposing sensitive information, private credentials,
  API keys, Wi-Fi passwords, signing keystores, and personal identifiable paths in public files.
---

# Sensitive Information & Secret Leak Prevention Guard

Use this skill whenever authoring code, committing to version control, preparing public releases, or reviewing pull requests. It guarantees that no credentials, private paths, or cryptographic keys are ever committed or exposed.

---

## 1. High-Risk Asset Categories

Never commit or hardcode the following items:

| Asset Category | High-Risk Patterns | Sanitized Replacement / Storage Policy |
| :--- | :--- | :--- |
| **Wi-Fi Credentials** | `const char* ssid = "..."`, `wifi_password`, SoftAP passkeys | Use captive portal / NVS input or placeholder: `"YOUR_WIFI_SSID"`, `"YOUR_WIFI_PASSWORD"`. |
| **Signing Keystores** | `*.jks`, `*.keystore`, `keyPassword`, `storePassword` | Store in local environment variables or external CI/CD secret vaults. Excluded in `.gitignore`. |
| **Local Machine Paths** | `C:\Users\<username>\...`, personal user directories | Use relative paths (`./`) or generic environmental references (`$HOME`, `$env:USERPROFILE`). |
| **Cryptographic Secrets** | `BEGIN PRIVATE KEY`, `.pem`, `.p12`, OAuth tokens, JWTs | Exclude via `.gitignore`. Never hardcode in test files or comments. |
| **Cloud & API Tokens** | Firebase tokens, AWS access keys, GitHub PATs | Use `.env` files (ignored by Git) or runtime parameter injection. |

---

## 2. Pre-Commit Audit Checklist

Before executing any `git commit` or pushing to a remote repository, audit with these steps:

1. **Check Staged Files**:
   ```powershell
   git status --short
   ```
   Ensure no `.env`, `local.properties`, `*.jks`, or build output cache files are staged.

2. **Regex Scan for Hardcoded Secrets**:
   Scan staged files for suspicious assignment patterns:
   * Regex: `(?i)(password|secret|api_key|token|private_key|keystore)\s*[:=]\s*["'][^"']+["']`
   * Regex for private keys: `-----BEGIN (RSA|EC|OPENSSH|PGP) PRIVATE KEY-----`

3. **Verify `.gitignore` Coverage**:
   Confirm that `.gitignore` is active and covering:
   * `android/local.properties` (contains absolute path to local Android SDK)
   * `*.keystore` and `*.jks`
   * Build caches (`.gradle/`, `.pio/`, `android/app/build/`)

---

## 3. Redaction & Sanitization Standards

When providing configuration examples, documentation, or public tutorials:
* **Placeholders**: Use uppercase, unmistakable placeholders:
  ```cpp
  // CORRECT:
  const char* default_ssid = "CONFIG_YOUR_SSID";
  const char* default_pass = "CONFIG_YOUR_PASSWORD";

  // STRICTLY FORBIDDEN:
  const char* default_ssid = "HomeNetwork_5G";
  const char* default_pass = "MySecretPass123!";
  ```
* **Redaction Tag**: When masking sensitive output in logs or summaries, use `[REDACTED]` or `<HIDDEN_KEY>`.

---

## 4. Emergency Remediation Protocol

If a secret is accidentally committed to Git:
1. **Immediate Revocation**: Immediately revoke, rotate, or re-issue the compromised credential.
2. **Git History Scrubbing**:
   * Do NOT merely make a new commit that deletes the file (the secret remains in Git history).
   * Use `git filter-repo` or `git reset HEAD~1` (if unpushed) to rewrite the commit without the sensitive file.
3. **Log Event**: Document the remediation action in `PROJECT_SUMMARY.md` without re-publishing the secret.
