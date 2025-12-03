import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
    apiKey: "AIzaSyBNz997erJsQ2xY_25ZNYCuaQjpVSPE-uk",
    authDomain: "studyflow-forum.firebaseapp.com",
    projectId: "studyflow-forum",
    storageBucket: "studyflow-forum.firebasestorage.app",
    messagingSenderId: "985177616944",
    appId: "1:985177616944:web:95faf04d7dc2143cbfb3f5",
    measurementId: "G-ZJQ23MEK26"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

export { auth };