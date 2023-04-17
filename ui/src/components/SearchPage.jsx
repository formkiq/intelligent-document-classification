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
import DownloadIcon from '@mui/icons-material/DownloadOutlined';
import StyleIcon from '@mui/icons-material/StyleOutlined';
import Stack from '@mui/material/Stack';
import React, { useEffect, useState } from 'react';
import { useAuth } from "../hooks/useAuth";
import { DataGrid, GridColDef, GridValueGetterParams, GridActionsCellItem } from '@mui/x-data-grid';
import moment from 'moment'
import { display } from '@mui/system';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';

export const SearchPage = ({ title, icon }) => {
  
  const { logout } = useAuth();
  const user = JSON.parse(window.localStorage.getItem("user"));
  const [searchTerm, setSearchTerm] = useState("");
  const [results, setResults] = useState([]);
  const [tags, setTags] = useState([]);
  const [documentId, setDocumentId] = useState("");
  const [displayTags, setDisplayTags] = useState("none");
  const [open, setOpen] = React.useState(false);

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleAddTagSubmit = (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const tagKey = data.get("tagKey");
    const tagValue = data.get("tagValue");
    const found = results.find(element => element.documentId === documentId);

    if (!found.tags) {
      found.tags = [];
    }

    if (found.tags[tagKey]) {
      found.tags[tagKey].push(tagValue);
    } else {
      found.tags[tagKey] = [tagValue];
    }

    let body = {"tags":found.tags};
    updateDocument(documentId, body);
    showTags(documentId);

    handleClose();
  };

  function updateDocument(documentId, body) {
    if (user && user.access_token) {

      fetch('/api/documents/' + documentId, {
        method: 'PATCH',
        body: JSON.stringify(body),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + user.access_token,
        }
      })
      .then(response => {

        if (response.status === 401) {
          logout();
        }
        return "";
      })
      .catch(error => {
        console.log(error);
      });
    }
  }

  const processRowUpdate = React.useCallback(
    async (newRow: GridRowModel) => {

      if (user && user.access_token) {
        let body = {"title":newRow.title};
        updateDocument(newRow.documentId, body);
      }

      return newRow;
    },
    [],
  );

  const handleProcessRowUpdateError = React.useCallback((error: Error) => {
    console.log("ERROR: " + error);
  }, []);

  const columns: GridColDef[] = [
    { field: 'filename', headerName: 'Filename', flex: 0.5 },
    {
      field: 'title',
      headerName: 'Title',
      flex: 1,
      editable: true
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
      width: 140,
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 135,
      cellClassName: 'actions',
      getActions: ({ id }) => {
        const found = results.find(element => element.documentId === id);
        if (found) {
          return [
          <GridActionsCellItem
            icon={<StyleIcon />}
            label="Tags"
            className="textPrimary"
            onClick={(event) => showTags(id)}
            color="inherit"
          />,
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

  const tagColumns: GridColDef[] = [
    { field: 'key', headerName: 'Key', flex: 1 },
    { field: 'value', headerName: 'Value', flex: 1 },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 135,
      cellClassName: 'actions',
      getActions: ({ id }) => {
          return [
          <GridActionsCellItem
            icon={<DeleteIcon />}
            label="Delete Tag"
            onClick={(event) => deleteDocumentTag(id)}
            color="inherit"
          />,
        ]
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

    setOpen(false);
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
          console.log("DOCUMENTS: " + JSON.stringify(data.documents));
          setResults(data.documents);
          setTags([]);
          setDisplayTags("none");
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

  function showTags(documentId) {
    var tags = [];
    const found = results.find(element => element.documentId === documentId);
    if (found) {
      setDocumentId(documentId);
      setDisplayTags("block");
      var id = 0;
      for (let key of Object.keys(found.tags)) {
        let values = found.tags[key];
        values.forEach(value => {
          tags.push({id:id, key:key, value:value});
          id = id+1;
        });
      }
    }

    setTags(tags);
  }

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

  function deleteDocumentTag(id) {

    const found = tags.find(element => element.id === id);
    if (user && user.access_token && found) {
      fetch('/api/documents/' + documentId + '/tags/' + found.key + '/' + found.value, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + user.access_token,
        }
      })
      .then(response => {

        if (response.ok) {

          setTags((prevResults) =>
            prevResults.filter((row, index) => row.id !== id)
          );

        } else if (response.status === 401) {
          logout();
        }
        return "";
      })
      .catch(error => {
        console.log(error);
      });
    }
  }

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
      <Dialog open={open} onClose={handleClose}>
        <DialogTitle>Add Tag To Document</DialogTitle>
        <Box component="form" onSubmit={handleAddTagSubmit} noValidate>
          <DialogContent>
            <TextField
              autoFocus
              margin="dense"
              id="tagKey"
              name="tagKey"
              label="Tag Key"
              type="text"
              fullWidth
              variant="standard"
            />
            <TextField
              margin="dense"
              id="tagValue"
              name="tagValue"
              label="Tag Value"
              type="text"
              fullWidth
              variant="standard"
            />
          </DialogContent>
          <DialogActions>
            <Button variant="outlined" onClick={handleClose}>Cancel</Button>
            <Button variant="outlined" type="submit">Add Tag</Button>
          </DialogActions>
        </Box>
      </Dialog>
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
        <Typography component="small">
          search by text or tag using "[tagKey]=tagValue"
        </Typography>
        <React.Fragment>
          <Box sx={{ width: '100%', marginTop: 5 }}>
            <h2>Document(s)</h2>
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
                    pageSize: 10,
                  },
                },
              }}
              pageSizeOptions={[10]}
              disableRowSelectionOnClick
              getRowId={(data) => data.documentId}
              getRowHeight={() => 'auto'}
              processRowUpdate={processRowUpdate}
              onProcessRowUpdateError={handleProcessRowUpdateError}
            />
          </Box>

          <Box sx={{ width: '100%', marginTop: 5, display: displayTags }}>
            <h2>Document Tag(s)</h2>
            <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
              <Button variant="outlined" onClick={handleClickOpen}>
                Add a Tag
              </Button>
            </Stack>

            <DataGrid
              rows={tags}
              columns={tagColumns}
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
            />

          </Box>
        </React.Fragment>
      </Box>
    </Container>
  );
};
