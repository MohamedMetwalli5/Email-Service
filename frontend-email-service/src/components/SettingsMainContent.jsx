import React, { useState, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from "axios";
import { AppContext } from '../AppContext.jsx';
import { useTranslation } from 'react-i18next';
import hash from 'hash.js';

const SettingsMainContent = () => {
  
  const { t } = useTranslation();
  
  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;
  
  const { authToken, sharedUserEmail, setSharedUserLanguage, sharedUserLanguage } = useContext(AppContext);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [profilePicture, setProfilePicture] = useState(null);
  const [file, setFile] = useState(null);
  const [fileSizeError, setFileSizeError] = useState('');

  const navigate = useNavigate();
  
  const handleHashPassword = (password) => {
    return hash.sha256().update(password).digest('hex');
  };

  const handlePasswordChange = async () => {
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    } else if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long!');
      return;
    }
    const hashedPassword = handleHashPassword(newPassword);
    try {
      const response = await axios.put(`${backendUrl}/change-password`, { 
        newPassword: hashedPassword, 
        email: sharedUserEmail 
      }, {
        headers: { Authorization: `Bearer ${authToken}` }
      });
      alert('Password changed successfully!');
      setNewPassword('');
    } catch (err) {
      console.error("Password change error:", err.response?.data);
      
      const backendErrors = err.response?.data?.errors;
      if (Array.isArray(backendErrors)) {
          setError(backendErrors.join(", ")); 
      } else {
          setError(err.response?.data?.message || 'Failed to change password.');
      }
    }
  };

  const handleDeleteAccount = async () => {
    try {
      const response = await axios.delete(`${backendUrl}/delete-account`, { 
        data: { email: sharedUserEmail }, 
      headers: { Authorization: `Bearer ${authToken}` },
      });
      console.log(response.data);
      navigate("/");
    } catch (error) {
      console.error("Account deletion error:", error.response?.data || error.message);
      
      const backendErrors = error.response?.data?.errors;
      if (Array.isArray(backendErrors)) {
        setError(`Delete failed: ${backendErrors.join(", ")}`);
      } else {
        setError(error.response?.data?.message || "Failed to delete account.");
      }
    }
  };

  const handleLanguageChange = async (language) => {
    try {
      await axios.put(`${backendUrl}/update-language`, 
        { language, email: sharedUserEmail }, 
        { headers: { Authorization: `Bearer ${authToken}` } }
      );
      setSharedUserLanguage(language);
    } catch (error) {
      console.error("Language update error:", error.response?.data);
      
      const backendErrors = error.response?.data?.errors;
      if (Array.isArray(backendErrors)) {
        setError(`Language error: ${backendErrors.join(", ")}`);
      } else {
        alert(error.response?.data?.message || "Failed to update language.");
      }
    }
  };

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    const maxSizeInBytes = 5 * 1024 * 1024; // 5MB
    
    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg'];

    if (selectedFile) {
      if (!allowedTypes.includes(selectedFile.type)) {
        setFileSizeError('Invalid file type. Please upload a PNG or JPEG image.');
        setFile(null);
        e.target.value = null;
        return;
      }

      if (selectedFile.size > maxSizeInBytes) {
        setFileSizeError('File size exceeds 5 MB. Please upload a smaller file.');
        setFile(null);
        e.target.value = null;
      } else {
        setFileSizeError('');
        setFile(selectedFile);
      }
    }
  };

  const handleUploadProfilePicture = async () => {
    if (!file) {
      alert('Please select a file to upload.');
      return;
    }

    const reader = new FileReader();
    reader.onloadend = async () => {
      const imageBytes = new Uint8Array(reader.result);
      try {
        await axios.post(`${backendUrl}/${sharedUserEmail}/profile-picture`, imageBytes, {
          headers: {
            'Content-Type': 'application/octet-stream',
            Authorization: `Bearer ${authToken}`
          }
        });
        alert('Profile picture uploaded successfully!');
        window.location.reload();
      } catch (error) {
        console.error('Error uploading profile picture:', error.response?.data);

        const backendErrors = error.response?.data?.errors;
        if (Array.isArray(backendErrors)) {
          alert(`Upload failed:\n${backendErrors.join('\n')}`);
        } else {
          alert(error.response?.data?.message || 'Error uploading profile picture. Please try again.');
        }
      }
    };
    reader.readAsArrayBuffer(file);
  };

  
  return (
    <div className="flex gap-1 bg-gray-800 rounded-lg shadow-md h-full w-full text-sm md:text-lg">
      <div className="flex flex-col w-full bg-gray-900 p-4 rounded-lg">
        <h1 className="text-2xl font-semibold text-blue-400 mb-4">{t('SETTINGS')}</h1>

        <div className="mb-6">
          <h2 className="text-lg text-gray-200 mb-2">{t('PROFILE_PICTURE')} (.png)</h2>
          {profilePicture && <img src={profilePicture} alt="Profile" className="w-32 h-32 rounded-full mb-2" />}
          <input 
            type="file" 
            accept="image/png" 
            onChange={handleFileChange} 
            className="p-2 w-full mb-4 rounded-lg bg-gray-700 text-white" 
          />
          {fileSizeError && <p className="text-red-500">{fileSizeError}</p>}
          <button 
            onClick={handleUploadProfilePicture} 
            className="w-full bg-blue-600 text-white p-2 rounded-lg hover:bg-blue-700 transition-all duration-300"
          >
            {t('UPLOAD_PROFILE_PICTURE')}
          </button>
        </div>
        
        {sharedUserEmail.endsWith("@seamail.com") && (
          <div className="mb-6">
            <h2 className="text-lg text-gray-200 mb-2">{t('CHANGE_PASSWORD')}</h2>
            <input
              type="password"
              placeholder={t('NEW_PASSWORD')}
              className="p-2 w-full mb-4 rounded-lg bg-gray-700 text-white"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
            <input
              type="password"
              placeholder={t('CONFIRM_NEW_PASSWORD')}
              className="p-2 w-full mb-4 rounded-lg bg-gray-700 text-white"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
            {error && <p className="text-red-500 mb-4">{error}</p>}
            <button
              onClick={handlePasswordChange}
              disabled={newPassword !== confirmPassword || error}
              className="w-full bg-blue-600 text-white p-2 rounded-lg hover:bg-blue-700 transition-all duration-300"
            >
              {t('CHANGE_PASSWORD')}
            </button>
          </div>
        )}

        <div className="mb-6">
          <h2 className="text-lg text-gray-200 mb-2">{t('LANGUAGE')}</h2>
          <select
            value={sharedUserLanguage}
            onChange={(e) => handleLanguageChange(e.target.value)}
            className="p-2 w-full mb-4 rounded-lg bg-gray-700 text-white cursor-pointer"
          >
            <option value="English">English</option>
            <option value="French">French</option>
            <option value="German">German</option>
          </select>
        </div>

        <button
          onClick={handleDeleteAccount}
          className="my-auto mx-auto flex justify-center items-center p-3 bg-red-600 text-white py-2 rounded-lg hover:bg-red-700 transition-all duration-300"
        >
          {t('DELETE_ACCOUNT')}
        </button>
      </div>
    </div>
  );
};

export default SettingsMainContent;