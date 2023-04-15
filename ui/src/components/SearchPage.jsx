import { Container, TextField, InputAdornment } from "@mui/material";
import Avatar from "@mui/material/Avatar";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import ButtonGroup from "@mui/material/ButtonGroup";
import Typography from "@mui/material/Typography";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import IconButton from "@mui/material/IconButton";
import SearchIcon from "@mui/icons-material/Search";
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import DownloadIcon from '@mui/icons-material/Download';
import React, { useEffect, useState } from 'react';
import { useAuth } from "../hooks/useAuth";
import { DataGrid, GridColDef, GridValueGetterParams, GridActionsCellItem } from '@mui/x-data-grid';
import moment from 'moment'

export const SearchPage = ({ title, icon }) => {
  
  const { logout } = useAuth();
  const user = JSON.parse(window.localStorage.getItem("user"));
  const [searchTerm, setSearchTerm] = useState("");
  const [results, setResults] = useState([]);

  const columns: GridColDef[] = [
    { field: 'filename', headerName: 'Filename', flex: 0.5 },
    {
      field: 'title',
      headerName: 'Title',
      flex: 1
    },
    {
      field: 'contentType',
      headerName: 'Content-Type',
      width: 150,
    },
    {
      field: 'insertedDate',
      headerName: 'Inserted Date',
      width: 200,
      valueGetter: (params: GridValueGetterParams) =>
      `${formatDateString(params.row.insertedDate) || ''}`,
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 100,
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 135,
      cellClassName: 'actions',
      getActions: ({ id }) => {
        const found = results.find(element => element.documentId === id);
        if (found && found.status === "COMPLETE") {
          return [
          <GridActionsCellItem
            icon={<DownloadIcon />}
            label="Download"
            className="textPrimary"
            onClick={(event) => downloadDocument(id)}
            color="inherit"
          />,
          <GridActionsCellItem
            icon={<DeleteIcon />}
            label="Delete"
            onClick={(event) => deleteDocument(id)}
            color="inherit"
          />,
          ]
        }

        return [];
      },
    },
  ];

  useEffect(() => {
    search("");
  }, []);

  const handleChange = (event) => {
    setSearchTerm(event.target.value);
  };

  function formatDateString(dateString) {
    return moment(dateString).format('MMM D, YYYY hh:mm:ss a')
  }

  function search(text) {

    let search = {"text":text};

    if (user && user.access_token) {
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

  function downloadDocument(documentId) {
    
    if (user && user.access_token && documentId) {
      fetch('/api/documents/' + documentId + '/content', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + user.access_token,
        }
      })
      .then(res => res.blob())
      .then(data => {
        var a = document.createElement("a");
        a.href = window.URL.createObjectURL(data);

        const found = results.find(element => element.documentId === documentId);
        if (found) {
          a.download = found.filename;
        }
        a.click();
      })
      .catch(error => {
        console.log(error);
      });
    }
  };

  function deleteDocument(documentId) {
    
    if (user && user.access_token && documentId) {
      fetch('/api/documents/' + documentId, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + user.access_token,
        }
      })
      .then(response => {

        if (response.ok) {

          setResults((prevResults) =>
            prevResults.filter((row, index) => row.documentId !== documentId)
          );

          let data = response.json();
          return data;
        } else if (response.status === 401) {
          logout();
          return "";
        }
      })
      .catch(error => {
        console.log(error);
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
          <Box sx={{ height: 400, width: '100%', marginTop: 5 }}>
            <DataGrid
              sx={{
                '&.MuiDataGrid-root--densityCompact .MuiDataGrid-cell': { py: '8px' },
                '&.MuiDataGrid-root--densityStandard .MuiDataGrid-cell': { py: '15px' },
                '&.MuiDataGrid-root--densityComfortable .MuiDataGrid-cell': { py: '22px' },
              }}

              rows={results}
              columns={columns}
              autoHeight
              initialState={{
                pagination: {
                  paginationModel: {
                    pageSize: 5,
                  },
                },
              }}
              pageSizeOptions={[5]}
              disableRowSelectionOnClick
              getRowId={(data) => data.documentId}
              getRowHeight={() => 'auto'}
            />

          </Box>
        </React.Fragment>
      </Box>
    </Container>
  );
};
