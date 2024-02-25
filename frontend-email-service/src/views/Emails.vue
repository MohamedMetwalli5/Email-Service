<template>
  <div id="container">

    <div id="optionsColumn">
      <div id="user-info" style="font-size: 1.5vw; font-weight: bold;">{{ userEmail }}</div>
      <button id="sendOption" @click="SendEmail()">
        Send Email ✏️
      </button>
      <div pageOptionDiv>
        <button id="inboxBtn" class="pageOption" @click="(pageOption = 'Inbox Mail📫')">
          Inbox 📫
        </button>
      </div>

      <div pageOptionDiv>
        <button id="trashBtn" class="pageOption" @click="(pageOption = 'Trash Box🗑️'), (LoadEmails('Trash'))">
          Trash 🗑️
        </button>
      </div>

      <div pageOptionDiv>
        <button
          class="pageOption" @click="(pageOption = 'Sent 📧'), (LoadEmails('Sent'))">
          Sent 📧
        </button>
      </div>
      <div pageOptionDiv>
        <button class="pageOption" id="LogOutOption" @click="LogOut()">Logout 🚪</button>
      </div>
    </div>



<!-- ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// -->



    <div class="displayColumn">
      <div>
        <h1 id="thePageTitle">{{ pageOption }}</h1>
        <ul id="emailsOperationDiv">
          <li class="emailsOperationButton">
            Filter
            
            <div class="subMenu-2">
              <ul>
                <div class="subMenuOption">
                  <button class="menuElement" @click="Filter()">Subject</button>
                  <input type="text" id="filterSubjectText" />
                </div>
                
                <div class="subMenuOption">
                  <button class="menuElement" @click="Filter()">Sender</button>
                  <input type="text" id="filterSenderText" />
                </div>
              </ul>
            </div>

          </li>
          <li class="emailsOperationButton">
            Sort
            <div class="subMenu-1">
              <ul>
                <button class="menuElement" @click=" sortText='Priority'">
                  Priority
                </button>
                <button class="menuElement" @click=" sortText='Date'">
                  Date
                </button>
                <button class="menuElement" @click=" sortText='Sender'">
                  Sender
                </button>
                <button class="menuElement" @click=" sortText='Subject'">
                  Subject
                </button>
              </ul>
            </div>
          </li>
        </ul>
        <br />
      </div>


      <table id="table-box">
        <tr id="titleRow">
          <td id="checkBoxClass">&nbsp;</td>
          <td @click="Sort('Sender')">Sender</td>
          <td @click="Sort('Subject')">Subject</td>
          <td @click="Sort('Priority')">Priority</td>
          <td @click="Sort('Date')" style="border-top-right-radius: 2vw;">Date</td>
        </tr>
        <tr class="row" v-for="(email, index) in emails" :key="email.id">
          <td class="rOption">
            <input type="button" name="EmailsOption" style="border-radius: 1vw; color: red; cursor: pointer; font-weight: bold;" value="X" @click="(deleteEmail(index))"/>
            <input type="button" name="EmailsOption" style="border-radius: 0.5vw; color: rgb(25, 0, 255); cursor: pointer; font-weight: bold;" value="Read" @click="(readEmail(index))"/>
          </td>
          <td class="rOption">{{ email.sender }}</td>
          <td class="rOption">{{ email.subject }}</td>
          <td class="rOption">{{ email.priority }}</td>
          <td class="rOption">{{ email.date }}</td>
        </tr>
      </table>
    </div>
    
    <EmailForm id="EmailForm" ref="EmailForm" :style="{ display: ShowEmailForm ? 'none' : 'block' }" :message="userEmail"/>
  
  </div>

</template>

<script>
import EmailForm from '../components/EmailForm';
const axios = require("axios").default;

export default {
  name: "Emails",
  components:{
    EmailForm,
  },
  data: function () {
    return {
      ShowEmailForm: false,
      userEmail: "example1@seamail.com",
      pageOption: "Inbox Mail ✉️",
      senderFilterText: "null",
      subjectFilterText: "null",
      sortText: "null",
      ShowEmailForm: "true",
      emails: [],
    };
  },
  methods: {
    SendEmail () {
      this.ShowEmailForm = !this.ShowEmailForm; 
    },
    // This function checks the status of the user whether he is logged in or note by printing his status
    CheckAuthStatus() {
      
    },
    deleteEmail(index){
      // console.log('Message deleted:', index);
      this.emails.splice(index, 1);
    },
    readEmail(index){
      // console.log('Message read:', index);
      alert("The Message: \n" + this.emails[index].body)
    },
    // This functions sign the user out
    LoadEmails(LoadingMailsOption) {
      if(LoadingMailsOption === "Inbox"){
        // An object with user credentials
        const userData = {
          email: this.userEmail,
          password:"1",
        };

        // Make a POST request to the server
        axios.post('http://localhost:8081/inbox', userData)
          .then(response => {
            // Handle successful response
            response.data.forEach(email => {
              this.emails.push(email);
            });
            console.log("Loading inbox emails!");
            console.log(response.data);
          })
          .catch(error => {
            // Handle error
            console.log("Error!");
          }
        );

      }else if(LoadingMailsOption === "Trash"){
        // An object with user credentials
        const userData = {
          email: this.email,
          password: "1"
        };
        // Make a POST request to the server
        axios.post('http://localhost:8081/trashbox', userData)
          .then(response => {
            // Handle successful response
            this.emails = [];
            response.data.forEach(email => {
              this.emails.push(email);
            });
            console.log("Loading trash emails!");
            console.log(response.data);
          })
          .catch(error => {
            // Handle error
            console.log("Error!");
          }
        );

      }else if(LoadingMailsOption === "Sent"){
        // An object with user credentials
        const userData = {
          email: this.email,
          password: "1",
        };
        // Make a POST request to the server
        axios.post('http://localhost:8081/outbox', userData)
          .then(response => {
            // Handle successful response
            this.emails = [];
            response.data.forEach(email => {
              this.emails.push(email);
            });        
            console.log("Loading sent emails!");
            console.log(response.data);
          })
          .catch(error => {
            // Handle error
            console.log("Error!");
          }
        );
      }
    },
    // This functions sign the user out
    LogOut() {
    },
  },
  beforeMount() {
    this.CheckAuthStatus();
    this.LoadEmails("Inbox");
  },
};
</script>
<style  scoped>

#container {
  display: flex;
  background-color: rgb(255, 238, 0);
  height: 100%;
  max-width: 100%;
  /* overflow-x: hidden; */
  overflow-y: hidden;
}

#thePageTitle {
  text-align: center;
  font-size: 6.5vw;
  color: #211C6A;
  margin-top: 0px;
  padding-left: 10px;
  padding: 0.5vw;
  height: 100%;
  max-width: 90%;
  border: solid;
  border-radius: 2vw;
  background-color: #9BCF53;
}
#optionsColumn {
  background: #7BD3EA;
  float: left;
  padding: 10px;
  height: 100%;
  border: solid;
  border-radius: 0vw 2vw 2vw 0vw;
  border-color: #7BD3EA;
  margin: 0vw;
  margin-right: 1vw;
}
.displayColumn {
  float: left;
  margin-left: 2vw;
  width: 90%;
  padding: 1vw;
  height: 100%;
}
#sendOption {
  text-align: center;
  height: 100%;
  margin-top: 15vw;
  border-radius: 2vw;
  border: solid;
  color: green;
  background: white;
  font-size: 4vw;
  font-weight: bold;
  margin-bottom: 50px;
  margin-top: 2vw;
  cursor: pointer;
}

#sendOption:hover {
  background: rgb(108, 225, 240);
}

.pageOption {
  width: 21vw;
  border-radius: 2vw;
  border: solid;
  color: green;
  background: white;
  font-size: 4vw;
  font-weight: bold;
  margin-bottom: 4vw;
  cursor: pointer;
  animation: shake 0.8s;
}

.pageOption:hover {
    background: rgb(129, 241, 148);
    animation:  shake 0.8s  ;
}

#LogOutOption:hover {
    background: rgb(244, 131, 131);
    animation:  shake 0.8s  ;
}

@keyframes shake{
    0%{
      transform: translateX(0)
    }
    25%{
      transform: translateX(10px);
    }
      
    50%{
      transform: translateX(-10px);
    }
    100%{
      transform: translateX(0px);
    }
}

#table-box {
  margin: auto;
  width: 100%;  
  border-radius: 2vw 2vw 0vw 0vw;
  border: solid;
  border-color: white;
}

#titleRow {
  border-top-right-radius: 2vw;
  font-size: 4vw;
  font-weight: bolder;
  color: #ffffff;
  text-align: center;
  background-color: #416D19;
  margin: auto;
  border-bottom: solid;
}
.row {
  border: solid;
  border-bottom: black;
  color: black;
  background-color: rgb(227, 230, 230);
  font-size: 1.5vw;
  height: 2vw;
  text-align: center;
}
.row:hover {
  background: rgb(108, 240, 211);
}
.rOption {
  margin-left: 3px;
  border-top: solid;
  border-bottom: rgb(0, 0, 0);
}


#emailsOperationDiv {
  display: flex;
  margin: auto;
}

.emailsOperationButton {
  margin-right: 15vw;
  float: left;
  color: rgb(255, 255, 255);
  border: solid;
  border-color: brown;
  border-radius: 2vw;
  background: #36afe6;
  padding: 0.9vw;
  font-size: 2vw;
  text-align: center;
  cursor: pointer;
}
.emailsOperationButton:hover {
  background: rgb(50, 230, 74);
}
.subMenu-1, .subMenu-2 {
  display: none;
  border: solid;
  float: left;
  border-color: brown;
  border-radius: 2vw;
  background: rgb(235, 74, 74);
}

.menuElement {
  display: flex;
  float: left;
  font-size: 2vw;
  border: solid;
  margin-left: 0px;
  border-radius: 2vw;
  cursor: pointer;
}

.menuElement:hover {
  background: rgb(9, 194, 194);
}

.emailsOperationButton:hover .subMenu-1 {
  margin-top: 1vw;
  display: block;
  position: absolute;
  border-radius: 2vw;
  border: solid;
  padding: 0.2vw;
  border-color: brown;
  background: rgb(241, 103, 103);
  float: left;
}

#filterSubjectText {
  background: whitesmoke;
  border-radius: 2vw;
  margin-right: 10vw;
}

#filterSenderText {
  background: whitesmoke;
  border-radius: 2vw;
}


.emailsOperationButton:hover .subMenu-2 {
  width: auto;
  margin-top: 1vw;
  display: block;
  position: absolute;
  border-radius: 2vw;
  border: solid;
  padding: 0.2vw;
  border-color: brown;
  background: rgb(241, 103, 103);
  float: left;
}

#checkBoxClass{
  border-top-left-radius: 2vw;
}

.subMenuOption{
  display: flex;
  float: left;
}

</style>
