<template>
  <div id="container">
    <h1 id="title" onclick="window.location.href='/';">Seamail</h1>
    <form id="login-form">
      <input
        type="email"
        placeholder="UserName@seamail.com"
        value=""
        ref="emailRef"
        v-model="email"
        required
        class="text-box"
        id="UserName"
      />
      <input
        type="password"
        placeholder="Password"
        value=""
        ref="passwordRef"
        v-model="password"
        required
        class="text-box"
        id="Password"
      />
      <input
        type="password"
        placeholder="Repeat password"
        value=""
        required
        class="text-box"
        id="RepeatedPassword"
      />
      <input type="button" value="Sign up" id="submit" @click="SignUp()" />
    </form>
    <h2>
      Already have an account?
      <a style="cursor: pointer; color: blue;" onclick="window.location.href='/signin';">Sign in</a>
    </h2>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: "Signup",
  props: {
    msg: String,
  },
  data() {
    return {
      email: "",
      password: "",
    };
  },
  methods: {
    SignUp() {
      // The form filling logic
      if(!document.getElementById("UserName").value.endsWith("@seamail.com")){
        alert("User name must end with '@seamail.com'");
      }else if(document.getElementById("Password").value != document.getElementById("RepeatedPassword").value){
        alert("Please, repeat the password correctly!");
      }else if(document.getElementById("Password").value.length < 8){
        alert("Password length must be at least 8!");
      }else{
        // An object with user credentials
        const userData = {
          email: this.email,
          password: btoa(this.password)
        };

        // Make a POST request to the server
        axios.post('http://localhost:8081/signup', userData)
          .then(response => {
            // Handle successful response
            console.log(response.data);
            window.location.href='/emails';
          })
          .catch(error => {
            // Handle error
            alert("This email already exists!");
          });
      }
    }
  },
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
#container {
  background-color: rgb(0, 225, 255);
  height: 100%;
  width: 100%;
  position: fixed;
}
#title {
  min-width: 30vw;
  border-bottom: 10px solid #ffffff;
  border-radius: 50px;
  padding-bottom: 15px;
  display: inline-block;
  font-size: 8vw;
  color: rgb(252, 52, 52);
  margin-left: 34%;
  margin-top: 1%;
  margin-bottom: 2vw;
  font-family: "Yanone Kaffeesatz", cursive;
  text-shadow: 0px 0px 0 rgb(236, 180, 180), 1px 0px 0 rgb(236, 180, 180),
    2px 0px 0 rgb(202, 202, 202), 3px 0px 0 rgb(187, 187, 187),
    4px 0px 0 rgb(173, 173, 173), 5px 0px 0 rgb(236, 180, 180),
    6px 0px 0 rgb(144, 144, 144), 7px 0px 6px rgba(0, 0, 0, 0.6),
    7px 0px 1px rgba(0, 0, 0, 0.5), 0px 0px 6px rgba(0, 0, 0, 0.2);
  z-index: 11;
  top: 0;
  cursor: pointer;
}
.text-box {
  padding: 0.5vw;
  width: 23vw;
  display: flex;
  height: 3vw;
  font-size: 2vw;
  margin: 2vw;
  margin-left: 36%;
  border-radius: 30px;
}
#submit {
  background-color: rgb(255, 0, 234);
  width: 8vw;
  height: 3vw;
  border-radius: 20px;
  font-size: 2vw;
  margin-left: 44%;
  cursor: pointer;
}
h2 {
  font-size: 1.5vw;
  margin-left: 36%;
}
</style>
