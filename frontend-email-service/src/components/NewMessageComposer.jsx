import React, { useState } from "react";
import { useContext } from 'react';
import { AppContext } from '../AppContext.jsx';
import axios from "axios";
import { useTranslation } from 'react-i18next';


const NewMessageComposer = ({ onClose }) => {

  const { t } = useTranslation();

  const backendUrl = import.meta.env.VITE_BACKEND_API_URL;

  const { authToken } = useContext(AppContext);
  const { sharedUserEmail } = useContext(AppContext);

  const [formData, setFormData] = useState({
    sender:sharedUserEmail,
    receiver: "",
    subject: "",
    body: "",
    priority: "1",
    trash:"No",
    date: new Date().toISOString().slice(0, 10), // Today's date in the YYYY-MM-DD format
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ 
        ...formData,
         [name]: value }
    );
  };

  const handleSubmit = async(e) => {
    // console.log(formData);
    e.preventDefault();
    try {
      const response = await axios.post(`${backendUrl}/sendemail`, formData, {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });
      console.log(response.data);
    } catch (error) {
      console.error(error.response?.data || error.message);
    }
    onClose();
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