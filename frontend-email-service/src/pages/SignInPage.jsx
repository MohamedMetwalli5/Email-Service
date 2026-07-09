import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useContext } from 'react';
import { AppContext } from '../AppContext.jsx';
import { parseApiError } from '../utils/parseApiError';
import SignInWithDiscord from '../components/SigninWithDiscord.jsx';
import apiClient from '../api/apiClient';
import toast from 'react-hot-toast';
import { useTranslation } from 'react-i18next';

const SignInPage = () => {
  const { t } = useTranslation();
  const termsOfUse = import.meta.env.VITE_TERMS_OF_USE_URL;
  const privacyPolicy = import.meta.env.VITE_PRIVACY_POLICY_URL;
  const { setAuthToken, setRefreshToken, setSharedUserEmail } = useContext(AppContext);

  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevState) => ({
      ...prevState,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await apiClient.post('/sign-in', {
        email: formData.email,
        password: formData.password,   
      });
      const { accessToken, refreshToken } = response.data;
      setAuthToken(accessToken);
      setRefreshToken(refreshToken);
      setSharedUserEmail(formData.email);
      navigate('/home');
    } catch (error) {
      const parsed = parseApiError(error);
      if (parsed.errorCode === 'USER_NOT_FOUND') {
        toast.error(t('INVALID_EMAIL_OR_PASSWORD'));
      } else if (parsed.fieldErrors.length > 0) {
        toast.error(parsed.fieldErrors.join('\n'));
      } else {
        const fallbackMessages = {
          INTERNAL_ERROR: t('INTERNAL_ERROR_MSG'),
          NETWORK_ERROR: t('NETWORK_ERROR_MSG'),
          UNAUTHORIZED: t('SESSION_EXPIRED'),
        };
        toast.error(fallbackMessages[parsed.errorCode] || t('SIGNIN_FAILED'));
      }
    }
  };

  
  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-500 to-blue-600 flex items-center justify-center p-4">
      <div className="w-full max-w-lg bg-white rounded-lg shadow-lg p-8 space-y-6">
        <h2 className="text-4xl font-bold text-center text-blue-700">Seamail</h2>
        <p className="text-center text-gray-600">{t('WELCOME_BACK')}</p>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-lg text-gray-800">{t('EMAIL_LABEL')}</label>
            <input
              type="email"
              id="email"
              name="email"
              placeholder="example@seamail.com"
              value={formData.email}
              onChange={handleInputChange}
              className="w-full p-3 mt-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-lg text-gray-800">{t('PASSWORD_LABEL')}</label>
            <input
              type="password"
              id="password"
              name="password"
              placeholder="••••••••"
              value={formData.password}
              onChange={handleInputChange}
              className="w-full p-3 mt-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div className="flex-col justify-between items-center">
            <button
              type="submit"
              className="w-full bg-yellow-400 hover:bg-yellow-500 text-white font-bold py-3 rounded-md transition-all duration-300"
            >
              {t('SIGN_IN_BUTTON')}
            </button>
          </div>
        </form>

        <SignInWithDiscord />

        <p className="text-center text-sm text-gray-500">
          {t('DONT_HAVE_ACCOUNT')}{' '}
          <a href="/" className="text-blue-500 hover:text-blue-700">
            {t('SIGN_UP_LINK')}
          </a>
        </p>

        <p className="text-center text-xs text-gray-400 pt-2">
          {t('TERMS_AGREE')}{' '}
          <a
            href={termsOfUse}
            className="underline hover:text-blue-500 transition-colors duration-200"
          >
            {t('TERMS_OF_USE')}
          </a>{' '}
          and{' '}
          <a
            href={privacyPolicy}
            className="underline hover:text-blue-500 transition-colors duration-200"
          >
            {t('PRIVACY_POLICY')}
          </a>
          .
        </p>
      </div>
    </div>
  );
};

export default SignInPage;