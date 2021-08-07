<template>
  <div id="container">
    <h1 id="title" onclick="window.location.href='/emails';">Seamail</h1>
    <form id="login-form">
      <input
        type="email"
        placeholder="Email"
        required
        class="text-box"
        v-model="email"
      />
      <input
        type="password"
        placeholder="Password"
        required
        v-model="password"
        class="text-box"
      />
      <input type="button" value="Sign in" id="submit" @click="SignIn()" />
    </form>
    <h2>
      Don't have an account?
      <a onclick="window.location.href='/signup';">Sign up</a>
    </h2>
  </div>
</template>

<script>
import firebase from "firebase";
// import "firebase/auth";
export default {
  name: "Home",
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
    SignIn() {
      firebase
        .auth()
        .signInWithEmailAndPassword(this.email, this.password)
        .then(
          (user) => {
            console.log("Signed in successfully!");
            console.log(user.data);
            // here we want the user to go to its mail box
          },
          (err) => {
            console.log("There is no user with this email and password!");
            alert(err);
          }
        );
    },
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
  margin-top: 8%;
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
  width: 22vw;
  display: flex;
  height: 3vw;
  font-size: 2vw;
  margin: 2vw;
  margin-left: 38%;
  border-radius: 30px;
}
#submit {
  background-color: rgb(255, 0, 234);
  width: 8vw;
  height: 3vw;
  border-radius: 20px;
  font-size: 2vw;
  margin-left: 45%;
  cursor: pointer;
}
h2 {
  font-size: 1.5vw;
  margin-left: 39%;
}
</style>
