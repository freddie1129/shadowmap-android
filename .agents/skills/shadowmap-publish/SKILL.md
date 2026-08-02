---
name: shadowmap-publish
description: Use when publishing or releasing the ShadowMap Android app, including patch/minor/major version bumps, updating the app version, committing, tagging, and pushing to trigger Google Play deployment.
---

# ShadowMap Publish

Bump the app version, commit, tag, and push to trigger the Google Play deploy pipeline.

## Version Scheme

- Version name: `Major.Minor.Patch` such as `2.0.11`.
- Version code: `Major * 10000 + Minor * 100 + Patch`.
- Major can be any number of digits. Minor and Patch are each `0` through `99`.

Examples:

| Version name | Version code |
| --- | --- |
| 0.0.2 | 2 |
| 2.0.11 | 20011 |
| 2.1.0 | 20100 |
| 2.1.9 | 20109 |
| 2.10.0 | 21000 |
| 3.0.0 | 30000 |

## Bump Rules

Always read the current `versionCode` and `versionName` from `app/build.gradle.kts` first.

- Patch: increment Patch only, for example `2.0.11` to `2.0.12`.
- Minor: increment Minor and reset Patch to `0`, for example `2.0.11` to `2.1.0`.
- Major: increment Major and reset Minor and Patch to `0`, for example `2.3.7` to `3.0.0`.

After calculating the new version name, derive the new version code:

```text
versionCode = Major * 10000 + Minor * 100 + Patch
```

## Workflow

1. Read the current `versionCode` and `versionName` from `app/build.gradle.kts`.
2. Calculate the requested bump.
3. Show the current and new versions, then ask for confirmation before editing files or running Git commands.
4. Update both version fields in `app/build.gradle.kts`.
5. Stage and commit the version change.
6. Create the tag `v<versionName>`.
7. Push both the commit and tag. The tag triggers `.github/workflows/deploy.yml`, which publishes the signed Android App Bundle to Google Play's internal track.

## Rules

- Never assume the current version from memory.
- Never skip confirmation before editing files or running Git commands.
- Always push both the commit and the tag.
- If a push fails, report the error and stop.
