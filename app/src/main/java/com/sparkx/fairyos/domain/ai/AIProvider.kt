package com.sparkx.fairyos.domain.ai

enum class AIProvider(val displayName: String, val endpoint: String, val model: String) {
    OPENAI("OpenAI (GPT-4o mini)", "https://api.openai.com/v1/chat/completions", "gpt-4o-mini"),
    GROK("Grok (xAI)", "https://api.x.ai/v1/chat/completions", "grok-beta"),
    GEMINI("Gemini (Google)", "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent", "gemini-1.5-flash"),
    CLAUDE("Claude (Anthropic)", "https://api.anthropic.com/v1/messages", "claude-3-haiku-20240307"),
    LOCAL("Local / Offline (future)", "", "")
}