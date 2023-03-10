from flask import Flask, jsonify, request, Request
from ml import named_entity_recognition, image_classification

app = Flask(__name__)

@app.route('/')
def index():
    documentId = request.args.get('documentId')
    if documentId:
        ner_results = named_entity_recognition(documentId)
        category = image_classification(documentId)
        return jsonify({'namedEntity': ner_results, 'category': category})
    else:
        return jsonify({'error': 'Please provide an "documentId" in the request parameter'}), 404

if __name__ == "__main__":
    from waitress import serve
    serve(app, host="0.0.0.0", port=5000)