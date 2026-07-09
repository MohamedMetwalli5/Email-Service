import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppContext } from '../AppContext.jsx';
import { useTranslation } from 'react-i18next';
import apiClient from '../api/apiClient';
import { parseApiError } from '../utils/parseApiError';
import toast from 'react-hot-toast';

const SettingsMainContent = () => {
  
  const { t } = useTranslation();
  
  const { sharedUserEmail, setSharedUserLanguage, sharedUserLanguage, bumpProfilePicture } = useContext(AppContext);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [profilePicture, setProfilePicture] = useState(null);
  const [file, setFile] = useState(null);
  const [fileSizeError, setFileSizeError] = useState('');

  const navigate = useNavigate();

  const handlePasswordChange = async () => {
    if (newPassword !== confirmPassword) {
      toast.error(t('PASSWORDS_DO_NOT_MATCH'));
      return;
    } else if (newPassword.length < 8) {
      toast.error(t('PASSWORD_TOO_SHORT'));
      return;
    }
    try {
      await apiClient.put('/change-password', {
        email: sharedUserEmail,
        currentPassword: currentPassword,
        newPassword: newPassword,
      });
      toast.success(t('PASSWORD_CHANGED_SUCCESS'));
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const parsed = parseApiError(err);
      if (parsed.errorCode === 'USER_NOT_FOUND') {
        toast.error(t('USER_NOT_FOUND_CHECK_EMAIL'));
      } else if (parsed.fieldErrors.length > 0) {
        toast.error(parsed.fieldErrors.join('\n'));
      } else {
        const fallbackMessages = {
          INTERNAL_ERROR: t('INTERNAL_ERROR_MSG'),
          NETWORK_ERROR: t('NETWORK_ERROR_MSG'),
          UNAUTHORIZED: t('SESSION_EXPIRED'),
        };
        toast.error(fallbackMessages[parsed.errorCode] || t('FAILED_CHANGE_PASSWORD'));
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
          INTERNAL_ERROR: t('INTERNAL_ERROR_MSG'),
          NETWORK_ERROR: t('NETWORK_ERROR_MSG'),
          UNAUTHORIZED: t('SESSION_EXPIRED'),
        };
        toast.error(fallbackMessages[parsed.errorCode] || t('FAILED_DELETE_ACCOUNT'));
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
          INTERNAL_ERROR: t('INTERNAL_ERROR_MSG'),
          NETWORK_ERROR: t('NETWORK_ERROR_MSG'),
        };
        toast.error(fallbackMessages[parsed.errorCode] || t('FAILED_UPDATE_LANGUAGE'));
      }
    }
  };

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    const maxSizeInBytes = 5 * 1024 * 1024; // 5MB
    
    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg'];

    if (selectedFile) {
      if (!allowedTypes.includes(selectedFile.type)) {
        setFileSizeError(t('INVALID_FILE_TYPE'));
        setFile(null);
        e.target.value = null;
        return;
      }

      if (selectedFile.size > maxSizeInBytes) {
        setFileSizeError(t('FILE_TOO_LARGE'));
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
      toast.error(t('SELECT_FILE_TO_UPLOAD'));
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
        toast.success(t('PROFILE_PICTURE_UPLOADED'));
        bumpProfilePicture();
        setFile(null);
      } catch (error) {
        const parsed = parseApiError(error);
        if (parsed.errorCode === 'INVALID_FILE_FORMAT') {
          toast.error(t('ONLY_PNG_JPEG_UNDER_5MB'));
        } else if (parsed.fieldErrors.length > 0) {
          toast.error(parsed.fieldErrors.join('\n'));
        } else {
          const fallbackMessages = {
            INTERNAL_ERROR: t('INTERNAL_ERROR_MSG'),
            NETWORK_ERROR: t('NETWORK_ERROR_MSG'),
          };
          toast.error(fallbackMessages[parsed.errorCode] || t('FAILED_CHANGE_PASSWORD'));
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
        
        {sharedUserEmail?.endsWith("@seamail.com") && (
          <div className="mb-6">
            <h2 className="text-lg text-gray-200 mb-2">{t('CHANGE_PASSWORD')}</h2>
            <input
              type="password"
              placeholder={t('CURRENT_PASSWORD')}
              className="p-2 w-full mb-4 rounded-lg bg-gray-700 text-white"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
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
            <option value="en">English</option>
            <option value="fr">French</option>
            <option value="de">German</option>
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