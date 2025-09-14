"use client";

import { Suspense } from "react";
import FileDetailsContent from "./FilePageContent";

export default function FileDetailsPage() {
  return (
    <Suspense fallback={<div>Loading file details...</div>}>
      <FileDetailsContent />
    </Suspense>
  );
}