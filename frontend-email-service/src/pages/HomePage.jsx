import React, { useState, useContext, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import HomeMainContent from '../components/HomeMainContent';
import { AppContext } from '../AppContext.jsx';
import { useNavigate } from 'react-router-dom';



const HomePage = () => {

  const { setSharedUserEmail, setAuthToken, sharedUserEmail, authToken } = useContext(AppContext);
  
  const navigate = useNavigate();  

  useEffect(() => {
    const queryParams = new URLSearchParams(window.location.search);
    const token = queryParams.get('token');
    const email = queryParams.get('email');

    if (token && email) {
      setAuthToken(token);
      setSharedUserEmail(email);
      navigate("/home"); // to hide the displayed token and email displaying from the URL
    }
  }, [setAuthToken, setSharedUserEmail]);

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