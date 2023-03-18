import React from 'react';
import './SearchResultsTable.css';

function SearchResultsTable({ results }) {
  if (!results || results.length === 0) {
    return <p>No results found.</p>;
  }

  return (
    <table className="search-results-table" style={{ width: '75%' }}>
      <thead>
        <tr>
          <th>Name</th>
          <th>Organization</th>
          <th>Location</th>
          <th>Type</th>
        </tr>
      </thead>
      <tbody>
        {results.map((result) => (
          <tr key={result.id}>
            <td>{result.name}</td>
            <td>{result.organization}</td>
            <td>{result.location}</td>
            <td>{result.type}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default SearchResultsTable;
