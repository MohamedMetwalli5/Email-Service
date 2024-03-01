import { reactive } from "vue";

export const SharedUserEmail = reactive({
    TheUserEmail: "",
    setUserEmail(value) {
        this.TheUserEmail = value;
    }
});
