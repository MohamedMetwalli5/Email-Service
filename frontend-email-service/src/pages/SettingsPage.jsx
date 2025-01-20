import React from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import SettingsMainContent from '../components/SettingsMainContent';


const SettingsPage = () => {

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
