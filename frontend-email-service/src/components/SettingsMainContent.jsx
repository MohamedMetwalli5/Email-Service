import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';


const SettingsMainContent = () => {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');

  const navigate = useNavigate();

  const handlePasswordChange = () => {
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match!');
    } else {
      console.log('Password changed');
    }
  };

  const handleDeleteAccount = () => {
    console.log('Account deleted');
    navigate("/");
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