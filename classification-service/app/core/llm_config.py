import os

from llama_index.llms.google_genai import GoogleGenAI

# gemini-2.5-flash is on the Gemini free tier (no card required).
# gemini-2.0-flash now returns free_tier limit:0 and must not be used.
llm = GoogleGenAI(
    model="gemini-2.5-flash",
    api_key=os.getenv("GOOGLE_API_KEY"),
    temperature=0,
)