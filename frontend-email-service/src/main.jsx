import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import SignUpPage from './pages/SignUpPage';
import SignInPage from './pages/SignInPage';
import HomePage from './pages/HomePage';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Router>
      <Routes>
        <Route path="/" element={<SignUpPage />} />
        <Route path="/signin" element={<SignInPage />} />
        <Route path="/home" element={<HomePage />} />
      </Routes>
    </Router>
  </StrictMode>
);
