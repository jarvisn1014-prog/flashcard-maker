# Sprint 1: AI Flashcard Maker — PM Learning Guide
## A Product Manager's Guide to Building an AI Product

---

## 1. Product Overview

### What We're Building
An Android app where users import text or PDFs, and AI generates flashcards with a spaced repetition study mode. Free, private, no account required — users bring their own API key.

### Target Audience
**Primary:** Students (high school, college, grad school) who:
- Already use Quizlet/Anki but resent paywalls and freemium restrictions
- Want AI-generated flashcards without a subscription
- Are privacy-conscious and prefer local-only data
- Are technically comfortable enough to get a free API key

**Secondary:** Self-learners and professionals studying for certifications (PMP, CSM, AWS, etc.)

**Why this audience matters:** Students are the highest-engagement user group for study tools. They're vocal, share apps with classmates, and generate organic word-of-mouth growth. They're also the most price-sensitive — which is why "free + BYOK" is a compelling value proposition.

### PM Insight — Jobs-to-be-Done Framework
When a student uses this app, they're "hiring" it to do a job:
> "When I have dense study material, I want to quickly turn it into reviewable flashcards so I can retain information for my exam."

The job has three parts:
1. **Input** — getting material into the app (the easier, the better)
2. **Transformation** — AI turning text into good flashcards (the smarter, the better)
3. **Output** — studying effectively (the more engaging, the better)

Most apps optimize for #3 (study mode) but make #1 and #2 painful. Our opportunity is making #1 and #2 effortless — that's the differentiator.

---

## 2. AI Concepts You'll Learn

### Concept 1: Prompt Engineering for Structured Output

**What it is:** Crafting LLM prompts that produce predictable, parseable, structured data (JSON) instead of free-form text.

**Why it matters:** This is the #1 skill for AI PMs. You're not coding the model — you're designing the instructions that make the model useful. The quality of your prompt determines the quality of your product.

**How we'll use it:** We'll ask Gemini to generate flashcards in JSON format:
```
Generate flashcards from the following text. Return a JSON array where each object has "front" and "back" keys. Keep fronts concise (1 sentence). Keep backs under 3 sentences. Generate 10-20 cards.

Text: [user's input]
```

**Trade-off: Prompt flexibility vs. output reliability**
- More flexible prompt → better quality cards, but harder to parse
- More rigid prompt → easier to parse, but cards may feel formulaic
- **Our choice:** Rigid JSON output with a detailed system prompt. We prioritize reliability over creative variation. Students need consistent, parseable cards — not surprises.

**PM Insight:** In AI products, the prompt IS the product. A 10% improvement in prompt quality = a 10% improvement in user outcomes. This is where AI PMs add value — not in model training, but in prompt design and iteration.

### Concept 2: BYOK (Bring Your Own Key) Model

**What it is:** Instead of paying for API access and charging users, users provide their own API key. The app makes calls directly from the user's device to the LLM provider.

**Why it matters:** This is a business model decision with major implications:

| Aspect | BYOK Model | Hosted Model (you pay) |
|--------|-----------|----------------------|
| Your cost | $0 — users pay their own API costs | You pay per API call |
| User friction | Higher — must get and enter API key | Lower — just open and use |
| Privacy | Maximum — no data touches your servers | User data flows through your backend |
| Scalability | Infinite — no server costs | Limited by your API budget |
| Monetization | Hard — why pay when they have the key? | Easy — subscription, freemium |
| Trust | High — users control their data | Requires trust in your privacy practices |

**Trade-off: Friction vs. cost**
- BYOK adds friction (getting an API key is a barrier for non-technical users)
- But it gives us $0 operating cost and maximum privacy
- **Our choice:** BYOK for this hobby project. If we ever monetize, we'd add a "use our API" option alongside BYOK.

**PM Insight:** Business model decisions ARE product decisions. The choice of BYOK shapes the entire UX: we need a great onboarding flow for API key setup, clear instructions, and graceful error handling when keys are invalid or rate-limited. This is a "non-functional requirement" that becomes a core feature.

### Concept 3: Spaced Repetition (SM-2 Algorithm)

**What it is:** A scheduling algorithm that determines when to show a flashcard again based on how well the user remembered it. Cards you struggle with appear more frequently; easy cards appear less often.

**Why it matters:** This is the "science" behind effective learning apps. It's not AI — it's a well-established cognitive science principle. But it's what makes flashcard apps actually work (vs. just reading notes).

**How it works (simplified):**
1. User rates their recall: Again (0), Hard (3), Good (4), Easy (5)
2. Algorithm calculates:
   - **Easiness factor** (starts at 2.5, adjusts based on performance)
   - **Interval** (days until next review: 1 → 3 → 7 → 16 → 35...)
   - **Repetition number** (how many times successfully recalled)
3. Card is scheduled for its next review date

**Trade-off: Algorithm complexity vs. user understanding**
- Full SM-2 is complex but proven (used by Anki, SuperMemo)
- Simplified version (fixed intervals) is easier to build but less effective
- **Our choice:** Full SM-2. It's a well-documented algorithm, and Anki's open-source code provides reference. The complexity is in the algorithm, not the UI — users just see "when to review."

**PM Insight:** Sometimes the most important product features aren't visible to users. Spaced repetition is invisible — users just see cards appear at the right time. But it's the core value proposition. As a PM, you need to understand which invisible features matter and fight for them in the roadmap.

### Concept 4: Local-First Architecture

**What it is:** All data is stored on the user's device (Room/SQLite). No cloud sync, no account, no server. The app works fully offline.

**Why it matters:**
- **Privacy:** User data never leaves their device (except the LLM API call)
- **Performance:** No network latency for reading/studying
- **Cost:** No server costs
- **Simplicity:** No auth, no sync conflicts, no backend to maintain

**Trade-off: Convenience vs. privacy/simplicity**
- No sync means users lose data if they switch devices
- No account means no social features, no sharing
- **Our choice:** Local-first for MVP. If users request sync, we add Supabase free tier later. This follows the "build what users ask for" principle.

**PM Insight:** Architecture decisions are product decisions. Choosing local-first means we're choosing a specific user: someone who values privacy and simplicity over cross-device convenience. We're implicitly defining our audience through technology choices.

---

## 3. Decision Log — Every Choice and Why

### Decision 1: Kotlin + Jetpack Compose (not Flutter/React Native)

**Options considered:**
- **Kotlin + Compose** — native Android, best performance, your existing skills
- **Flutter** — cross-platform, one codebase for Android + iOS
- **React Native** — cross-platform, JavaScript ecosystem
- **PWA** — web app, no install needed, limited device access

**Why Kotlin + Compose:**
- You're already skilled — zero learning curve for the framework
- Best performance and native feel (important for a study app with smooth card animations)
- Compose's declarative UI model is ideal for flashcard flip animations
- Pixel 10 Pro is your test device — native = best testing experience

**Trade-off:** Android-only. If we want iOS later, we'd need to rebuild or use KMP (Kotlin Multiplatform).
**Why it's worth it:** For a 10-day sprint, eliminating framework learning time is critical. Cross-platform can come later if the app gains traction.

**PM Insight — Platform strategy:** Choosing a platform is choosing a market. Android-only means we're targeting Android users (broader globally, more price-sensitive, more technical users who can handle BYOK). iOS users tend to be less technical and more willing to pay — which actually makes BYOK a worse fit for iOS. Our platform choice aligns with our business model.

### Decision 2: Gemini API (not OpenAI/Claude)

**Options considered:**
- **Google Gemini API** — free tier (15 RPM, 1500 req/day), good quality, Android-native ecosystem
- **OpenAI API** — free credits for new users, best quality, most documented
- **Anthropic Claude** — good quality, smaller free tier
- **Local Ollama** — $0, fully offline, but requires user's hardware

**Why Gemini:**
- Best free tier for a BYOK model (users get generous free limits)
- Google has the most Android-developer-friendly SDK and documentation
- Gemini 2.0 Flash is fast enough for real-time card generation
- Users can get a key from Google AI Studio (free, no credit card)

**Trade-off:** Gemini may produce slightly lower quality output than GPT-4 for complex text. But for flashcard generation (simple Q&A pairs), the quality difference is negligible.
**Risk mitigation:** We'll support multiple providers via a simple interface. Users can enter an OpenAI key instead — the app detects the key format and routes accordingly.

**PM Insight — Vendor strategy:** In AI products, your LLM provider is a strategic dependency. Choosing Gemini ties us to Google's ecosystem (which aligns with Android). But we should always design for provider-agnosticism — the app should work with any OpenAI-compatible API. This reduces lock-in risk and lets users choose based on their preferences.

### Decision 3: BYOK (not hosted API)

**Options considered:**
- **BYOK** — users bring their own API key, $0 cost for us
- **Shared API key** — we pay, all users share our rate limit
- **Freemium** — free tier with our key, paid tier for more usage

**Why BYOK:**
- $0 operating cost (critical for hobby project)
- Maximum user privacy (API calls go direct from device to provider)
- No backend to maintain
- Teaches users about AI APIs (educational value aligns with learning project)

**Trade-off:** Higher user friction. Getting an API key requires:
1. Going to Google AI Studio
2. Creating an account
3. Generating a key
4. Pasting it into the app

This is a 4-step barrier. Most non-technical users will bounce. But our target audience (students, self-learners) is increasingly comfortable with API keys thanks to the AI boom.

**Risk mitigation:** We'll create a beautiful, step-by-step onboarding flow with screenshots, a "Get your free key" button that opens the browser to the right page, and a test-key feature that validates the key before saving.

**PM Insight — Onboarding as a feature:** The API key setup IS a feature, not a bug. It's an opportunity to teach users something valuable (how AI APIs work), build trust (we don't have their key), and differentiate from competitors (who hide behind a freemium wall). Frame friction as empowerment.

### Decision 4: Room/SQLite (not cloud database)

**Options considered:**
- **Room/SQLite** — local, free, no backend
- **Supabase** — free tier, PostgreSQL, real-time sync
- **Firebase Firestore** — free tier, NoSQL, Google ecosystem

**Why Room:**
- No backend = $0 cost, no maintenance, no auth needed
- Works offline (critical for studying on the go)
- Room is the standard Android local DB — excellent documentation
- Flashcard data is simple (decks, cards, review history) — no need for cloud complexity

**Trade-off:** No cross-device sync. If a user studies on their phone and wants to continue on a tablet, they can't.
**Future path:** If users request sync, we add Supabase as an optional layer. Room already has offline-first patterns that sync well with Supabase.

**PM Insight — MVP scope:** The hardest PM skill is deciding what NOT to build. Cloud sync is a "nice to have" that doubles the complexity. For an MVP, local-only is the right call. The rule: if 80% of users don't need it, don't build it in v1.

### Decision 5: Spaced Repetition with SM-2 (not simple review)

**Options considered:**
- **No scheduling** — all cards available all the time (like a simple deck)
- **Random review** — shuffle cards randomly
- **SM-2 algorithm** — science-based scheduling (Anki-style)

**Why SM-2:**
- It's the proven science behind effective learning
- It's the differentiator vs. simple "flashcard" apps
- It's a well-documented algorithm (implementation is ~100 lines of code)
- It creates the "come back tomorrow" habit loop (retention driver)

**Trade-off:** SM-2 adds complexity (review scheduling, interval calculation, easiness factor). But it's the core value — without it, we're just a card viewer.
**Why it's worth it:** The "spaced repetition" label is what makes users choose Anki over Quizlet. It's a feature that sounds smart (because it is) and creates retention (because it works).

**PM Insight — Core vs. nice-to-have:** SM-2 is core. Card animations are nice-to-have. The temptation is to spend time on animations because they're visible and fun. But a PM's job is to prioritize the invisible science that makes the product work. Ship SM-2 first, polish animations later.

---

## 4. Competitive Analysis

### Direct Competitors

| App | Model | Strengths | Weaknesses | Our Edge |
|-----|-------|-----------|-----------|----------|
| **Quizlet** | Freemium ($7.99/mo) | Massive content, brand, AI features now | Paywall for core features, no BYOK | Free + private + BYOK |
| **Anki** | Free (open source) | Powerful, SM-2, huge community | Ugly UI, steep learning curve, desktop-first | Clean Compose UI, mobile-first, AI generation |
| **Knowt** | Freemium | 7M users, AI flashcards, free learn mode | Freemium pushing, account required | No account, local-first, BYOK |
| **Coconote** | Freemium | AI note-taking + flashcards, growing fast | Complex, many features beyond flashcards | Single-purpose, focused, fast |

### Market Positioning

We're positioning in the gap between Anki (powerful but ugly) and Quizlet (beautiful but paywalled):

```
Price ────────────────────────────────────────────
  $$$$ │  Quizlet Premium
       │
  $$   │              Knowt Freemium
       │                    Coconote
  $0   │  Anki (free)    ──► OUR APP ◄──
       │                      (free, BYOK, AI)
Quality ──────────────────────────────────────────
 Ugly  │  Anki
       │                    Knowt
  Clean│              ──► OUR APP ◄──
       │  Quizlet
Beauty │
```

**PM Insight — Positioning:** "The beautiful, free Anki alternative with AI" is a clear, defensible position. It gives users a reason to switch (beauty + AI) and a reason to stay (free + private). Your positioning should be one sentence that a user can repeat to a friend.

---

## 5. User Journey Map

### Stage 1: Discovery & Onboarding (First 5 Minutes)
1. User installs APK (sideloaded for now)
2. Opens app → clean welcome screen with value proposition
3. Prompted to enter API key → step-by-step guide with link to Google AI Studio
4. Key validated (test API call: "Generate one sample flashcard")
5. Ready to create first deck

**PM focus:** Minimize time-to-value. User should create their first deck within 3 minutes of opening the app. If they can't, they'll leave.

### Stage 2: Core Loop (Daily Usage)
1. Import notes/PDF → AI generates flashcards (10 seconds)
2. Review generated cards → edit/add/remove (2 minutes)
3. Study session: flip cards, self-assess (5-15 minutes)
4. App schedules next review based on SM-2
5. Next day: app shows "X cards to review" → habit loop

**PM focus:** The core loop is import → generate → study → review. Every feature must support this loop. Anything that doesn't (sharing, social, themes) is v2.

### Stage 3: Retention (Ongoing)
1. Daily review notification ("You have 12 cards to review")
2. Streak counter (gamification)
3. Weekly progress summary
4. Deck mastery percentage

**PM focus:** Retention is driven by SM-2 scheduling (the science) and streaks (the psychology). Don't add social features for retention — they don't work for single-user study tools.

---

## 6. Success Metrics (Even for a Hobby Project)

| Metric | Target (30 days) | Why it matters |
|--------|-------------------|----------------|
| Time to first deck | < 3 minutes | Onboarding friction measurement |
| Card generation success rate | > 95% | AI reliability (prompt quality) |
| Average cards per deck | 15-25 | AI output quality (not too many, not too few) |
| Study session completion rate | > 80% | Core loop engagement |
| Day 1 → Day 7 retention | > 40% | SM-2 habit loop working |
| API key setup completion rate | > 70% | BYOK friction measurement |

**PM Insight:** Even without analytics infrastructure, defining metrics shapes your product thinking. When you build a feature, ask: "Which metric does this move?" If the answer is "none," reconsider.

---

## 7. Key PM Concepts Demonstrated in This Sprint

### Jobs-to-be-Done (JTBD)
We're not building a "flashcard app." We're helping students "turn dense material into reviewable knowledge quickly." The job defines the product, not the category.

### Minimum Viable Product (MVP)
MVP = the smallest thing that delivers the core value. For us: import text → AI generates cards → study with spaced repetition. That's it. No sync, no social, no themes, no dark mode toggle (use system default).

### North Star Metric
For a study app: **weekly active study sessions per user**. If users study at least 3x per week, the app is working. If they open it once and never return, SM-2 isn't creating the habit loop.

### Build-Measure-Learn Loop
1. **Build** → ship the MVP in 10 days
2. **Measure** → use it yourself for a week, note pain points
3. **Learn** → what broke, what was slow, what was confusing
4. **Iterate** → fix top 3 issues in Sprint 2's buffer time

### Product-Market Fit Signals (Even for Hobbies)
- You use it yourself and it replaces Quizlet/Anki → strong signal
- You show a friend and they ask "where can I get this?" → very strong
- You show a friend and they say "cool" and move on → weak signal
- You can't explain what it does in one sentence → positioning problem

### Technical Debt as a Product Decision
Every shortcut in the MVP is a debt you'll pay later. Examples:
- Hardcoded API key format (only Gemini) → debt: need to refactor for multi-provider
- No error categorization (all errors show "something went wrong") → debt: need granular error handling
- No tests → debt: risky refactors

**PM Insight:** Technical debt is OK if it's intentional. Document what you're deferring and why. The danger is unconscious debt — shortcuts you don't realize you're making.

---

## 8. Risk Register

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Gemini API rate limits for power users | Medium | High | Batch generation (1 API call per deck), cache results locally |
| AI generates poor quality flashcards | Medium | High | Prompt engineering iteration, user can edit cards before saving |
| BYOK friction kills onboarding | High | High | Step-by-step onboarding with screenshots, "Get free key" deep link |
| PDF parsing fails on complex documents | Low | Medium | Fallback to text paste, show error with guidance |
| SM-2 implementation bugs cause wrong scheduling | Low | High | Unit test the algorithm, compare with Anki's behavior |
| App feels too basic vs. Quizlet/Knowt | Medium | Medium | Focus on speed and cleanliness — "it just works" positioning |

---

## 9. What You'll Be Able to Talk About in Interviews

After building this app, you can speak to:

1. **AI Product Design:** "I designed prompt templates for structured LLM output, iterating on quality based on user testing with real study material."

2. **BYOK Business Model:** "I chose a bring-your-own-key model to achieve $0 operating cost while maximizing user privacy — and designed the onboarding flow to minimize friction."

3. **AI Vendor Strategy:** "I evaluated Gemini, OpenAI, and Claude for a mobile AI app, choosing Gemini for its free tier and Android ecosystem alignment, while designing for provider-agnosticism."

4. **Local-First Architecture:** "I made a deliberate architecture choice to go local-first (Room, no backend) for MVP, trading cross-device sync for $0 cost, privacy, and offline capability."

5. **Spaced Repetition Science:** "I implemented the SM-2 algorithm to create a retention-driving habit loop, prioritizing invisible science over visible polish."

6. **MVP Scoping:** "I scoped the MVP to 4 screens and 1 core loop, deferring cloud sync, social features, and dark mode to v2 based on user demand signals."

7. **Metrics-Driven Thinking:** "I defined success metrics before building — time-to-first-deck, card generation success rate, day-7 retention — and used them to prioritize features."

8. **Competitive Positioning:** "I positioned the app as 'the beautiful, free Anki alternative with AI' — targeting the gap between paywalled apps and powerful-but-ugly open-source tools."