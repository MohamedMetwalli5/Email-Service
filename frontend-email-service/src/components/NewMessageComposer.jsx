import React, { useState } from "react";
import { useContext } from 'react';
import { AppContext } from '../AppContext.jsx';
import apiClient from '../api/apiClient';
import { useTranslation } from 'react-i18next';
import { parseApiError } from '../utils/parseApiError';


const NewMessageComposer = ({ onClose }) => {

  const { t } = useTranslation();

  const { sharedUserEmail } = useContext(AppContext);

  const [formData, setFormData] = useState({
    sender:sharedUserEmail,
    receiver: "",
    subject: "",
    body: "",
    priority: "1",
    trash: false,
    date: new Date().toISOString().slice(0, 10), // Today's date in the YYYY-MM-DD format
  });

  const [fieldError, setFieldError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ 
        ...formData,
         [name]: value }
    );
  };

  const handleSubmit = async(e) => {
    e.preventDefault();
    try {
      // Use apiClient for protected endpoints; Bearer header attached by interceptor
      const response = await apiClient.post('/send-email', {
        receiver: formData.receiver,
        subject: formData.subject,
        body: formData.body,
        priority: formData.priority
      });
      
      console.log("Email sent:", response.data);
      onClose(); 
      
    } catch (error) {
      console.error("Send error:", error.response?.data || error.message);
      const parsed = parseApiError(error);
      if (parsed.errorCode === 'RECEIVER_NOT_FOUND') {
        setFieldError('The recipient was not found. Please check the email address.');
      } else if (parsed.fieldErrors.length > 0) {
        setFieldError(parsed.fieldErrors.join('\n'));
      } else {
        const fallbackMessages = {
          INTERNAL_ERROR: 'Something went wrong on our end. Please try again later.',
          NETWORK_ERROR: 'Could not connect to the server. Please check your internet connection.',
        };
        setFieldError(fallbackMessages[parsed.errorCode] || 'Failed to send email. Please try again.');
      }
    }
  };

  return (
    <div className="bottom-0 right-0 inset-0 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-gray-900 text-gray-100 p-6 rounded-lg shadow-lg w-96">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium">{t('RECEIVER')}</label>
            <input
              type="email"
              name="receiver"
              value={formData.receiver}
              onChange={handleChange}
              placeholder={t('ENTER_RECIPIENT_EMAIL')}
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium">{t('SUBJECT')}</label>
            <input
              type="text"
              name="subject"
              value={formData.subject}
              onChange={handleChange}
              placeholder={t('ENTER_SUBJECT')}
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium">{t('BODY')}</label>
            <textarea
              name="body"
              value={formData.body}
              onChange={handleChange}
              placeholder={t('ENTER_MESSAGE_BODY')}
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none"
              rows="4"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium">{t('PRIORITY')}</label>
            <select
              name="priority"
              value={formData.priority}
              onChange={handleChange}
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none cursor-pointer"
            >
              <option value="1">1 - {t('HIGH')}</option>
              <option value="2">2 - {t('MEDIUM')}</option>
              <option value="3">3 - {t('LOW')}</option>
            </select>
          </div>

          <div className="text-gray-500 rounded border p-1 border-gray-700">
            <label className="block text-sm font-bold">{t('DATE')}</label>
            <label className="block text-sm font-medium">{formData.date}</label>
          </div>

          {fieldError && (
            <div className="text-red-400 text-sm bg-red-900 bg-opacity-50 p-2 rounded">
              {fieldError.split('\n').map((line, i) => <div key={i}>{line}</div>)}
            </div>
          )}

          <div className="flex justify-between mt-4">
            <button
              type="button"
              onClick={onClose}
              className="bg-red-500 px-4 py-2 rounded hover:bg-red-600"
            >
              {t('CANCEL')}
            </button>
            <button
              type="submit"
              className="bg-green-500 px-4 py-2 rounded hover:bg-green-600"
            >
              {t('SEND')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default NewMessageComposer;