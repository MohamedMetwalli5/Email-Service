import React, { useContext, useEffect, useState } from 'react';
import defaultPersonalPhoto from '../assets/defaultPersonalPhoto.png'; // The default profile picture
import { useNavigate } from 'react-router-dom';
import { AppContext } from '../AppContext.jsx';
import axios from 'axios';

const Navbar = () => {
  const { sharedUserEmail, authToken } = useContext(AppContext);
  const userEmail = sharedUserEmail;
  const navigate = useNavigate();
  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;
  
  const [profilePicture, setProfilePicture] = useState(null);

  const fetchProfilePicture = async () => {
    try {
      const response = await axios.get(`${backendUrl}/${sharedUserEmail}/profile-picture`, {
        responseType: 'arraybuffer',
        headers: {
          'Authorization': `Bearer ${authToken}`,
        }
      });
  
      const contentType = response.headers['content-type'] || 'image/png';
      const blob = new Blob([response.data], { type: contentType });
      const imageUrl = URL.createObjectURL(blob);
  
      setProfilePicture(imageUrl);
    } catch (error) {
      console.error('Error fetching profile picture:', error);
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
  }, [sharedUserEmail]);



  return (
    <nav className="top-0 z-50 w-full bg-gray-900 shadow-md">
      <div className="px-4 py-3 lg:px-6">
        <div className="flex items-center justify-between">
          <h1 className="text-lg font-semibold text-blue-300">{userEmail}</h1>
          <img
            className="w-10 h-10 rounded-full border-2 border-blue-500 hover:cursor-pointer"
            src={profilePicture || defaultPersonalPhoto}
            alt="User Avatar"
            onClick={() => navigate("/settings")}
          />
        </div>
      </div>
    </nav>
  );
};

export default Navbar;