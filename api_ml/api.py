from flask import Flask, jsonify, request, Request
from ml import named_entity_recognition, image_classification, generate_title, load_ocr_file
import urllib.parse

app = Flask(__name__)

@app.route('/')
def index():
    documentId = request.args.get('documentId')
    path = request.args.get('path')

    if documentId is not None and path is not None:

        ocr = load_ocr_file(documentId)
        ner_results = named_entity_recognition(ocr)
        title = generate_title(ocr)

        path = urllib.parse.unquote(path)
        category = image_classification(path)
        
        return jsonify({'namedEntity': ner_results, 'category': category, 'title': title})
    else:
        return jsonify({'error': 'Please provide an "documentId" in the request parameter'}), 404

if __name__ == "__main__":
    from waitress import serve
    serve(app, host="0.0.0.0", port=5000)