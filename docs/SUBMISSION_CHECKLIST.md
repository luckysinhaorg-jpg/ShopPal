# Submission checklist

## Application

- [ ] Run the backend from a clean checkout with Java 21 and Maven Wrapper.
- [ ] Run the frontend from a clean checkout with `npm ci` and `npm run dev`.
- [ ] Import the CSV template and confirm recommendations refresh.
- [ ] Verify one malformed CSV is rejected and a repeated file is skipped.
- [ ] Verify expired stock is marked expired and is not presented as sellable.
- [ ] Connect a real MCP client to `http://127.0.0.1:8080/mcp`; list all five tools and invoke at least three against seeded data.
- [ ] Check browser console and backend logs on a machine where local loopback calls are available.
- [ ] Record all synthetic data, forecast, and financial estimate disclosures in the submission.

## Recording and submission

- [ ] Record a 2–3 minute walkthrough following [the demo script](DEMO.md).
- [ ] Show the CSV-to-recommendation path and one MCP client tool call.
- [ ] State clearly that there is no Alexa+ integration or certification claim.
- [ ] Include setup and known limitations from the README.
- [ ] Confirm all screenshots and submitted data are synthetic or authorized for sharing.
- [ ] Remove local secrets and generated files before packaging.

## Product feedback notes

No Amazon Developer Console, Alexa+ access, Bedrock account, or hackathon judging environment was used during this implementation. There are no observed Amazon product experiences to report as feedback. Ask a judge or developer to assess the onboarding clarity, MCP setup, and usefulness of risk explanations during the live demo; record their actual comments afterward.
