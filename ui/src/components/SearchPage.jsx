import { Container, TextField, InputAdornment } from "@mui/material";
import Avatar from "@mui/material/Avatar";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import SearchIcon from "@mui/icons-material/Search";
import React, { useEffect, useState } from 'react';
import { useAuth } from "../hooks/useAuth";

export const SearchPage = ({ title, icon }) => {
  
  const { logout } = useAuth();
  const user = JSON.parse(window.localStorage.getItem("user"));
  const [searchTerm, setSearchTerm] = useState("");
  const [results, setResults] = useState([]);

  useEffect(() => {
    search("");
  }, []);

  const handleChange = (event) => {
    setSearchTerm(event.target.value);
  };

  function search(text) {

    let search = {"text":text};

    if (user.access_token) {
      fetch('/api/search', {
        method: 'POST',
        body: JSON.stringify(search),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + user.access_token,
        }
      })
      .then(response => {

        if (response.ok) {
          let data = response.json();
          return data;
        } else if (response.status === 401) {
          logout();
          return "";
        }
      })
      .then(data => {
        if (data && data.documents) {
          setResults(data.documents);
        }
      })
      .catch(error => {
        console.log(error);
      });
    }
  }

  const handleKeyDown = (event) => {

    if (event.key === 'Enter') {
      search(event.target.value);
    }
  };

  return (
    <Container component="main" maxWidth="lg">
      <Box
        sx={{
          marginTop: 8,
          width:"100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center"
        }}
      >
        <Avatar sx={{ m: 1, bgcolor: "primary.main" }}>{icon}</Avatar>
        <Typography component="h1" variant="h5">
          {title}
        </Typography>

         <TextField
        id="search"
        type="search"
        label="Search"
        value={searchTerm}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        sx={{ width: 600 }}
        InputProps={{
          endAdornment: (
            <InputAdornment position="end">
              <SearchIcon />
            </InputAdornment>
          ),
        }}
      />
        <React.Fragment>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Id</TableCell>
                <TableCell>Title</TableCell>
                <TableCell>Content Type</TableCell>
                <TableCell>Inserted Date</TableCell>
                <TableCell>Tag(s)</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {results.map((row) => (
                <TableRow key={row.documentId}>
                  <TableCell>{row.documentId}</TableCell>
                  <TableCell>{row.title}</TableCell>
                  <TableCell>{row.contentType}</TableCell>
                  <TableCell>{row.insertedDate}</TableCell>
                  <TableCell>
                  {
                    row.tags && Object.entries(row.tags).map(([key, values]) => (  
                      <p>{key} : {values.join(", ")}</p>
                    ))
                  }
                  </TableCell>
                  <TableCell>{row.status}</TableCell>
                  <TableCell align="right"></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </React.Fragment>
      </Box>
    </Container>
  );
};
