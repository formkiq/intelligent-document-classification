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
import React, { useState } from 'react';
import { useAuth } from "../hooks/useAuth";

// Generate Order Data
function createData(
  id: number,
  date: string,
  name: string,
  shipTo: string,
  paymentMethod: string,
  amount: number,
) {
  return { id, date, name, shipTo, paymentMethod, amount };
}

const rows = [
  createData(
    0,
    '16 Mar, 2019',
    'Elvis Presley',
    'Tupelo, MS',
    'VISA ⠀•••• 3719',
    312.44,
  ),
  createData(
    1,
    '16 Mar, 2019',
    'Paul McCartney',
    'London, UK',
    'VISA ⠀•••• 2574',
    866.99,
  ),
  createData(2, '16 Mar, 2019', 'Tom Scholz', 'Boston, MA', 'MC ⠀•••• 1253', 100.81),
  createData(
    3,
    '16 Mar, 2019',
    'Michael Jackson',
    'Gary, IN',
    'AMEX ⠀•••• 2000',
    654.39,
  ),
  createData(
    4,
    '15 Mar, 2019',
    'Bruce Springsteen',
    'Long Branch, NJ',
    'VISA ⠀•••• 5919',
    212.79,
  ),
];

export const SearchPage = ({ title, icon }) => {
  
  const { logout } = useAuth();
  const user = JSON.parse(window.localStorage.getItem("user"));
  const [searchTerm, setSearchTerm] = useState("");
  const [results, setResults] = useState([]);

  const handleChange = (event) => {
    setSearchTerm(event.target.value);
  };

  const handleKeyDown = (event) => {

    if (event.key === 'Enter') {

      let search = {"text":event.target.value};

      fetch('https://localhost/api/search', {
        method: 'POST',
        body: JSON.stringify(search),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + user.access_token,
        }
      })
      .then(response => response.json())
      .then(data => {
        if (data && data.documents) {
          setResults(data.documents);
        }
        console.log("DATA: " + JSON.stringify(data));
      })
      .catch(error => {
        if (error.message && error.message.includes("401")) {
          logout();
        } else {
          console.error(error); 
        }
      });
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
