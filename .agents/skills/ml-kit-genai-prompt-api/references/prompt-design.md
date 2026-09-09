When using Prompt API, there are specific strategies you can use to tailor your prompts and receive optimal results. This page describes best practices for formatting prompts for Gemini Nano.

## Prompt design best practices

- **Provide examples for in-context learning:** Add well-distributed examples to your prompt to show Gemini Nano the kind of result you expect.
- **Be concise:** Verbose preambles with repeated instructions can produce suboptimal results. Keep your prompt focused and to-the-point.
- **Structure prompts:** Define instructions, constraints, and examples clearly.
- **Keep output short:** LLM inference speeds are heavily dependent on output length. Carefully consider generating the shortest possible output for your use case. Use Structured Output API where appropriate.
- **Add delimiters:** Use delimiters like `<background_information>`, `<instruction>`, and `##` to create clear separation between different parts of your prompt.
- **Prefer simple logic and focused tasks:** Break up complex tasks into smaller focused Gemini Nano calls chained in code.
- **Use lower temperature values for deterministic tasks:** For tasks such as entity extraction or translation, consider starting with temperature `0.2`.
