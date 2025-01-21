import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from "axios";
import { AppContext } from '../AppContext.jsx';


const SettingsMainContent = () => {
  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;
  
  const { authToken, sharedUserEmail } = useContext(AppContext);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  
  const navigate = useNavigate();

  const handlePasswordChange = async () => {
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    }else if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long!');
      return;
    }
    
    try {
      const response = await axios.put(`${backendUrl}/changepassword`, 
        { newPassword, email: sharedUserEmail }, 
        {
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        }
      );
      console.log(response.data);
      alert('Password is changed successfully!');
    } catch (error) {
      console.error(error.response?.data || error.message);
      setError('Failed to change password.');
    }
  };

  const handleDeleteAccount = async () => {
    try {
      const response = await axios.post(`${backendUrl}/deleteaccount`, { email: sharedUserEmail }, {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });
      console.log(response.data);
      navigate("/");
    } catch (error) {
      console.error(error.response?.data || error.message);
    }
  };

  return (
    <div className="flex gap-1 bg-gray-800 rounded-lg shadow-md h-full w-full">
      <div className="flex flex-col w-full bg-gray-900 p-4 rounded-lg">
        <h1 className="text-2xl font-semibold text-blue-400 mb-4">Settings</h1>
        
        <div className="mb-6">
          <h2 className="text-lg text-gray-200 mb-2">Change Password</h2>
          <input
            type="password"
            placeholder="New Password"
            className="p-2 w-full mb-4 rounded-lg bg-gray-700 text-white"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
          <input
            type="password"
            placeholder="Confirm New Password"
            className="p-2 w-full mb-4 rounded-lg bg-gray-700 text-white"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
          {error && <p className="text-red-500 mb-4">{error}</p>}
          <button
            onClick={handlePasswordChange}
            disabled={newPassword !== confirmPassword || error}
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition-all duration-300"
          >
            Change Password
          </button>
        </div>

        <div>
          <h2 className="text-lg text-gray-200 mb-2">Delete Account</h2>
          <button
            onClick={handleDeleteAccount}
            className="w-full bg-red-600 text-white py-2 rounded-lg hover:bg-red-700 transition-all duration-300"
          >
            Delete Account
          </button>
        </div>
      </div>
    </div>
  );
};

export default SettingsMainContent;