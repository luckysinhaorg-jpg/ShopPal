# API reference

Base URL: `http://127.0.0.1:8080/api`

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Process health response |
| GET | `/dashboard?profile=KIRANA` | Summary counts, stock exposure, last-30-day units, expiry risks, and data origin |
| GET | `/products?profile=KIRANA` | Product list for a profile |
| GET | `/recommendations?profile=KIRANA&horizonDays=7` | Order recommendations; accepts optional `seasonalMultiplier`, `underageCost`, and `overageCost` |
| GET | `/expiry?days=30` | Expired, unknown, and upcoming-expiry batch alerts through the requested window |
| GET | `/returns?profile=KIRANA` | Return rates and available financial estimates |
| GET | `/durability` | Footwear durability evidence and sample size |
| GET | `/seasonality` | Calendar year and local adaptation note |
| GET | `/import/sample.csv` | Download the CSV template |
| POST | `/import/preview` | Preview a multipart file field named `file` |
| POST | `/import` | Validate and import the same multipart CSV; exact duplicate files are skipped |

CSV header must be `sku,date,unitsSold,unitPrice`. Dates use ISO `YYYY-MM-DD`; units must be positive whole numbers; prices must be nonnegative. Maximum file size is 1 MB. Unknown SKUs reject the entire import. The sample CSV uses seeded Kirana SKUs.

## Calculation notes

- Forecast: historical average per calendar day × recent 7-day trend clamped to 0.5–2.0 × one seasonal factor × supplier lead time plus requested planning horizon. Missing days count as zero demand.
- Order: target demand plus configured safety stock minus sellable inventory, rounded up to a whole unit and then to pack size. If sufficient history and positive underage/overage costs are passed, the service uses a normal approximation to the newsvendor quantile; otherwise it labels the deterministic fallback.
- Risk labels: stockout when sellable inventory is below forecast coverage demand; overstock when it exceeds target demand plus twice safety stock; expiry review when a batch is due within 30 days; unknown when there is no usable sales history. These are review signals, not guarantees.
- Returns: return rate is returned units ÷ sold units. Financial estimates are withheld if sales, return counts, sales prices, or purchase cost are inadequate. Refunds are estimated at current selling price; holding cost is subtracted only if supplied.
- Expiry: expired batches are marked unsellable by the alert and never given a discount or bundle recommendation.
