# deploy/resource — Static Resource Files

This directory holds legal/doc files served via `GET /api/resource/{fileName}`.
It is gitignored — files here are mounted as a Docker volume in production.

## Naming Convention

```
{name}.md              → Chinese (default, no locale suffix)
{name}_{locale}.md     → Other locales, e.g. _en_US
```

## Required Files

| File | Description | Protected |
|------|-------------|-----------|
| `eula.md` | End User License Agreement | No |
| `eula_en_US.md` | End User License Agreement (English) | No |
| `about.md` | About the platform | No |
| `about_en_US.md` | About the platform (English) | No |
| `privacy.md` | Privacy policy | No |
| `privacy_en_US.md` | Privacy policy (English) | No |
| `terms.md` | Terms of service | No |
| `terms_en_US.md` | Terms of service (English) | No |
| `contact.md` | Contact information | Yes |
| `contact_en_US.md` | Contact information (English) | Yes |

Protected files require the user to be logged in.

## Adding New Files

1. Place the `.md` file in this directory
2. If it needs authentication, add the filename to `PROTECTED_FILES` in `LegalResourceConstants.java`
3. No code changes needed for public files — just drop them in

## Volume Mount (Docker)

```
deploy/resource/ → /app/deploy/resource (ro)
```

Override the base path via `APP_RESOURCES_PATH` env var.
