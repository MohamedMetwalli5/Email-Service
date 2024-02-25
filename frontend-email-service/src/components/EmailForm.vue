<template>
  <div id="container">
    <div style="border: solid; border-radius: 20px; height: 50%; padding: 2vw; background-color: #069A8E;">
      <label for="To">To</label>
      <input type="text" id="To" v-model="receiver" style="border-radius: 2vw;" placeholder="UserName@seamail.com">

      <label for="Subject">Subject</label>
      <input type="text" id="Subject" v-model="subject" placeholder="Subject">

      <label id="PriorityLabel" for="Priority">Priority</label>
      <select id="Priority" v-model="priority">
        <option value="1">1</option>
        <option value="2">2</option>
        <option value="3">3</option>
      </select>

      <label for="Message">Message</label>
      <textarea id="Message" v-model="body" placeholder="Write something.."></textarea>

      <button id="Submit" @click="Send">Submit</button>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: "EmailForm",
  props: ['message'],
  data() {
    return {
      sender: this.message,
      receiver: "",
      subject: "",
      body: "",
      priority: "1",
      date: "",
      trash: "No",
    };
  },
  methods: {
    Send() {
      const userData = {
        sender: this.sender,
        receiver: this.receiver,
        subject: this.subject,
        body: this.body,
        priority: this.priority,
        date: this.getCurrentDate(),
        trash: this.trash,
      };

      // Make a POST request to the server
      axios.post('http://localhost:8081/sendemail', userData)
        .then(response => {
          // Handle successful response
          console.log(response.data);
          window.location.reload();
        })
        .catch(error => {
          // Handle error
          alert("Couldn't send the email!");
        }
      );
    },
    getCurrentDate() {
      return new Date().toISOString().slice(0, 10);
    },
  },
};

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
#container {
  display: block;
  margin-top: 10%;  
  border-radius: 2vw;
  margin-left: 25%;  
  height: 50%;
  width: 50%;
  position: fixed;
}

/* Style inputs with type="text", select elements and textareas */
input[type=text], select, textarea {
  width: 100%; /* Full width */
  padding: 1vw; /* Some padding */ 
  border: 1px solid #ccc; /* Gray border */
  border-radius: 4px; /* Rounded borders */
  box-sizing: border-box; /* Make sure that padding and width stays in place */
  margin-top: 0.5vw; /* Add a top margin */
  margin-bottom: 1vw; /* Bottom margin */
  resize: none 
}


label{
    width: 100%;
    padding: 0.5vw;
    font: 2vw;
    font-weight: bold;
    border-radius: 10px;
    background: #00FFAB;
}

#Subject {
    width: 100%;
    border-radius: 2vw;
}

#Submit {
    font-size: larger;
    border-radius: 2vw;
    background-color: rgb(93, 82, 251);
    color: aliceblue;
    display: inline-block;
    cursor: pointer;
}

#Priority{
    width: 100%;
    border-radius: 2vw; 
    cursor: pointer;
}

#Message{
    border-radius: 2vw;
    height: 8vw;
}

</style>
