import Container from "@mui/material/Container";
import Avatar from "@mui/material/Avatar";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import React, { useState } from 'react';
import axios from 'axios';
import Dropzone from 'react-dropzone';
import { useAuth } from "../hooks/useAuth";

export const AddPage = ({ title, icon }) => {

  const [files, setFiles] = useState([]);

  const { logout } = useAuth();
  const user = JSON.parse(window.localStorage.getItem("user"));

  const handleFileUpload = (acceptedFiles) => {

    const formData = new FormData();
    formData.append('file', acceptedFiles[0]);
    
    axios.post('https://localhost/api/upload', formData, 
      { 
        headers: {
          'Authorization': 'Bearer ' + user.access_token,
        }
      })
      .then(response => {
        console.log(response);

        let obj = {path: acceptedFiles[0].path, status: "success"};
        setFiles(current => [...current, obj]);
      })
      .catch(error => {
        if (error.message && error.message.includes("401")) {
          logout();
        } else {
          console.error(error); 
          let obj = {path: acceptedFiles[0].path, status: "failed"};
          setFiles(current => [...current, obj]);
        }
      });
  }

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: "flex",
          flexDirection: "column",
          alignItems: "center"
        }}
      >
        <Avatar sx={{ m: 1, bgcolor: "primary.main" }}>{icon}</Avatar>
        <Typography component="h1" variant="h5">
          {title}
        </Typography>

        <section className="container dropzone">
          <Dropzone onDrop={handleFileUpload}>
          {({getRootProps, getInputProps}) => (
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <p>Drag and drop a file here, or click to select a file</p>
            </div>
          )}
          </Dropzone>
        </section>

        { (files && files.length > 0) && 
          <aside>
            <h4>Files</h4>
            <ul>
              {files.map((f, index) => (
                <li key={index}>
                    <span>name: {f.path}</span>&nbsp;<span>status: {f.status}</span>
                </li>
              ))}
            </ul>
          </aside>
        }
      </Box>
    </Container>
  );
};
