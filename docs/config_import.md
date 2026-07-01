# Config Import — JSON Schema

VigoTrack can bulk-import patients, stages, activities, and sensor-patient links from a single JSON file. Use the **+** FAB on the main screen → **Importer une config** to pick a file.

## Schema

### Top-level

```json
{
  "version": 1,
  "patients": [ ... ],
  "stages": [ ... ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `version` | integer | **Yes** | Must be `1`. |
| `patients` | array | No | Array of patient objects. |
| `stages` | array | **Yes** | Must contain at least one stage. |

### Patient

```json
{ "name": "Jean Dupont" }
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | **Yes** | Patient name. If a patient with the same name already exists, the import skips it. |

### Stage

```json
{
  "name": "Phase 1",
  "startDate": "2025-01-15",
  "endDate": "2025-03-15",
  "activities": [ ... ],
  "bilans": [ ... ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | **Yes** | Stage name. |
| `startDate` | string | **Yes** | `yyyy-MM-dd` or `yyyy-MM-dd'T'HH:mm:ss` |
| `endDate` | string | **Yes** | Same format as `startDate`. |
| `activities` | array | No | Activity objects for the **Activité** category. |
| `bilans` | array | No | Activity objects for the **Bilan** category. |

### Activity (appears in `activities` or `bilans`)

```json
{
  "type": "MARCHE",
  "typeDisplayName": "Marche rapide",
  "typeCategory": "ACTIVITE",
  "scheduledDate": "2025-02-01T09:00:00",
  "links": [ ... ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | **Yes** | Activity type name (e.g. `MARCHE`, `HIIT`, `TDM6`). If not a built-in type, it is automatically created as a custom type. |
| `typeDisplayName` | string | No | Human-readable label. Falls back to `type` if omitted. |
| `typeCategory` | string | No | `ACTIVITE` or `BILAN`. Defaults to `ACTIVITE`. |
| `scheduledDate` | string | **Yes** | `yyyy-MM-dd` or `yyyy-MM-dd'T'HH:mm:ss` |
| `links` | array | No | Array of link objects connecting this activity to a patient and sensor. |

### Link

```json
{
  "patientName": "Jean Dupont",
  "sensorId": "",
  "features": ["HR", "PPI", "ACC"]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `patientName` | string | **Yes** | Must match an existing patient (either already in the database or created earlier in the same import). |
| `sensorId` | string | **Yes** | The device address of a registered sensor. Must be non-blank. |
| `features` | array of strings | No | Features to track. Supported values: `HR`, `PPI`, `ACC`, `ECG`, `EULER`, `QUATERNION`, `FREE_ACCELERATION`. Defaults to `["HR"]`. |

## Built-in Activity Types

### Activité
| type | displayName |
|------|-------------|
| `MARCHE` | Marche |
| `APA` | APA |
| `HIIT` | HIIT |
| `RENFORCEMENT` | Renforcement |
| `PISCINE` | Piscine |

### Bilan
| type | displayName |
|------|-------------|
| `TDM6` | TDM6 |
| `_10M_1` | 10m 1 |
| `_10M_2` | 10m 2 |
| `_10M_3` | 10m 3 |
| `_10M_4` | 10m 4 |
| `REPOS_1` | Repos 1 |
| `REPOS_2` | Repos 2 |

## Example

```json
{
  "version": 1,
  "patients": [
    { "name": "Jean Dupont" },
    { "name": "Marie Martin" }
  ],
  "stages": [
    {
      "name": "Phase 1 - Post-op",
      "startDate": "2025-01-15",
      "endDate": "2025-03-15",
      "activities": [
        {
          "type": "MARCHE",
          "scheduledDate": "2025-02-01T09:00:00",
          "links": [
            {
              "patientName": "Jean Dupont",
              "sensorId": "",
              "features": ["HR", "PPI", "ACC"]
            }
          ]
        },
        {
          "type": "HIIT",
          "typeDisplayName": "HIIT adapté",
          "scheduledDate": "2025-02-03T14:00:00",
          "links": [
            {
              "patientName": "Jean Dupont",
              "sensorId": "",
              "features": ["HR", "ECG"]
            }
          ]
        }
      ],
      "bilans": [
        {
          "type": "TDM6",
          "scheduledDate": "2025-02-15T10:00:00",
          "links": [
            {
              "patientName": "Jean Dupont",
              "sensorId": "",
              "features": ["HR", "PPI"]
            }
          ]
        }
      ]
    },
    {
      "name": "Phase 2 - Renforcement",
      "startDate": "2025-03-16",
      "endDate": "2025-05-30",
      "activities": [
        {
          "type": "RENFORCEMENT",
          "scheduledDate": "2025-03-20T08:00:00",
          "links": [
            {
              "patientName": "Marie Martin",
              "sensorId": "F2:45:01:XX:XX:XX",
              "features": ["HR", "ACC"]
            }
          ]
        }
      ]
    }
  ]
}
```

## Behavior

- **Patients**: Duplicate names are silently skipped (no error, logged as skipped).
- **Activity types**: Unrecognized `type` values are automatically created as custom types and persisted.
- **Dates**: Two formats accepted: `yyyy-MM-dd` and `yyyy-MM-dd'T'HH:mm:ss`. Invalid dates cause the activity or stage to be skipped with an error message.
- **Links**: If `patientName` does not match any known patient (including those created earlier in the same import), that link is skipped with an error. If `sensorId` is blank, the link is skipped.
- **Result dialog**: After import, a dialog shows counts of created/skipped/errored items. Errors are listed individually.
