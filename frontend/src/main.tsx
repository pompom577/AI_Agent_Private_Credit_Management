import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import TermSheetReview from './pages/TermSheetReview';
import NewDealIngestion from './pages/NewDealIngestion';
import OcrReviewPage from './pages/OcrReviewPage';
import { ManualReviewBanner } from './components/notifications/ManualReviewBanner';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <ManualReviewBanner />

      <Routes>
        <Route path="/" element={<NewDealIngestion />} />

        <Route path="/term-sheet-review" element={<TermSheetReview />} />

        <Route path="/ocr-review" element={<OcrReviewPage />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>,
);
