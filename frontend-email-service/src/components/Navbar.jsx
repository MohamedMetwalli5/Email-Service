import React from 'react';
import PersonalPhoto from '../assets/PersonalPhoto.png';


const Navbar = () => {

  const userName = "Mohamed"

  return (
    <div className="fixed top-0 z-50 w-full bg-gray-900 shadow-md h-16">
      <nav className="fixed top-0 z-50 w-full bg-gray-900 shadow-md">
        <div className="px-4 py-3 lg:px-6">
          <div className="flex items-center justify-between">
            <h1 className="text-lg font-semibold text-blue-300">{userName}</h1>
            <img
                className="w-10 h-10 rounded-full border-2 border-blue-500"
                src={PersonalPhoto}
                alt="User Avatar"
            />
          </div>
        </div>
      </nav>
    </div>
  );
};

export default Navbar;