import React, { useState, useContext, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import HomeMainContent from '../components/HomeMainContent';
import { AppContext } from '../AppContext.jsx';
import apiClient from '../api/apiClient';

const HomePage = () => {
  const params = new URLSearchParams(window.location.search);
  const codeFromUrl = params.get('code');

  const {
    setSharedUserEmail,
    setAuthToken,
    setRefreshToken,
    sharedUserEmail,
    authToken,
  } = useContext(AppContext);

  const [exchanging, setExchanging] = useState(!!codeFromUrl);

  // Discord OAuth callback: the backend redirected with ?code=<opaque ticket>.
  // Exchange the ticket for tokens + email via POST /api/v1/auth/exchange so the
  // tokens never live in the URL (history/logs/Referer). The ticket is single-use.
  useEffect(() => {
    if (!codeFromUrl) {
      return;
    }

    let cancelled = false;
    apiClient
      .post('/auth/exchange', { code: codeFromUrl })
      .then((response) => {
        if (cancelled) return;
        const { accessToken, refreshToken, email } = response.data;
        setAuthToken(accessToken);
        setRefreshToken(refreshToken);
        setSharedUserEmail(email);
        window.history.replaceState({}, document.title, '/home');
      })
      .catch(() => {
        if (cancelled) return;
        // Invalid/expired/already-used ticket - send the user to sign-in.
        window.location.href = '/sign-in';
      })
      .finally(() => {
        if (!cancelled) setExchanging(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (exchanging) {
    return <></>;
  }

  if (!sharedUserEmail || !authToken) {
    return <></>;
  }

  return (
    <div className="flex h-full w-full bg-gray-700">
      <Sidebar />
      <div className="flex flex-col flex-1">
        <Navbar />
        <div className="flex-1 mt-auto p-4">
          <HomeMainContent />
        </div>
      </div>
    </div>
  );
};

export default HomePage;