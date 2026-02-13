import React,{useState} from "react";
import { loginStatic } from "../utils/auth";
import { useNavigate } from "react-router-dom";
import { TextField, Button, Paper } from "@mui/material";

export default function LoginPage(){
  const [u,setU]=useState("admin");
  const [p,setP]=useState("admin");
  const nav=useNavigate();

  const submit=()=>{
    if(loginStatic(u,p)) nav("/");
    else alert("Invalid credentials");
  };

  return (
    <Paper sx={{p:4, maxWidth:300, m:"100px auto"}}>
      <TextField fullWidth label="Username" value={u} onChange={e=>setU(e.target.value)} />
      <TextField fullWidth label="Password" type="password" value={p} onChange={e=>setP(e.target.value)} sx={{mt:2}} />
      <Button fullWidth variant="contained" sx={{mt:2}} onClick={submit}>Login</Button>
    </Paper>
  );
}
