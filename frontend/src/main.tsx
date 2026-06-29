import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import NavBar from './components/layout/NavBar';
import TermSheetReview from './pages/TermSheetReview';
import NewDealIngestion from './pages/NewDealIngestion';
import OcrReviewPage from './pages/OcrReviewPage';
import { ManualReviewBanner } from './components/notifications/ManualReviewBanner';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      {/* Fixed toast overlay — no layout impact */}
      <ManualReviewBanner />

      {/* Persistent navigation — sticky, 56px (h-14) */}
      <NavBar />

      <Routes>
        <Route path="/" element={<NewDealIngestion />} />
        <Route path="/term-sheet-review" element={<TermSheetReview />} />
        <Route path="/ocr-review" element={<OcrReviewPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>,
);
