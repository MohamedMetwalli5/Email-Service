import React from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import SettingsMainContent from '../components/SettingsMainContent';
import { useContext } from "react";
import { AppContext } from '../AppContext.jsx';


const SettingsPage = () => {
  
  const { authToken, sharedUserEmail } = useContext(AppContext);

  if (!authToken || !sharedUserEmail) {
    return <></>;
  }

  return (
    <div className="flex h-screen bg-gray-700">
      <Sidebar />
      <div className="flex flex-col flex-1">
        <Navbar />
        <div className="flex-1 mt-auto p-4">
          <SettingsMainContent />
        </div>
      </div>
    </div> 
  );
};

export default SettingsPage;
