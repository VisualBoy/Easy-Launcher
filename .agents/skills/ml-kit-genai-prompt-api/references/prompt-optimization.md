This skill provides guidance on how to optimize prompts for use with the ML Kit Prompt API.

## Guidelines & Rules

Apply these core rules:

- **Include examples for in-context learning:** Include examples of the desired model output.
- **Make prompts concise:** Remove duplicate or repeated instructions. Do not include conversational filler such as hello, please, do this, help me with, etc.
- **Use paired XML/HTML delimiters:** Denote dynamic inputs (for example `<email>[content]</email>`) to clearly isolate user data from prompt instructions.
- **Use the Structured Output API:** If the output requires parsing responses into certain formats, use Structured Output API. Return the `@Generable` typed object from the function signature instead of a string or JSON string.
- **Keep output short:** Add output constraints such as word count, character count, or bullet limits.
- **Use system instructions:** For short instructions that define how a model should behave, and use prefix caching for prompts over 200 words.
