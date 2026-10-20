# 2–3 minute demo script

## Before the recording

1. Start backend: `cd backend; .\mvnw.cmd spring-boot:run`.
2. Start UI in a second terminal: `cd frontend; npm ci; npm run dev`.
3. Open `http://127.0.0.1:5173` and select **Kirana**.
4. Keep the disclosure visible: seeded records are synthetic; forecast figures are estimates.

## Walkthrough (about 2:20)

- **0:00–0:20 — Value in one screen.** Point to stock exposure, 30-day sales, expiry risk, and the recommended order cards. Say: “ShopPal turns shop sales and stock into an explainable next step.”
- **0:20–0:45 — Import.** Download the CSV template, select it in **Import sales**, inspect the preview, then import. Explain that malformed files and unknown SKUs are rejected before writing.
- **0:45–1:10 — Recommendation.** Open the order list and explain demand over supplier lead time plus the chosen planning horizon, safety stock, sellable stock, and pack rounding. Call out the synthetic-data label.
- **1:10–1:30 — Expiry Guard.** Open **Expiry Guard**. Show the expired oil batch and point out that the action says to quarantine it and never sell expired stock.
- **1:30–1:50 — Returns and evidence.** Switch to **Footwear**, open **Returns**, then **Supplier insights**. Show the return reason and low observation count; avoid calling the supplier unreliable from one record.
- **1:50–2:15 — Conversational lookup.** Open **ShopPal assistant** and ask “What expires in the next 7 days?” or choose the example prompt. Show the API source line and explain this is a data-grounded browser helper, not Alexa+.
- **2:15–2:25 — Close.** “ShopPal helps a shopkeeper spot avoidable inventory risks and make a better-informed decision; it does not guarantee sales or ratings.”

## If live data is unavailable

Do not hide the error. Show the API message, verify that both local processes are running, and retry. Do not substitute invented dashboard numbers.
