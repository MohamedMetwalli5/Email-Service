import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppContext } from '../AppContext.jsx';
import { useTranslation } from 'react-i18next';
import apiClient from '../api/apiClient';
import { parseApiError } from '../utils/parseApiError';
import toast from 'react-hot-toast';

const SettingsMainContent = () => {
  
  const { t } = useTranslation();
  
  const { sharedUserEmail, setSharedUserLanguage, sharedUserLanguage } = useContext(AppContext);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [profilePicture, setProfilePicture] = useState(null);
  const [file, setFile] = useState(null);
  const [fileSizeError, setFileSizeError] = useState('');

  const navigate = useNavigate();

  const handlePasswordChange = async () => {
    if (newPassword !== confirmPassword) {
      toast.error('Passwords do not match!');
      return;
    } else if (newPassword.length < 8) {
      toast.error('Password must be at least 8 characters long!');
      return;
    }
    try {
      await apiClient.put('/change-password', {
        email: sharedUserEmail,
        newPassword: newPassword,
      });
      toast.success('Password changed successfully.');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.errorCode === 'USER_NOT_FOUND') {
        toast.error('User not found. Please check your email address.');
      } else if (parsed.fieldErrors.length > 0) {
        toast.error(parsed.fieldErrors.join('\n'));
      } else {
        const fallbackMessages = {
          INTERNAL_ERROR: 'Something went wrong on our end. Please try again later.',
          NETWORK_ERROR: 'Could not connect to the server. Please check your internet connection.',
          UNAUTHORIZED: 'Your session has expired. Please sign in again.',
        };
        toast.error(fallbackMessages[parsed.errorCode] || 'Failed to change password. Please try again.');
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
      const parsed = parseApiError(error);
      if (parsed.fieldErrors.length > 0) {
        toast.error(parsed.fieldErrors.join('\n'));
      } else {
        const fallbackMessages = {
          INTERNAL_ERROR: 'Something went wrong on our end. Please try again later.',
          NETWORK_ERROR: 'Could not connect to the server. Please check your internet connection.',
          UNAUTHORIZED: 'Your session has expired. Please sign in again.',
        };
        toast.error(fallbackMessages[parsed.errorCode] || 'Failed to delete account. Please try again.');
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
      const parsed = parseApiError(error);
      if (parsed.fieldErrors.length > 0) {
        toast.error(parsed.fieldErrors.join('\n'));
      } else {
        const fallbackMessages = {
          INTERNAL_ERROR: 'Something went wrong on our end. Please try again later.',
          NETWORK_ERROR: 'Could not connect to the server. Please check your internet connection.',
        };
        toast.error(fallbackMessages[parsed.errorCode] || 'Failed to update language. Please try again.');
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
      toast.error('Please select a file to upload.');
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
        toast.success('Profile picture uploaded successfully!');
        window.location.reload();
      } catch (error) {
        const parsed = parseApiError(error);
        if (parsed.errorCode === 'INVALID_FILE_FORMAT') {
          toast.error('Only PNG and JPEG images under 5 MB are accepted.');
        } else if (parsed.fieldErrors.length > 0) {
          toast.error(parsed.fieldErrors.join('\n'));
        } else {
          const fallbackMessages = {
            INTERNAL_ERROR: 'Something went wrong on our end. Please try again later.',
            NETWORK_ERROR: 'Could not connect to the server. Please check your internet connection.',
          };
          toast.error(fallbackMessages[parsed.errorCode] || 'Failed to upload profile picture. Please try again.');
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
            <button
              onClick={handlePasswordChange}
              disabled={newPassword !== confirmPassword}
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