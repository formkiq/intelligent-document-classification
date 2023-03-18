import React, { useState } from 'react';
import SearchResultsTable from './SearchResultsTable';
import './SearchComponent.css';

function SearchComponent() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedOrganization, setSelectedOrganization] = useState('');
  const [selectedLocation, setSelectedLocation] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [results, setResults] = useState([]);

  // Function to handle changes to the search input
  const handleSearchTermChange = (event) => {
    setSearchTerm(event.target.value);
  };

  // Function to handle changes to the organization dropdown
  const handleOrganizationChange = (event) => {
    setSelectedOrganization(event.target.value);
  };

  // Function to handle changes to the location dropdown
  const handleLocationChange = (event) => {
    setSelectedLocation(event.target.value);
  };

  // Function to handle changes to the type dropdown
  const handleTypeChange = (event) => {
    setSelectedType(event.target.value);
  };

  // Function to handle the search button click
  const handleSearchButtonClick = (event) => {
    event.preventDefault();
    // Perform search logic here
    console.log('Searching for:', searchTerm);
    console.log('Organization:', selectedOrganization);
    console.log('Location:', selectedLocation);
    console.log('Type:', selectedType);

    // const searchParams = {
    //   selectedOrganization,
    //   searchTerm,
    //   selectedLocation,
    //   selectedType
    // };

    const searchParams = {
      "text":searchTerm
    };

    fetch('http://localhost:8080/search', {
      method: 'POST',
      body: JSON.stringify(searchParams),
      headers: {
        'Content-Type': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => setResults(data));

    setResults([
      { id: 1, name: 'Result 1', organization: 'This is result 1' },
      { id: 2, name: 'Result 2', organization: 'This is result 2' },
      { id: 3, name: 'Result 3', organization: 'This is result 3' },
    ]);
  };

  return (
    <div className="search-component-container">
      <div className="search-input-container">
        <input
          type="text"
          placeholder="Search"
          value={searchTerm}
          onChange={handleSearchTermChange}
          style={{ width: '50%' }}
        />
        <button onClick={handleSearchButtonClick}>Search</button>
      </div>
      <div className="dropdowns-container">
        <select value={selectedOrganization} onChange={handleOrganizationChange}>
          <option value="">Select Organization</option>
          <option value="Org 1">Org 1</option>
          <option value="Org 2">Org 2</option>
          <option value="Org 3">Org 3</option>
        </select>
        <select value={selectedLocation} onChange={handleLocationChange}>
          <option value="">Select Location</option>
          <option value="Location 1">Location 1</option>
          <option value="Location 2">Location 2</option>
          <option value="Location 3">Location 3</option>
        </select>
        <select value={selectedType} onChange={handleTypeChange}>
          <option value="">Select Type</option>
          <option value="Type 1">Type 1</option>
          <option value="Type 2">Type 2</option>
          <option value="Type 3">Type 3</option>
        </select>
      </div>
      {results.length > 0 && <SearchResultsTable results={results} />}
    </div>
  );
}

export default SearchComponent;
