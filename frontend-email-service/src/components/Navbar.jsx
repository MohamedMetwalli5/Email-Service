import React, { useContext, useEffect, useState } from 'react';
import defaultPersonalPhoto from '../assets/defaultPersonalPhoto.png'; // The default profile picture
import { useNavigate } from 'react-router-dom';
import { AppContext } from '../AppContext.jsx';
import apiClient from '../api/apiClient';

const Navbar = () => {
  const { sharedUserEmail, profilePictureVersion } = useContext(AppContext);
  const userEmail = sharedUserEmail;
  const navigate = useNavigate();
  
  const [profilePicture, setProfilePicture] = useState(null);

  const fetchProfilePicture = async () => {
    try {
      const response = await apiClient.get(`/${sharedUserEmail}/profile-picture`, {
        responseType: 'arraybuffer',
      });
   
      const contentType = response.headers['content-type'] || 'image/jpeg';
      const blob = new Blob([response.data], { type: contentType });
      const imageUrl = URL.createObjectURL(blob);
   
      setProfilePicture(imageUrl);
    } catch (error) {
      // 404 when no profile picture uploaded - use default
      setProfilePicture(null);
    }
  };
   
  useEffect(() => {
    if (sharedUserEmail) {
      fetchProfilePicture();
    }

    return () => {
      if (profilePicture) {
        URL.revokeObjectURL(profilePicture);
      }
    };
  }, [sharedUserEmail, profilePictureVersion]);



  return (
    <nav className="top-0 z-50 w-full bg-gray-900 shadow-md">
      <div className="px-4 py-3 lg:px-6">
        <div className="flex items-center justify-between">
          <h1 className="text-lg font-semibold text-blue-300">{userEmail}</h1>
          <button
            className="w-10 h-10 rounded-full border-2 border-blue-500 hover:cursor-pointer overflow-hidden"
            onClick={() => navigate("/settings")}
            aria-label="Settings"
          >
            <img
              className="w-full h-full object-cover"
              src={profilePicture || defaultPersonalPhoto}
              alt="User Avatar"
            />
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;