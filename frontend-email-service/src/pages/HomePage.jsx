import React, { useState, useContext, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import HomeMainContent from '../components/HomeMainContent';
import { AppContext } from '../AppContext.jsx';

const HomePage = () => {
  const params = new URLSearchParams(window.location.search);
  const tokenFromUrl = params.get('token');
  const refreshTokenFromUrl = params.get('refreshToken');
  const emailFromUrl = params.get('email');

  const { setSharedUserEmail, setAuthToken, setRefreshToken, sharedUserEmail, authToken } = useContext(AppContext);

  useEffect(() => {
    if (tokenFromUrl && emailFromUrl) {
      setAuthToken(tokenFromUrl);
      setSharedUserEmail(emailFromUrl);

      if (refreshTokenFromUrl) {
        setRefreshToken(refreshTokenFromUrl);
      }

      window.history.replaceState({}, document.title, '/home');
    }
  }, []);

  if (!(sharedUserEmail || emailFromUrl) || !(authToken || tokenFromUrl)) {
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