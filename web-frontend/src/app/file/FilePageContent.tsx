"use client";

import { useState, useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import jsPDF from "jspdf";
import Swal from "sweetalert2";

interface ChatMessage {
  sender: "user" | "ai";
  text: string;
  is_html?: boolean;
}

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:5000";

export default function FileDetailsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const fileId = searchParams.get("file_id");
  const filenameParam = searchParams.get("filename") || "";
  const chatEndRef = useRef<HTMLDivElement | null>(null);

  const [filename, setFilename] = useState(filenameParam);
  const [summary, setSummary] = useState<string | string[]>("");
  const [chatHistory, setChatHistory] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState("");
  const [loadingSummary, setLoadingSummary] = useState(false);
  const [loadingChat, setLoadingChat] = useState(false);
  const [lengthOption, setLengthOption] = useState("short");
  const [styleOption, setStyleOption] = useState("bullet");

  useEffect(() => {
    if (chatEndRef.current) {
      chatEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [chatHistory, loadingChat]);

  // Check auth and load chat
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      router.push("/auth");
      return;
    }
    if (fileId) loadChatHistory();
  }, [fileId]);

  const loadChatHistory = async () => {
    setChatHistory([]);
    const token = localStorage.getItem("token");
    if (!token) return;
    try {
      const res = await fetch(
        `${BASE_URL}/chat/history?context_file_id=${fileId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      const data = await res.json();
      if (data.chat_history) setChatHistory(data.chat_history);
    } catch (err) {
      console.error("Chat history error:", err);
    }
  };

  const summarizeFile = async () => {
    if (!fileId) return;
    setLoadingSummary(true);
    setSummary("");
    const token = localStorage.getItem("token");
    if (!token) {
      alert("Please login first");
      return;
    }
    try {
      const res = await fetch(`${BASE_URL}/summarize/file/`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          file_id: fileId,
          style: styleOption,
          length: lengthOption,
        }),
      });
      const data = await res.json();

      if (styleOption === "bullet") {
        setSummary(data.summary || []);
      } else {
        setSummary(data.summary || "");
      }
    } catch (err) {
      setSummary(`Error: ${err}`);
    } finally {
      setLoadingSummary(false);
    }
  };

  const sendChat = async () => {
    if (!question.trim() || !fileId) return;
    const newMsg: ChatMessage = { sender: "user", text: question };
    setChatHistory((prev) => [...prev, newMsg]);
    setQuestion("");
    setLoadingChat(true);
    const token = localStorage.getItem("token");
    if (!token) return;
    try {
      const res = await fetch(`${BASE_URL}/chat/`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ context_file_id: fileId, question }),
      });
      const data = await res.json();
      setChatHistory((prev) => [
        ...prev,
        { sender: "ai", text: data.answer || "No answer." },
      ]);
    } catch (err) {
      setChatHistory((prev) => [
        ...prev,
        { sender: "ai", text: `Error: ${err}` },
      ]);
    } finally {
      setLoadingChat(false);
    }
  };

  const exportSummaryToPDF = (
    summary: string | string[],
    filename = "summary.pdf"
  ) => {
    const doc = new jsPDF();
    const title = "FileWhisper Summary";

    doc.setFontSize(18);
    doc.setFont("helvetica", "bold");
    doc.text(title, 105, 20, { align: "center" });

    doc.setFontSize(12);
    doc.setFont("helvetica", "normal");

    let startY = 40;
    const pageWidth = 190;

    if (Array.isArray(summary)) {
      summary.forEach((item) => {
        const lines = doc.splitTextToSize(`• ${item}`, pageWidth);
        doc.text(lines, 15, startY);
        startY += lines.length * 7;
      });
    } else {
      const lines = doc.splitTextToSize(summary, pageWidth);
      doc.text(lines, 15, startY);
    }

    doc.save(filename);
  };

  const deleteFile = async () => {
    const token = localStorage.getItem("token");
    if (!token) return;
    try {
      const res = await fetch(`${BASE_URL}/files/${fileId}`, { 
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });

    if (!res.ok) {
      alert('Failed to delete the file!');
      return;
    }

    Swal.fire("Success", "File Deleted Successfully", "success").then(() => {
              router.push('/');
            });
    } catch (err) {
      alert('Failed to delete the file!');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-900">
      <main className="flex flex-col w-[80%] mx-auto p-0 shadow-lg bg-white rounded-xl border border-slate-100">
        {/* Header */}
        <header className="relative w-full flex flex-col items-center gap-2 mb-10 p-10 rounded-t-xl bg-gradient-to-r from-blue-600 to-sky-400">
          <button
            className="absolute top-5 left-10 bg-white text-blue-700 px-6 py-2 rounded-lg font-bold shadow hover:bg-slate-100 cursor-pointer"
            onClick={() => router.back()}
          >
            Back
          </button>
          <img
            src="/file-icon-big.png"
            alt="Logo"
            className="w-16 h-16 mb-2 drop-shadow-lg"
          />
          <h1 className="text-4xl font-black tracking-tight text-white mb-1">
            {filename}
          </h1>
          <button
            className="absolute top-5 right-10 bg-white text-red-600 px-6 py-2 rounded-lg font-bold shadow hover:bg-slate-100 cursor-pointer"
            onClick={() => deleteFile()}
          >
            Delete
          </button>
        </header>

        <section className="flex flex-row mb-10 mx-5">
          {/* Summary section */}
          <section className="flex-1 flex flex-col p-5 mr-10 shadow-md rounded-xl border border-slate-200">
            <div className="flex flex-col gap-4 w-full md:w-auto items-center mb-10">
              <h2 className="text-2xl font-bold text-slate-700 mb-4">Summary</h2>
              <div className="flex gap-6 mb-5">
                {/* Style select */}
                <div>
                  <label className="block text-xs font-bold mb-2 text-slate-500">
                    Summary Style
                  </label>
                  <select
                    aria-label="Choose summary style"
                    className="border border-slate-200 rounded-xl px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-300 bg-slate-50 text-slate-700"
                    value={styleOption}
                    onChange={(e) => setStyleOption(e.target.value)}
                  >
                    <option value="bullet">Bullet Points</option>
                    <option value="paragraph">Paragraph</option>
                  </select>
                </div>
                {/* Length select */}
                <div>
                  <label className="block text-xs font-bold mb-2 text-slate-500">
                    Summary Length
                  </label>
                  <select
                    aria-label="Choose summary length"
                    className="border border-slate-200 rounded-xl px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-300 bg-slate-50 text-slate-700"
                    value={lengthOption}
                    onChange={(e) => setLengthOption(e.target.value)}
                  >
                    <option value="short">Short</option>
                    <option value="medium">Medium</option>
                    <option value="long">Long</option>
                  </select>
                </div>
              </div>
              <button
                className="bg-blue-700 text-white w-[50%] px-8 py-2 rounded-xl font-bold shadow-lg hover:bg-blue-800 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-blue-400 cursor-pointer"
                onClick={summarizeFile}
                disabled={loadingSummary}
              >
                {loadingSummary ? "Summarizing..." : "Summarize"}
              </button>
            </div>

            {/* Summary result */}
            <div className="flex-1 mb-8 md:mb-0">
              {(loadingSummary || summary) && (
                <div className="bg-slate-50 rounded-xl p-6 min-h-[60px] border border-slate-100">
                  {loadingSummary ? (
                    <div className="flex flex-col items-center gap-3">
                      <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                      <span className="text-blue-700 font-semibold">
                        Summarizing...
                      </span>
                    </div>
                  ) : Array.isArray(summary) ? (
                    <ul className="list-disc pl-6 space-y-2">
                      {summary.map((item, idx) => (
                        <li
                          key={idx}
                          className="text-slate-800 text-base leading-relaxed"
                        >
                          {item}
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="text-slate-800 text-base whitespace-pre-line leading-relaxed">
                      {summary}
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Export button */}
            {summary &&
              (Array.isArray(summary)
                ? summary.length > 0
                : summary.trim().length > 0) && (
                <button
                  className="flex flex-col items-center bg-purple-600 text-white text-md w-fit self-end mt-10 px-4 py-2 rounded-xl font-bold shadow-lg hover:bg-purple-700 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-purple-400 cursor-pointer"
                  onClick={() =>
                    exportSummaryToPDF(summary, `${filename}-summary.pdf`)
                  }
                >
                  <img
                    src="/pdf-icon.png"
                    alt="PDF"
                    className="w-12 h-12 inline-block mr-2 -mt-1"
                  />
                  Export to PDF
                </button>
              )}
          </section>

          {/* Chat section */}
          <section className="flex-1 flex flex-col p-5 items-center shadow-md rounded-xl border border-slate-200">
            <div className="flex-1 flex flex-col p-5">
              <h2 className="text-2xl text-center font-bold text-slate-700 mb-4">
                Chat with AI about this file
              </h2>

              {/* Chat container */}
              <div className="flex-1 border border-slate-100 rounded-xl p-4 bg-slate-50 mb-5 max-h-100 w-full overflow-y-auto flex flex-col gap-3">
                {chatHistory.length === 0 && !loadingChat && (
                  <div className="text-gray-400 text-center">
                    No chat history yet.
                  </div>
                )}

                {chatHistory.map((msg, idx) => {
                  const sender = msg.sender === "user" ? "user" : "ai";
                  return (
                    <div
                      key={idx}
                      className={`flex items-start ${
                        sender === "user" ? "justify-end" : "justify-start"
                      }`}
                    >
                      {sender === "ai" && (
                        <img
                          src="/gemini-icon.png"
                          alt="AI"
                          className="w-8 h-8 rounded-full flex-shrink-0 mr-2 object-cover"
                        />
                      )}
                      <div
                        className={`px-5 py-3 rounded-2xl max-w-[70%] break-words shadow ${
                          sender === "user"
                            ? "bg-blue-100 text-blue-900"
                            : "bg-gray-100 text-gray-800"
                        }`}
                        dangerouslySetInnerHTML={
                          msg.is_html ? { __html: msg.text } : undefined
                        }
                      >
                        {!msg.is_html ? msg.text : null}
                      </div>
                      {sender === "user" && (
                        <img
                          src="/user-icon.png"
                          alt="User"
                          className="w-8 h-8 rounded-full flex-shrink-0 ml-2 object-cover"
                        />
                      )}
                    </div>
                  );
                })}

                {loadingChat && (
                  <div className="flex items-start gap-2">
                    <div className="w-8 h-8 bg-gray-400 rounded-full animate-pulse flex-shrink-0"></div>
                    <div className="px-4 py-2 bg-gray-100 rounded-2xl text-gray-700 shadow">
                      AI is typing...
                    </div>
                  </div>
                )}
                <div ref={chatEndRef}/>
              </div>

              {/* Input form */}
              <form
                className="flex gap-3 items-center"
                onSubmit={(e) => {
                  e.preventDefault();
                  sendChat();
                }}
              >
                <textarea
                  rows={1}
                  className="flex-1 border border-slate-200 rounded-xl px-5 py-5 focus:outline-none focus:ring-2 focus:ring-green-400 bg-white text-slate-800 shadow resize-none overflow-hidden leading-4"
                  placeholder="Ask something about this file..."
                  value={question}
                  onChange={(e) => {
                    setQuestion(e.target.value);
                    e.target.style.height = "auto";
                    e.target.style.height = e.target.scrollHeight + "px";
                  }}
                  disabled={loadingChat}
                />
                <button
                  type="submit"
                  className="bg-green-600 text-white h-14 px-8 py-2 rounded-xl font-bold shadow-lg hover:bg-green-700 transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-green-400 cursor-pointer"
                  disabled={loadingChat || !question.trim()}
                >
                  Send
                </button>
              </form>
            </div>
          </section>
        </section>
      </main>
    </div>
  );
}