"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import Swal from "sweetalert2";

export default function AuthPage() {
  const [loading, setLoading] = useState(false);
  const [isLogin, setIsLogin] = useState(true);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const router = useRouter();

  const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:5000";
  console.log(BASE_URL);
  const maxFullNameLength = 30;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isLogin) {
      await login();
    } else {
      await register();
    }
  };

  const login = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${BASE_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      const data = await res.json();
      if (data.token) {
        localStorage.setItem("token", data.token);
        localStorage.setItem("full_name", data.full_name);
        router.replace("/");
      } else {
        Swal.fire("Login Failed", data.error || "Invalid credentials", "error");
      }
    } catch (err) {
      Swal.fire("Error", "An error occurred during login", "error");
    } finally {
      setLoading(false);
    }
  };

  const register = async () => {
    setLoading(true);
    if (fullName.length > maxFullNameLength) {
      Swal.fire(
        "Invalid Full Name",
        `Full name must be less than ${maxFullNameLength} characters.`,
        "error"
      );
      return;
    }
    try {
      const res = await fetch(`${BASE_URL}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ full_name: fullName.trim(), email, password }),
      });
      const data = await res.json();
      if (!res.ok) {
        Swal.fire("Registration Failed", data.error, "error");
      } else {
        Swal.fire("Success", "Registration successful", "success").then(() => {
          window.location.reload();
        });
      }
    } catch (err) {
      Swal.fire("Error", "An error occurred during registration", "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-[#E0E0E0]">
      <div
        className={`relative w-[800px] h-[500px] flex overflow-hidden rounded-2xl shadow-lg transition-all duration-700`}
      >
        {/* Form Box */}
        <div
          className={`w-1/2 flex flex-col justify-center items-center p-6 bg-white transition-transform duration-700 ${isLogin ? "translate-x-0" : "translate-x-full"
            }`}
        >
          <h2 className="text-2xl font-bold mb-6">
            {isLogin ? "Login" : "Register"}
          </h2>
          <form
            onSubmit={handleSubmit}
            className="flex flex-col w-full items-center"
          >
            {!isLogin && (
              <div className="w-4/5 mb-2">
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Full Name"
                  className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:border-blue-500"
                  maxLength={maxFullNameLength}
                  required={!isLogin}
                />
                {fullName.length > maxFullNameLength && (
                  <p className="text-red-500 text-sm mt-1">
                    Full name is limited to {maxFullNameLength} characters!
                  </p>
                )}
              </div>
            )}
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Email"
              required
              className="w-4/5 px-3 py-2 border rounded-lg mb-3 focus:outline-none focus:border-blue-500"
            />
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password"
              required
              minLength={6}
              className="w-4/5 px-3 py-2 border rounded-lg mb-10 focus:outline-none focus:border-blue-500"
            />
            <button
              type="submit"
              className="w-4/5 flex items-center justify-center bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600 transition cursor-pointer"
            >
              {loading ? (
                <svg
                  className="animate-spin h-5 w-5 text-white"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
                  ></path>
                </svg>
              ) : (
                <>{isLogin ? "Login" : "Register"}</>
              )}
            </button>
          </form>
        </div>

        {/* Info Box */}
        <div
          className={`w-1/2 flex flex-col justify-center items-center bg-gradient-to-r from-blue-600 to-sky-400 transition-transform duration-700 ${isLogin ? "translate-x-0" : "-translate-x-full"
            }`}
        >
          <img
            src="/file_whisper_logo.png"
            alt="Logo"
            className="w-40 mb-10"
          />
          <h1 className="text-2xl text-white font-semibold mb-10">
            {isLogin ? "Welcome Back!" : "Join Us!"}
          </h1>
          <button
            onClick={() => setIsLogin(!isLogin)}
            className="px-6 py-2 bg-[#a885dc] text-white rounded-lg shadow hover:bg-[#9361df] transition cursor-pointer"
          >
            {isLogin ? "Register" : "Login"}
          </button>
        </div>
      </div>
    </div>
  );
}