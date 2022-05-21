<template>
  <div id="container">
    <form style="border: solid; border-radius: 20px; height: 50%; padding: 20px; background-color: #069A8E;" action="action_page.php">
        <label for="To">To</label>
        <input type="text" id="To" name="firstname" style="border-radius: 20px;" placeholder="UserName@seamail.com">

        <label for="Subject">Subject</label>
        <input type="text" id="Subject" name="lastname" placeholder="Subject">

        <label id="Priority" for="Priority">Priority</label>
            <select id="Priority" name="country">
            <option value="1">1</option>
            <option value="2">2</option>
            <option value="3">3</option>
        </select>

        <label for="Message">Message</label>
        <textarea id="Message" name="Message" placeholder="Write something.." style="height:200px"></textarea>

        <input id="Submit" type="submit" value="Submit">

    </form>
  </div>
</template>

<script>
export default {
  name: "EmailForm",
  props: {
    msg: String,
  },
  data() {
    return {
      email: "",
      subject: "",
      content: "",
    };
  },
  methods: {
    Send() {
    },
  },
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
#container {
  margin-top: 10%;  
  border-radius: 20px;
  margin-left: 25%;  
  height: 50%;
  width: 50%;
  position: fixed;
}

/* Style inputs with type="text", select elements and textareas */
input[type=text], select, textarea {
  width: 100%; /* Full width */
  padding: 12px; /* Some padding */ 
  border: 1px solid #ccc; /* Gray border */
  border-radius: 4px; /* Rounded borders */
  box-sizing: border-box; /* Make sure that padding and width stays in place */
  margin-top: 6px; /* Add a top margin */
  margin-bottom: 16px; /* Bottom margin */
  resize: vertical /* Allow the user to vertically resize the textarea (not horizontally) */
}


label{
    width: 100%;
    padding: 5px;
    font: 25px;
    font-weight: bold;
    border-radius: 10px;
    background: #00FFAB;
}

#Subject {
    width: 100%;
    border-radius: 20px;
}

#Submit {
    font-size: larger;
    border-radius: 20px;
    background-color: rgb(93, 82, 251);
    color: aliceblue;
    display: inline-block;
    cursor: pointer;
}

#Priority{
    width: 100%;
    border-radius: 20px; 
    cursor: pointer;
}

#Message{
    border-radius: 20px;
}

/* .container {
  border-radius: 5px;
  background-color: #f2f2f2;
  padding: 20px;
} */
</style>
