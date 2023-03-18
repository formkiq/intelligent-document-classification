import './App.css';
import FileUploader from './FileUploader';
import SearchComponent from './SearchComponent';
import './styles.css';

function App() {
  return (
    <div>
      <SearchComponent />
      <h1>File Uploader</h1>
      <FileUploader />
    </div>
  );
}

export default App;
