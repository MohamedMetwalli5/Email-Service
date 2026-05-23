import React, { useState, useContext, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import HomeMainContent from '../components/HomeMainContent';
import { AppContext } from '../AppContext.jsx';

// Read refreshToken from Discord OAuth redirect; use replaceState instead of navigate

const HomePage = () => {

  const { setSharedUserEmail, setAuthToken, setRefreshToken, sharedUserEmail, authToken } = useContext(AppContext);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token        = params.get('token');
    const refreshToken = params.get('refreshToken');
    const email        = params.get('email');

    if (token && email) {
      setAuthToken(token);
      setSharedUserEmail(email);

      if (refreshToken) {
        setRefreshToken(refreshToken);
      }

      // Strip query params from browser URL (UX: don't leave tokens in address bar)
      window.history.replaceState({}, document.title, '/home');
    }
  }, []);

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