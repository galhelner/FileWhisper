import os
import PyPDF2
import google.generativeai as genai
from datetime import datetime
from bson import ObjectId
from schemas.upload import UploadModel
from schemas.chat_history import ChatHistoryModel

# Load API key from environment variable
genai.configure(api_key=os.environ.get("GEMINI_API_KEY"))

MODEL_NAME = "gemini-1.5-flash"


async def get_summary(text: str, style: str = "bullets", length: str = "short") -> dict:
    """
    Summarizes the input text in the requested style and length.
    Returns a dict, so it can be JSON serialized directly.
    """

    if style == "bullet":
        style_instruction = (
            "Summarize the following text into multiple concise bullet points. "
            "Each bullet must be on its own line, plain text only (no symbols like *, -, •). "
            "Always provide at least 3 separate bullet points."
        )
    elif style == "paragraph":
        style_instruction = (
            "Summarize the following text in a coherent paragraph. "
            "Do NOT use *, -, • or any other symbols, just plain text lines."
        )
    else:
        style_instruction = "Summarize the following text"

    length_instruction = {
        "short": "Keep each bullet very brief (a few words to one sentence).",
        "medium": "Provide a moderate summary with main points and some details.",
        "long": "Provide a detailed summary including all important points."
    }.get(length, "")

    prompt = f"{style_instruction} {length_instruction}\n\nText:\n{text}"

    model = genai.GenerativeModel(MODEL_NAME)
    response = model.generate_content(prompt)

    summary_text = response.text.strip()

    if style == "bullet":
        lines = [
            line.lstrip("*-• ").strip()
            for line in summary_text.split("\n")
            if line.strip()
        ]
        return lines
    else:
        return summary_text


async def ask_model(db, user_id: str, context_file_id: str, question: str) -> dict:
    """
    Answers a user question using the uploaded file as context and stores conversation in MongoDB.
    """

    # Fetch the uploaded file
    upload_doc = await db["uploads"].find_one({
        "_id": ObjectId(context_file_id),
        "user_id": user_id
    })
    if not upload_doc:
        return {"error": "Upload not found"}

    upload = UploadModel(**upload_doc)
    file_path = upload.file_path

    # Read file content
    text = ""
    if file_path.lower().endswith(".txt"):
        with open(file_path, "r", encoding="utf-8") as f:
            text = f.read()
    elif file_path.lower().endswith(".pdf"):
        with open(file_path, "rb") as f:
            pdf_reader = PyPDF2.PdfReader(f)
            for page in pdf_reader.pages:
                text += page.extract_text() + "\n"
    else:
        text = "Unsupported file type"

    # Load previous conversation if exists
    chat_doc = await db["chat_history"].find_one({"upload_id": context_file_id})
    chat_history_text = chat_doc["history_text"] if chat_doc else ""

    # Build prompt
    prompt = f"""
    You are a helpful assistant. Always answer the user's question clearly and naturally. 

    Instructions:
    1. Use the uploaded file as context whenever possible.
       - If the answer can be derived from the file, start with: "Based on the uploaded file: ..." 
       - If the answer is not in the file, simply answer from general knowledge. You may optionally note: "(This answer is based on general knowledge.)"
    2. Do not respond with "no information" or "the file doesn't contain this." Always give a useful answer.
    3. Be concise, clear, and polite.  

    Context (uploaded file):
    {text}

    Conversation history:
    {chat_history_text}

    User question:
    {question}

    Answer:
    """

    model = genai.GenerativeModel(MODEL_NAME)
    response = model.generate_content(prompt)
    answer = response.text.strip()

    # Update or insert chat history
    if chat_doc:
        new_history = chat_doc["history_text"] + f"\nUser: {question}\nAssistant: {answer}"
        await db["chat_history"].update_one(
            {"_id": chat_doc["_id"]},
            {"$set": {"history_text": new_history}}
        )
    else:
        chat_history = ChatHistoryModel(
            upload_id=context_file_id,
            history_text=f"User: {question}\nAssistant: {answer}"
        )
        await db["chat_history"].insert_one(chat_history.dict())

    return answer
