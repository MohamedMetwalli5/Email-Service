<template>
  <div id="container">
    <div id="optionsColumn">
      <div id="user-info" style="font-size: 25px">{{ userEmail }}</div>
      <button id="sendOption" onclick="window.location.href='/SendEmail';">
        Send Email ✏️
      </button>
      <div pageOptionDiv>
        <button id="inboxBtn" class="pageOption">Inbox 📫</button>
      </div>
      <div pageOptionDiv>
        <button
          id="trashBtn"
          @click="
            // SetButtonColorTrash(),
            (pageOption = 'Trash Box🗑️'),
              (pageNumber = 1),
              ChangePage(),
              (document.getElementById('pageNumberOneOption').value = true),
              (document.getElementById('pageNumberOneOption').checked = true),
              console.log('pageNumber'),
              (folderName = 'Trash')
          "
          class="pageOption"
        >
          Trash 🗑️
        </button>
      </div>
      <div pageOptionDiv>
        <button
          class="pageOption"
          @click="
            (pageOption = 'Drafts 📋'),
              ChangePage(),
              (pageNumber = 1),
              (folderName = 'Draft')
          "
        >
          Drafts 📋
        </button>
      </div>
      <div pageOptionDiv>
        <button
          class="pageOption"
          @click="
            (pageOption = 'Sent 📧'),
              ChangePage(),
              (pageNumber = 1),
              (folderName = 'Sent')
          "
        >
          Sent 📧
        </button>
      </div>
      <div pageOptionDiv>
        <button class="pageOption" @click="LogOut()">Logout 🚪</button>
      </div>
    </div>
    <div class="displayColumn">
      <div>
        <h1 id="theInboxTitle">{{ pageOption }}</h1>
        <ul id="emailsOperationDiv">
          <li class="emailsOperationButton">
            Filter
            <div class="subMenu-2">
              <ul>
                <button class="menuElement" @click="Filter()">Subject</button>
                <input type="text" id="filterSubjectText" />

                <br />
                <button class="menuElement" @click="Filter()">Sender</button>
                <input type="text" id="filterSenderText" />
              </ul>
            </div>
          </li>
          <li class="emailsOperationButton">
            Sort
            <div class="subMenu-1">
              <ul>
                <button class="menuElement" @click="sortText = 'Priority'">
                  Priority
                </button>
                <button class="menuElement" @click="sortText = 'Date'">
                  Date
                </button>
                <button class="menuElement" @click="sortText = 'Sender'">
                  Sender
                </button>
                <button class="menuElement" @click="sortText = 'Subject'">
                  Subject
                </button>
              </ul>
            </div>
          </li>
        </ul>
        <div id="pageNumberOptionsDiv">
          <h2>&nbsp; 1 2 3 4</h2>
          <input
            type="radio"
            id="pageNumberOneOption"
            name="radioButtonPageNumberOption"
            value="false"
            checked="false"
            @click="(pageNumber = 1), ChangePage()"
          />

          <input
            type="radio"
            id="pageNumberTwoOption"
            name="radioButtonPageNumberOption"
            value="false"
            @click="(pageNumber = 2), ChangePage()"
          />
          <input
            type="radio"
            id="pageNumbeThreerOption"
            name="radioButtonPageNumberOption"
            value="false"
            @click="(pageNumber = 3), ChangePage()"
          />
          <input
            type="radio"
            id="pageNumberFourOption"
            name="radioButtonPageNumberOption"
            value="false"
            @click="(pageNumber = 4), ChangePage()"
          />
        </div>
        <br />
      </div>
      <table id="table-box">
        <tr id="titleRow">
          <td class="checkBoxClass">&nbsp;</td>
          <td @click="Sort('Sender')">Sender</td>
          <td @click="Sort('Subject')">Subject</td>
          <td @click="Sort('Priority')">Priority</td>
          <td @click="Sort('Date')">Date</td>
        </tr>
        <tr class="row">
          <td class="rOption">
            <input
              type="radio"
              name="EmailsOption"
              value="false"
              @click="namePointer = 1"
            />
          </td>
          <td class="rOption">
            {{ sender[0] }}
          </td>
          <td class="rOption">{{ subject[0] }}</td>
          <td class="rOption">{{ priority[0] }}</td>
          <td class="rOption">{{ date[0] }}</td>
        </tr>
        <tr class="row">
          <td class="rOption">
            <input
              type="radio"
              name="EmailsOption"
              value="false"
              @click="namePointer = 2"
            />
          </td>
          <td class="rOption">{{ sender[1] }}</td>
          <td class="rOption">{{ subject[1] }}</td>
          <td class="rOption">{{ priority[1] }}</td>
          <td class="rOption">{{ date[1] }}</td>
        </tr>
        <tr class="row">
          <td>
            <input
              type="radio"
              name="EmailsOption"
              value="false"
              @click="namePointer = 2"
            />
          </td>
          <td class="rOption">{{ sender[2] }}</td>
          <td class="rOption">{{ subject[2] }}</td>
          <td class="rOption">{{ priority[2] }}</td>
          <td class="rOption">{{ date[2] }}</td>
        </tr>
        <tr class="row">
          <td class="rOption">
            <input
              type="radio"
              name="EmailsOption"
              value="false"
              @click="namePointer = 4"
            />
          </td>
          <td class="rOption">{{ sender[3] }}</td>
          <td class="rOption">{{ subject[3] }}</td>
          <td class="rOption">{{ priority[3] }}</td>
          <td class="rOption">{{ date[3] }}</td>
        </tr>
        <tr class="row">
          <td class="rOption">
            <input
              type="radio"
              name="EmailsOption"
              value="false"
              @click="this.namePointer = 5"
            />
          </td>
          <td class="rOption">{{ sender[4] }}</td>
          <td class="rOption">{{ subject[4] }}</td>
          <td class="rOption">{{ priority[4] }}</td>
          <td class="rOption">{{ date[4] }}</td>
        </tr>
      </table>
    </div>
    <!-- -->
  </div>
</template>

<script>

export default {
  name: "Emails",
  data: function () {
    return {
      userEmail: "",
      pageOption: "Inbox Mail ✉️", //the folder name
      sender: [].fill(null),
      subject: [].fill(null),
      priority: [].fill(null),
      date: [].fill(null),
      checkMark: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
      senderFilterText: "null",
      subjectFilterText: "null",
      sortText: "null",
      pageNumber: 1,
      folderName: "inbox",
      namePointer: 0,
    };
  },
  methods: {
    // This function checks the status of the user whether he is logged in or note by printing his status
    CheckAuthStatus() {
      
    },
    // This functions sign the user out
    LogOut() {
    },
  },
  beforeMount() {
    this.CheckAuthStatus();
  },
};
</script>
<style  scoped>
#container {
  display: flex;
  background-color: rgb(255, 238, 0);
  height: 100%;
  min-height: 1100px;
  width: 100%;
  min-width: 100%;
  background-position-x: fixed;
}
.ButtonsGroup {
  background-image: linear-gradient(-45deg, rgb(59, 203, 228), magenta);
  border-radius: 20px;
  margin-top: 1px;
  width: 100%;
  height: 5%;
}
.b {
  margin: auto;
  margin-top: 5px;
  margin-bottom: 5px;
  font-size: 20px;
  font-style: oblique;
  width: 10%;
  height: 80%;
  background-color: linear-gradient(
    -100deg,
    rgb(153, 35, 168),
    rgb(213, 233, 31)
  );
  background: linear-gradient(-100deg, rgb(72, 212, 16), rgb(215, 236, 21));
  border-radius: 20px;
}
#theInboxTitle {
  text-align: center;
  line-height: 70px;
  font-size: 60px;
  color: rgb(36, 47, 196);
  /* margin: auto; */
  margin-top: 0px;
  padding-left: 10px;
  padding-bottom: 1px;
  height: 100%;
  min-width: 250px;
  border: solid;
  border-radius: 20px;
  background-color: aqua;
}
#optionsColumn {
  background: linear-gradient(-45deg, rgb(238, 189, 30), rgb(255, 238, 0));
  float: left;
  width: 350px;
  padding: 10px;
  height: 100%;
  border: solid;
  border-radius: 15px;
  border-color: red;
  margin-top: 10px;
}
.displayColumn {
  float: left;
  margin-left: 2%;
  width: 80%;
  padding: 10px;
  height: 1000px;
}
#sendOption {
  text-align: center;
  height: 100%;
  margin-top: 150px;
  border-radius: 20px;
  border: solid;
  color: green;
  background: white;
  font-size: 40px;
  font-weight: bold;
  margin-bottom: 50px;
  margin-top: 20px;
}
#sendOption:hover {
  background: rgb(108, 225, 240);
}
.pageOption {
  width: 190px;
  height: 100%;
  border-radius: 20px;
  border: solid;
  color: green;
  background: white;
  font-size: 30px;
  font-weight: bold;
  margin-bottom: 50px;
}
.pageOption:hover {
  background: rgb(129, 241, 148);
}
.pageOptionDiv {
  background-image: black;
  color: aqua;
}
#table-box {
  margin: auto;
  width: 100%;
  height: 70%;
  border-radius: 20px;
  border: solid;
  border-color: white;
}
#titleRow {
  border-radius: 20px;
  width: 90%;
  height: 8%;
  font-size: 35px;
  font-weight: bolder;
  border-radius: 50px;
  color: rgb(243, 45, 184);
  text-align: center;
  background-color: rgb(255, 217, 0);
  margin: auto;
  margin-bottom: 10px;
  border-bottom: soli;
}
.row {
  border: solid;
  border-radius: 30px;
  border-bottom: black;
  color: black;
  font-size: 30px;
  height: 45px;
}
.row:hover {
  background: rgb(108, 240, 174);
}
.rOption {
  /* used to make the underline between emails */
  margin-left: 3px;
  border-top: solid;
  border-bottom: rgb(0, 0, 0);
}
#pageNumberOptionsDiv {
  line-height: 5px;
  padding-bottom: 4px;
  margin-top: 1.8%;
  border-radius: 25px;
  margin-left: 45%;
  width: 100px;
  color: rgb(29, 201, 6);
  border: solid;
}
#emailsOperationDiv {
  display: inline-block;
  margin-top: auto;
  width: 100%;
  max-width: 1430px;
  color: rgb(226, 40, 211);
  border: solid;
  border-radius: 40px;
  background: blueviolet;
  height: 50px;
}
.emailsOperationButton {
  margin-right: 150px;
  line-height: 40px;
  float: left;
  width: 110px;
  color: rgb(255, 255, 255);
  border: solid;
  border-color: brown;
  border-radius: 20px;
  background: rgb(235, 74, 74);
  height: 90%;
  font-size: 30px;
  text-align: center;
}
.emailsOperationButton:hover {
  background: rgb(50, 230, 74);
}
.subMenu-1 {
  display: none;
  border-bottom: solid;
  border-color: brown;
  border-radius: 20px;
  background: rgb(235, 74, 74);
}
.menuElement {
  float: left;
  /* display: inline-block; */
  padding-left: 5px;
  /* margin-left: 10px;
  margin-right: 20px; */
  color: black;
  border: solid;
  border-radius: 20px;
  height: 90%;
}
.menuElement:hover {
  background: rgb(9, 194, 194);
}
.emailsOperationButton:hover .subMenu-1 {
  text-align: left;
  margin-top: 0px;
  display: block;
  position: absolute;
  border-radius: 20px;
  border: solid;
  border-color: brown;
  background: rgb(241, 103, 103);
  height: 30px;
}
#filterSubjectText {
  background: whitesmoke;
  border-radius: 10px;
}
#filterSenderText {
  background: whitesmoke;
  border-radius: 10px;
}
.subMenu-2 {
  display: none;
  float: left;
  border: solid;
  border-radius: 20px;
  border-color: brown;
  background: rgb(241, 103, 103);
  width: 220px;
}
.emailsOperationButton:hover .subMenu-2 {
  text-align: left;
  margin-top: 0px;
  display: block;
  position: absolute;
  float: left;
  border: solid;
  border-radius: 20px;
  border-color: brown;
  background: rgb(241, 103, 103);
  width: 220px;
}
</style>
