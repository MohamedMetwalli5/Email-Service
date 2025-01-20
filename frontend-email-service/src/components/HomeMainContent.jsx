import React from 'react';
import EmailsSnippetView from "./home-main-content-components/EmailsSnippetView";
import EmailFullView from "./home-main-content-components/EmailFullView";


const HomeMainContent = () => {
  return (
    <div className="flex gap-1 bg-gray-800 rounded-lg shadow-md h-full w-full">
      <EmailsSnippetView />
      <EmailFullView />
    </div>
  );
};

export default HomeMainContent;