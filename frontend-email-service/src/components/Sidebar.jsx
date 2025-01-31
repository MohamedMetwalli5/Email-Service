import React, {useState, useEffect} from 'react';
import { LuInbox } from "react-icons/lu";
import { FaLocationArrow, FaPlus } from "react-icons/fa";
import { FaRegTrashCan } from "react-icons/fa6";
import { PiSignOutBold } from "react-icons/pi";
import SeamailWebsiteLogo from "../../public/SeamailWebsiteLogo.svg";
import { useNavigate } from 'react-router-dom';
import NewMessageComposer from './NewMessageComposer';
import { useContext } from 'react';
import { AppContext } from '../AppContext.jsx';
import { useTranslation } from 'react-i18next';


const Sidebar = () => {

  const { t, i18n } = useTranslation();
  
  const { setSharedMailBoxOption } = useContext(AppContext);
  const { sharedUserLanguage } = useContext(AppContext);

  const [isComposerOpen, setComposerOpen] = useState(false);
  
  const navigate = useNavigate();

  useEffect(() => {
    i18n.changeLanguage(sharedUserLanguage);
  }, [sharedUserLanguage, i18n]);

  return (
    <div className="flex h-full bg-gradient-to-b from-gray-900 to-gray-800 text-sm md:text-lg">
      <aside
        id="sidebar"
        className={`${
          isComposerOpen ? "w-80" : "w-64"
        } bg-gray-900 border-r border-gray-700 shadow-lg flex flex-col h-screen`}
      >
        <div className="flex flex-col h-full px-4 py-6 overflow-y-auto">
          <a className="flex items-center px-4 mb-6 hover:cursor-pointer" 
            onClick={() => navigate("/home")}
          >
            <img
              src={SeamailWebsiteLogo}
              className="h-10 mr-3"
              alt="Seamail Logo"
            />
            <span className="text-3xl font-bold text-blue-400">Seamail</span>
          </a>
          <ul className="space-y-4 flex-grow">
            <li>
              <a
                onClick={() => setComposerOpen(true)}
                className="flex items-center p-3 text-gray-200 bg-yellow-500 rounded-lg hover:bg-yellow-600 hover:cursor-pointer transition-all duration-300"
              >
                <FaPlus />
                <span className="ml-3 font-medium text-white">{t('NEW_MESSAGE')}</span>
              </a>
              {isComposerOpen && (
                <NewMessageComposer onClose={() => setComposerOpen(false)} />
              )}
            </li>
            <li>
              <a
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 hover:cursor-pointer transition-all duration-300"
                onClick={() => {setSharedMailBoxOption("Inbox"); navigate("/home");}}
              >
                <LuInbox />
                <span className="ml-3 font-medium text-white">{t('INBOX')}</span>
              </a>
            </li>
            <li>
              <a
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 hover:cursor-pointer transition-all duration-300"
                onClick={() => {setSharedMailBoxOption("Outbox"); navigate("/home");}}
              >
                <FaLocationArrow />
                <span className="ml-3 font-medium text-white">{t('SENT')}</span>
              </a>
            </li>
            <li>
              <a
                className="flex items-center p-3 text-gray-200 bg-blue-600 rounded-lg hover:bg-blue-700 hover:cursor-pointer transition-all duration-300"
                onClick={() => {setSharedMailBoxOption("Trashbox"); navigate("/home");}}
              >
                <FaRegTrashCan />
                <span className="ml-3 font-medium text-white">{t('TRASH')}</span>
              </a>
            </li>
          </ul>
          <li className="mt-auto">
            <a
              className="flex items-center p-3 text-gray-200 bg-red-500 rounded-lg hover:bg-red-600 hover:cursor-pointer transition-all duration-300"
              onClick={() => {
                localStorage.removeItem('authToken');
                localStorage.removeItem('sharedUserEmail');
                localStorage.removeItem('sharedMailBoxOption');
                localStorage.removeItem('sharedEmailToFullyView');
                localStorage.removeItem('sharedUserLanguage');

                navigate('/signin');
              }}
            >
              <PiSignOutBold />
              <span className="ml-3 font-medium text-white">{t('SIGN_OUT')}</span>
            </a>
          </li>
        </div>
      </aside>
    </div>
  );
};

export default Sidebar;