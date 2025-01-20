import React, { useState } from "react";

const NewMessageComposer = ({ onClose }) => {
  const [formData, setFormData] = useState({
    receiver: "",
    subject: "",
    body: "",
    priority: "1",
    date: new Date().toISOString().slice(0, 10), // Today's date in the YYYY-MM-DD format
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ 
        ...formData,
         [name]: value }
    );
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("Message Data: ", formData);
    onClose();
  };

  return (
    <div className="bottom-0 right-0 inset-0 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-gray-900 text-gray-100 p-6 rounded-lg shadow-lg w-96">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium">Receiver</label>
            <input
              type="email"
              name="receiver"
              value={formData.receiver}
              onChange={handleChange}
              placeholder="Enter recipient's email"
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium">Subject</label>
            <input
              type="text"
              name="subject"
              value={formData.subject}
              onChange={handleChange}
              placeholder="Enter subject"
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium">Body</label>
            <textarea
              name="body"
              value={formData.body}
              onChange={handleChange}
              placeholder="Enter message body"
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none"
              rows="4"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium">Priority</label>
            <select
              name="priority"
              value={formData.priority}
              onChange={handleChange}
              className="w-full p-2 bg-gray-800 rounded border border-gray-700 focus:outline-none cursor-pointer"
            >
              <option value="1">1 - High</option>
              <option value="2">2 - Medium</option>
              <option value="3">3 - Low</option>
            </select>
          </div>

          <div className="text-gray-500 rounded border p-1 border-gray-700">
            <label className="block text-sm font-bold">Date</label>
            <label className="block text-sm font-medium">{formData.date}</label>
          </div>

          <div className="flex justify-between mt-4">
            <button
              type="button"
              onClick={onClose}
              className="bg-red-500 px-4 py-2 rounded hover:bg-red-600"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="bg-green-500 px-4 py-2 rounded hover:bg-green-600"
            >
              Send
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default NewMessageComposer;