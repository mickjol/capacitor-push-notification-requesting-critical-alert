# Upgrading capacitor-push-notification

## Repo structure

This repository is a manual clone of the `push-notifications` plugin from
[ionic-team/capacitor-plugins](https://github.com/ionic-team/capacitor-plugins/tree/main/push-notifications),
with CAUCA-specific customizations layered on top.

The git history follows a deliberate two-commit model per upstream version:

```
<initial / older versions>
    ↓
Clone @capacitor/push-notifications <upstream-version>   ← pure upstream copy
    ↓
Version CAUCA du plugin                                   ← all CAUCA changes
```

**Never mix upstream and CAUCA changes into a single commit.** This structure
makes it straightforward to rebase CAUCA changes on a new upstream base.

---

## CAUCA customizations

The CAUCA commit always contains the following changes on top of the upstream base.

### Files added by CAUCA (not present in upstream)

**Android**
- `android/src/main/java/.../NotificationLogItem.java`
- `android/src/main/java/.../acknowledge/AcknowledgePublisher.java`
- `android/src/main/java/.../acknowledge/AcknowledgeService.java`
- `android/src/main/java/.../acknowledge/DeviceInfoExtractor.java`
- `android/src/main/java/.../acknowledge/NotificationDataExtractor.java`
- `android/src/main/java/.../storage/NotificationContentProvider.java`
- `android/src/main/java/.../storage/NotificationContentProviderDefinition.java`
- `android/src/main/java/.../storage/NotificationStorage.java`

**iOS**
- `ios/Sources/PushNotificationsPlugin/NotificationLogItem.swift`
- `ios/Sources/PushNotificationsPlugin/acknowledge/AcknowledgeService.swift`
- `ios/Sources/PushNotificationsPlugin/storage/NotificationStorage.swift`

### Files modified by CAUCA (also exist in upstream)

These files require careful manual merge when rebasing on a new upstream version.

| File | Nature of change |
|------|-----------------|
| `android/src/main/AndroidManifest.xml` | Added `<provider>` for `NotificationContentProvider` |
| `android/src/main/java/.../MessagingService.java` | Hooks into notification receipt to trigger acknowledge/storage |
| `android/src/main/java/.../PushNotificationsPlugin.java` | Exposes new plugin methods and wires storage |
| `ios/Sources/PushNotificationsPlugin/PushNotificationsHandler.swift` | Hooks into notification receipt for acknowledge/storage |
| `ios/Sources/PushNotificationsPlugin/PushNotificationsPlugin.swift` | Exposes new plugin methods and wires storage |
| `CHANGELOG.md` | CAUCA changelog entries prepended |
| `README.md` | CAUCA usage notes appended |
| `.gitignore` | Minor additions |

---

## How to upgrade to a new upstream version

### Prerequisites

- Access to [ionic-team/capacitor-plugins](https://github.com/ionic-team/capacitor-plugins)
- `git`, `node`, `npm` installed
- Know the target upstream version tag (e.g., `@capacitor/push-notifications@8.0.3`)

### Step 1 — Identify the upstream tag

Find the target release tag on the upstream repo:
```
https://github.com/ionic-team/capacitor-plugins/releases?q=push-notifications
```

The tag format is: `@capacitor/push-notifications@<version>`

### Step 2 — Review the upstream changelog

Before touching any code, read the upstream CHANGELOG for the version range you
are upgrading across. Pay special attention to changes in the files listed in the
**"Files modified by CAUCA"** table above — those are the ones that will conflict.

```
https://github.com/ionic-team/capacitor-plugins/blob/<tag>/push-notifications/CHANGELOG.md
```

### Step 3 — Create (or switch to) the upgrade branch

```bash
git checkout -b upgrade/v<major>
```

If the branch already exists:
```bash
git checkout upgrade/v<major>
```

### Step 4 — Reset to the upstream base commit

Find the hash of the **pure upstream base commit** (the "Clone @capacitor/push-notifications" commit):

```bash
git log --oneline
```

Reset the branch to that commit (this leaves the CAUCA commit unreachable but safe — its hash is recorded in this file):

```bash
git reset --hard <base-commit-hash>
```

### Step 5 — Download the new upstream version

Download the upstream `push-notifications` folder at the target tag.

**Option A — Download the archive (recommended)**

```bash
# Windows PowerShell
$tag = "@capacitor/push-notifications@8.0.3"
$encoded = [uri]::EscapeDataString($tag)
Invoke-WebRequest "https://github.com/ionic-team/capacitor-plugins/archive/refs/tags/$encoded.zip" -OutFile upstream.zip
Expand-Archive upstream.zip -DestinationPath upstream_extracted
```

The extracted folder will be at:
`upstream_extracted/capacitor-plugins-<tag-encoded>/push-notifications/`

**Option B — Clone and extract**

```bash
git clone --depth 1 --branch "@capacitor/push-notifications@8.0.3" \
  https://github.com/ionic-team/capacitor-plugins.git upstream_clone
# Then copy: upstream_clone/push-notifications/ into this repo
```

### Step 6 — Replace upstream-owned files

Copy all files from the extracted `push-notifications/` folder into this repo,
**overwriting** everything. This will overwrite the upstream-modified CAUCA files
(that is expected — the CAUCA changes will be re-applied in the next step).

Do **not** manually delete the CAUCA-added files listed above — the upstream
archive simply does not contain them, so they will not be touched.

```bash
# From the repo root — adjust path to your extracted folder
cp -r upstream_extracted/.../push-notifications/* .
```

Commit the pure upstream state:

```bash
git add -A
git commit -m "Clone @capacitor/push-notifications <new-version>"
```

### Step 7 — Re-apply the CAUCA commit

Cherry-pick the CAUCA commit from the previous version branch:

```bash
git cherry-pick <cauca-commit-hash>
```

This will likely produce conflicts in the files listed in the "modified by CAUCA"
table. For each conflict:

1. Open the file and look for `<<<<<<<` markers.
2. Keep the upstream v8 structure as the base.
3. Re-integrate the CAUCA addition (hook call, new method, provider declaration, etc.)
4. Mark the conflict resolved: `git add <file>`

Once all conflicts are resolved:

```bash
git cherry-pick --continue
# Use commit message: "Version CAUCA du plugin"
```

### Step 8 — Build and verify

```bash
npm install
npm run build
```

Verify `dist/` is regenerated. Commit if the build outputs changed:

```bash
git add dist/
git commit --amend --no-edit
```

### Step 9 — Update this file

Update the **Known CAUCA commit hashes** section below with the new commit hash.

### Step 10 — Open a PR

Push the branch and open a pull request targeting `master`.

---

## Known CAUCA commit hashes

| Upstream version | CAUCA commit |
|-----------------|--------------|
| 7.0.0 | `b279f6a1b9025d8c1411568f49bfbc2d9b2df49d` |
| 8.0.3 | `ce87399` |
