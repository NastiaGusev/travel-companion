# AI Extraction Service

Small FastAPI service for the Travel Companion **quick-add** feature: it turns
free trip text into structured days/stops. It is a pure text→structure
transformer — stateless, no database, no Google, no trip business logic. The
Kotlin backend calls it, then resolves each stop against Google Places and
saves.

## Contract

`POST /extract`

Request:
```json
{ "text": "day one: lunch at Tsukiji 12:30, then TeamLab. day two: dinner at Gonpachi" }
```

Response:
```json
{
  "days": [
    { "position": 1, "stops": [
      { "searchQuery": "Tsukiji Market", "name": "Tsukiji Market", "timeHint": "12:30" }
    ] }
  ]
}
```

- `days` are in the order described; `position` is 1-based (not a calendar date).
- No day structure in the text → all stops in one day at `position: 1`.
- Nothing extractable → `{ "days": [] }`.
- `timeHint` is optional (clock time, or morning/afternoon/evening, or omitted).

## Provider seam

The LLM provider sits behind the `Extractor` interface (`app/extractors/base.py`).
`app/extractors/factory.py` picks the implementation from `EXTRACTOR_PROVIDER`
(currently `stub`, no LLM). Adding Anthropic/OpenAI is a branch in the factory;
nothing else changes.

## Run locally

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
# Swagger UI at http://localhost:8000/docs
```

## Test

```bash
pytest
```