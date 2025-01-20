import React from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import MainContent from '../components/MainContent';


const HomePage = () => {
  return (
    <div className="flex h-screen bg-gray-700">
      <Sidebar />
      <div className="flex flex-col flex-1">
        <Navbar />
        <div className="flex-1 mt-auto p-4">
          <MainContent />
        </div>
      </div>
    </div>
  );
};

export default HomePage;