"use client";

import React, { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:5000";

// Types
interface FileItem {
  id: string;
  filename: string;
  uploaded_at?: string | null;
}

export default function FilesPage() {
  const router = useRouter();

  // ----------------- State -----------------
  const [authorized, setAuthorized] = useState<boolean | null>(null);
  const [files, setFiles] = useState<FileItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileValid, setFileValid] = useState<"success" | "error" | "">("");
  const [uploading, setUploading] = useState(false);
  const [userPopupOpen, setUserPopupOpen] = useState(false);
  const modalRef = useRef<HTMLDivElement | null>(null);
  const avatarBtnRef = useRef<HTMLButtonElement | null>(null);
  const [popupPos, setPopupPos] = useState<{ top: number; left: number } | null>(null);

  const userName =
    typeof window !== "undefined" ? localStorage.getItem("full_name") || "User" : "User";

  // ----------------- Auth -----------------
  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
      router.replace("/auth");
    } else {
      setAuthorized(true);
    }
  }, [router]);

  // ----------------- Fetch files -----------------
  const fetchFiles = async () => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      setIsLoading(true);
      const res = await fetch(`${BASE_URL}/files/`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      setFiles(Array.isArray(data.files) ? data.files : []);
    } catch (err) {
      console.error("Error loading files:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (authorized) fetchFiles();
  }, [authorized]);

  // ----------------- Modal backdrop -----------------
  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (e.target instanceof Node && modalRef.current && e.target === modalRef.current) {
        handleCloseModal();
      }
    }
    if (isModalOpen) document.addEventListener("click", onClick);
    return () => document.removeEventListener("click", onClick);
  }, [isModalOpen]);

  const resetFileInput = () => {
    setSelectedFile(null);
    setFileValid("");
  };

  const handleOpenModal = () => {
    resetFileInput();
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    resetFileInput();
  };

  // ----------------- File selection -----------------
  const onFileChange: React.ChangeEventHandler<HTMLInputElement> = (e) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
    if (!file) {
      setFileValid("");
      return;
    }
    const type = file.type;
    if (type === "text/plain" || type === "application/pdf") {
      setFileValid("success");
    } else {
      setFileValid("error");
    }
  };

  // ----------------- Logout -----------------
  const logout = async () => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      await fetch(`${BASE_URL}/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch (err) {
      console.error("Logout error:", err);
    } finally {
      localStorage.removeItem("token");
      localStorage.removeItem("full_name");
      router.replace("/auth");
    }
  };

  // ----------------- Open file -----------------
  const openFilePage = (fileId: string, filename: string) => {
    router.push(`/file?file_id=${fileId}&filename=${encodeURIComponent(filename)}`);
  };

  // ----------------- Upload -----------------
  const uploadFile = async () => {
    if (!selectedFile || fileValid === "error") return;

    const token = localStorage.getItem("token");
    if (!token) {
      alert("Please login first!");
      return;
    }

    setUploading(true);

    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
      const res = await fetch(`${BASE_URL}/upload/`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });

      const data = await res.json();

      handleCloseModal();
      await fetchFiles(); // Refresh files after upload

      if (data.file_id) {
        localStorage.setItem("lastFileId", data.file_id as string);
      }
    } catch (err) {
      console.error(err);
      setFileValid("error");
    } finally {
      setUploading(false);
    }
  };

  // ----------------- Render -----------------
  if (authorized === null) return null;

  return (
    <div className="min-h-screen bg-gradient-to-tr from-slate-50 to-slate-200 text-slate-900 flex">
      <div className="w-full mx-10 my-10 bg-white rounded-3xl shadow-2xl overflow-hidden flex flex-col">
        {/* Header */}
        <header className="flex flex-row items-center bg-gradient-to-r from-blue-600 to-sky-400 py-8 px-4 relative">
          <img src="/file_whisper_logo.png" alt="Logo" className="w-23 h-16 mr-5 drop-shadow-lg" />
          <h1 className="flex items-center text-3xl font-extrabold text-white tracking-tight">
            <span className="text-[#38bdf8]">File</span>
            <span className="text-[#9f69f0]">Whisper</span>
          </h1>

          <div className="flex items-center gap-2 ml-auto">
            <button
              ref={avatarBtnRef}
              onClick={() => {
                if (!userPopupOpen && avatarBtnRef.current) {
                  const rect = avatarBtnRef.current.getBoundingClientRect();
                  setPopupPos({
                    top: rect.bottom + window.scrollY + 8,
                    left: rect.right + window.scrollX - 192,
                  });
                }
                setUserPopupOpen((v) => !v);
              }}
              className="w-15 h-15 rounded-full bg-white/80 hover:bg-white border-2 border-sky-200 flex items-center justify-center shadow-md focus:outline-none focus:ring-2 focus:ring-blue-400 transition cursor-pointer"
              aria-label="User menu"
            >
              <svg
                className="w-7 h-7 text-blue-700"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
                viewBox="0 0 24 24"
              >
                <circle cx="12" cy="8" r="4" />
                <path d="M4 20c0-2.5 3.5-4 8-4s8 1.5 8 4" />
              </svg>
            </button>
          </div>
        </header>

        {/* User popup */}
        {userPopupOpen && popupPos && (
          <>
            <div className="fixed inset-0 z-40" onClick={() => setUserPopupOpen(false)} />
            <div
              className="fixed z-50 w-48 bg-white rounded-xl shadow-xl border border-slate-200 animate-fadeIn"
              style={{ top: popupPos.top, left: popupPos.left }}
            >
              <div className="px-5 py-4 flex flex-col items-center">
                <div className="w-14 h-14 rounded-full bg-sky-100 flex items-center justify-center mb-2">
                  <svg
                    className="w-8 h-8 text-blue-700"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth={2}
                    viewBox="0 0 24 24"
                  >
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c0-2.5 3.5-4 8-4s8 1.5 8 4" />
                  </svg>
                </div>
                <div className="font-semibold text-slate-800 mb-3 text-center">{userName}</div>
                <button
                  onClick={logout}
                  className="w-full bg-red-600 hover:bg-red-700 text-white rounded-md px-4 py-2 text-sm font-medium shadow-md focus:outline-none focus:ring-2 focus:ring-red-400 cursor-pointer"
                >
                  Logout
                </button>
              </div>
            </div>
          </>
        )}

        {/* Main */}
        <main className="p-6 md:p-8 flex-1 flex flex-col relative items-center">
          <h1 className="text-2xl font-bold mb-3">My Files</h1>
          <section className="flex-1 w-full overflow-auto p-5">
            {isLoading ? (
              <div className="flex items-center justify-center h-24">
                <div className="w-12 h-12 border-4 border-slate-200 border-t-blue-600 rounded-full animate-spin" />
              </div>
            ) : files.length === 0 ? (
              <div className="text-center text-slate-400 py-10 text-lg font-medium flex flex-col items-center gap-2">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="w-10 h-10 text-slate-300 mb-1"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                No files uploaded yet.
              </div>
            ) : (
              <div className="grid justify-center gap-5 [grid-template-columns:repeat(auto-fit,minmax(240px,max-content))]">
                {files.map((file) => {
                  const uploaded = file.uploaded_at ? new Date(file.uploaded_at).toLocaleString() : "";
                  return (
                    <div
                      key={file.id}
                      onClick={() => openFilePage(file.id, file.filename)}
                      className="bg-gradient-to-br from-sky-100 to-blue-50 border border-slate-200 rounded-2xl shadow-md px-8 pt-7 pb-5 flex flex-col items-center transition hover:-translate-y-1 hover:shadow-xl cursor-pointer min-w-[240px] max-w-[340px] group focus:outline-none focus:ring-2 focus:ring-blue-400"
                      tabIndex={0}
                    >
                      <img src="/file-icon.png" alt="File icon" className="w-8 h-8 object-contain mb-2" />
                      <div
                        className="text-slate-800 font-semibold text-[1.08rem] mb-1 line-clamp-2 text-center"
                        title={file.filename}
                      >
                        {file.filename}
                      </div>
                      <div className="text-slate-500 text-xs">{uploaded}</div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          <button
            className="absolute flex justify-center items-center bottom-5 right-5 w-18 h-18 bg-gradient-to-r from-blue-600 to-sky-500 hover:from-blue-700 hover:to-sky-600 text-white rounded-lg shadow-md transition focus:outline-none focus:ring-2 focus:ring-blue-400 cursor-pointer"
            onClick={handleOpenModal}
          >
            <img src="/upload_file_icon.png" alt="Upload File" className="w-13 h-13"/>
          </button>
        </main>
      </div>

      {/* Modal */}
      {isModalOpen && (
        <div
          ref={modalRef}
          className="fixed inset-0 z-50 bg-slate-900/30 backdrop-blur-[2px] flex items-center justify-center animate-fadeIn"
        >
          <div className="relative bg-white border border-slate-200 rounded-3xl shadow-2xl px-10 pt-12 pb-8 min-w-[340px] max-w-[92vw] mx-4 animate-scaleIn">
            <button
              aria-label="Close"
              onClick={handleCloseModal}
              className="absolute top-4 right-5 text-slate-400 hover:text-red-500 text-3xl leading-none focus:outline-none"
            >
              &times;
            </button>
            <h2 className="text-2xl font-bold text-slate-800 mb-5 text-center">Upload a File</h2>

            <label
              htmlFor="fileInput"
              className={`w-full flex items-center justify-center gap-3 bg-slate-100 text-sky-900 rounded-lg px-5 py-4 text-base font-medium border-2 transition cursor-pointer ${
                fileValid === "error"
                  ? "border-red-400 bg-red-50"
                  : fileValid === "success"
                  ? "border-green-400 bg-green-50"
                  : "border-sky-200 hover:bg-sky-100"
              }`}
            >
              <span className="inline-flex items-center gap-2">
                {selectedFile ? selectedFile.name : "Choose a file…"}
              </span>
              <input
                id="fileInput"
                type="file"
                accept=".txt,.pdf"
                className="hidden"
                onChange={onFileChange}
                disabled={uploading}
              />
            </label>
            {fileValid === "error" && (
              <div className="text-red-500 text-sm mt-2 text-center">
                Please select a .txt or .pdf file.
              </div>
            )}

            <button
              onClick={uploadFile}
              disabled={uploading || fileValid === "error" || !selectedFile}
              className={`mt-6 w-full bg-gradient-to-r from-blue-600 to-sky-500 hover:from-blue-700 hover:to-sky-600 text-white rounded-lg px-6 py-2 text-base font-semibold shadow-md transition focus:outline-none focus:ring-2 focus:ring-blue-400 flex items-center justify-center gap-2 ${
                uploading ? "opacity-60 cursor-not-allowed" : ""
              }`}
            >
              {uploading ? "Uploading…" : "Upload"}
            </button>
          </div>
        </div>
      )}

      <style>{`
        @keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
        .animate-fadeIn { animation: fadeIn .25s ease-out both }
        @keyframes scaleIn { 0% { transform: translateY(14px) scale(.98); opacity: 0 } 100% { transform: translateY(0) scale(1); opacity: 1 } }
        .animate-scaleIn { animation: scaleIn .28s cubic-bezier(.4,1.4,.6,1) both }
      `}</style>
    </div>
  );
}