import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'sonner';
import NavBar from './components/layout/NavBar';
import TermSheetReview from './pages/TermSheetReview';
import NewDealIngestion from './pages/NewDealIngestion';
import OcrReviewPage from './pages/OcrReviewPage';
import ComplianceReview from './pages/ComplianceReview';
import { ManualReviewBanner } from './components/notifications/ManualReviewBanner';
import './index.css';


ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      {/* Fixed toast overlay — no layout impact */}
      <ManualReviewBanner />

      {/* Sub-Story 3.3a: Approve/Reject confirmation + audit-failure toasts */}
      <Toaster richColors position="top-right" />

      {/* Persistent navigation — sticky, 56px (h-14) */}
      <NavBar />

      <Routes>
        <Route path="/" element={<NewDealIngestion />} />
        <Route path="/term-sheet-review" element={<TermSheetReview />} />
        <Route path="/ocr-review" element={<OcrReviewPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
        <Route path="/compliance-review" element={<ComplianceReview />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>,
);
