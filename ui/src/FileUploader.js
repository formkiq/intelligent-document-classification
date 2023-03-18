import React from 'react';
import axios from 'axios';
import Dropzone from 'react-dropzone';
import './styles.css';

class FileUploader extends React.Component {
  handleFileUpload = (files) => {
    const formData = new FormData();
    formData.append('file', files[0]);

    axios.post('http://localhost:8080/upload', formData)
      .then(response => {
        console.log(response);
        // Display success message to user
      })
      .catch(error => {
        console.error(error);
        // Display error message to user
      });
  }

  render() {
    return (
      <Dropzone onDrop={this.handleFileUpload}>
        {({getRootProps, getInputProps}) => (
          <div {...getRootProps()}>
            <input {...getInputProps()} />
            <p>Drag and drop a file here, or click to select a file</p>
          </div>
        )}
      </Dropzone>
    );
  }
}

export default FileUploader;