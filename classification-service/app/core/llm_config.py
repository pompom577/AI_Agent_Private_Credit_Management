import os

from llama_index.llms.google_genai import GoogleGenAI

# gemini-2.0-flash is on the Gemini free tier (no card required).
# Other free options: gemini-1.5-flash, gemini-2.5-flash.
llm = GoogleGenAI(
    model="gemini-2.0-flash",
    api_key=os.getenv("GOOGLE_API_KEY"),
    temperature=0,
)