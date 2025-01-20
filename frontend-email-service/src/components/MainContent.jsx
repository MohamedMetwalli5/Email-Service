import React from 'react';
import EmailsSnippetView from "./main-content-components/EmailsSnippetView";
import EmailFullView from "./main-content-components/EmailFullView";


const MainContent = () => {
  return (
    <div className="flex gap-1 bg-gray-800 rounded-lg shadow-md h-full w-full">
      <EmailsSnippetView />
      <EmailFullView />
    </div>
  );
};

export default MainContent;