import React, { useState, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppContext } from '../AppContext.jsx';
import { useTranslation } from 'react-i18next';
import apiClient from '../api/apiClient';
import { parseApiError } from '../utils/parseApiError';

const SettingsMainContent = () => {
  
  const { t } = useTranslation();
  
  const { sharedUserEmail, setSharedUserLanguage, sharedUserLanguage } = useContext(AppContext);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [profilePicture, setProfilePicture] = useState(null);
  const [file, setFile] = useState(null);
  const [fileSizeError, setFileSizeError] = useState('');

  const navigate = useNavigate();

  const handlePasswordChange = async () => {
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    } else if (newPassword.length < 8) {
      setError('Password must be at least 8 characters long!');
      return;
    }
    try {
      await apiClient.put('/change-password', {
        email: sharedUserEmail,
        newPassword: newPassword,
      });
      setSuccessMessage('Password changed successfully.');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const parsed = parseApiError(err);
      // Handle specific error codes for precise UX feedback
      if (parsed.errorCode === 'USER_NOT_FOUND') {
        setError('User not found. Please check your email address.');
      } else if (parsed.fieldErrors.length > 0) {
        setError(parsed.fieldErrors.join('\n'));
      } else {
        setError(parsed.message);
      }
    }
  };

  const handleDeleteAccount = async () => {
    try {
      // Use apiClient for protected endpoints; Bearer header attached by interceptor
      await apiClient.delete('/delete-account', { 
        data: { email: sharedUserEmail }, 
      });
      navigate("/");
    } catch (error) {
      console.error("Account deletion error:", error.response?.data || error.message);
      // Use parseApiError instead of old errors field; handle specific codes
      const parsed = parseApiError(error);
      if (parsed.fieldErrors.length > 0) {
        setError(parsed.fieldErrors.join('\n'));
      } else {
        setError(parsed.message);
      }
    }
  };

  const handleLanguageChange = async (language) => {
    try {
      // Use apiClient for protected endpoints; Bearer header attached by interceptor
      await apiClient.put('/update-language', 
        { language, email: sharedUserEmail }
      );
      setSharedUserLanguage(language);
    } catch (error) {
      console.error("Language update error:", error.response?.data);
      // Use parseApiError instead of old errors field
      const parsed = parseApiError(error);
      if (parsed.fieldErrors.length > 0) {
        setError(parsed.fieldErrors.join('\n'));
      } else {
        setError(parsed.message);
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
        // Use apiClient for protected endpoints; Bearer header attached by interceptor
        await apiClient.post(`/${sharedUserEmail}/profile-picture`, imageBytes, {
          headers: {
            'Content-Type': 'application/octet-stream',
          }
        });
        alert('Profile picture uploaded successfully!');
        window.location.reload();
      } catch (error) {
        console.error('Error uploading profile picture:', error.response?.data);
        // Use parseApiError; handle INVALID_FILE_FORMAT with specific message
        const parsed = parseApiError(error);
        if (parsed.errorCode === 'INVALID_FILE_FORMAT') {
          setError('Only PNG and JPEG images under 5 MB are accepted.');
        } else if (parsed.fieldErrors.length > 0) {
          setError(parsed.fieldErrors.join('\n'));
        } else {
          setError(parsed.message);
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
            accept="image/png,image/jpeg"
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
            {successMessage && <p className="text-green-500 mb-4">{successMessage}</p>}
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