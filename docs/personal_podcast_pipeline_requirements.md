# Personal Podcast Pipeline Requirements

## Goal
Create a CLI-driven workflow that turns individual links (Hacker News, Reddit, blogs, or local text files) into narrated podcast episodes hosted privately on DigitalOcean Spaces.

## Core Capabilities
- Fetch and normalize source content, classifying discussion threads vs standalone articles.
- Summarize the main post and generate comment buckets with representative quotes when applicable.
- Assemble a structured narration script (intro, synopsis, comment recap, full read-through when desired).
- Synthesize audio using OpenAI TTS with configurable prompt and voice presets.
- Store episode metadata locally and maintain transcripts/artifacts for replay or regeneration.
- Upload episodes and feeds to DigitalOcean Spaces with restricted discoverability, updating the RSS feed per new episode.
- Include metadata including original link for each episode in rss feed. Possibly also include configuration like which models and voice settings were used.

## CLI Workflow
1. Ingest a URL or local text file; fetch content and comments (HN/Reddit APIs preferred, scrape fallback).
2. Produce summaries via LLM prompts defined in editable EDN config.
3. Render a narration script and convert each section to audio; merge into a single MP3.
4. Persist episode records (EDN or lightweight DB) and stage transcripts.
5. Publish audio + transcript to Spaces and rebuild the RSS feed.
6. Allow manual checkpoints (ingest-only, generate-only, publish-only) for ad hoc runs.

## Non-Goals / Constraints
- No automated scheduling; runs ad hoc from the CLI.
- Keep bucket/feed private but accessible via direct link; no public indexing.
- Limit complexity: prioritize readability and ease of tweaking prompts/voices.

## Open Questions
- Final choice of local storage format (EDN vs SQLite).
- Voice persona strategy for quotes vs narration.
- Handling of very large comment threads beyond depth/length limits.

