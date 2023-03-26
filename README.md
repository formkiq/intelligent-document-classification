
<div>
Intelligent Document Classification is an web-based document classification application deployed on the [DAIR Cloud](https://www.canarie.ca/cloud). Using Optical Character Recognition, Natural Language Processing, and Full-Text Search, the Automated Document Classification and Discovery BoosterPack can automate the creation of document metadata.
<br />
<br />
</div>

### Architecture

<div align="center" style="margin: 30px;">
<a href="https://formkiq.com/">
  <img src="https://github.com/formkiq/intelligent-document-classification/raw/v1/docs/architecture/formkiq_automated_document_classification.png" style="width:600px;" align="center" />
</a>
<br />
<br />

</div>

### Features

✅ Optical character recognition using [Tesseract](https://github.com/tesseract-ocr/tesseract)

✅ Natural language processing using [PyTorch](https://pytorch.org/) and [Hugging Face](https://huggingface.co) Machine Learning Datasets

✅ Fulltext search and metadata storage using Elasticsearch

✅ Web interface for document upload and document search using ReactJS

✅ Event Streaming using Apache Kafka

### Run Local

```shell
# Build application
docker-compose -f docker-compose-dev.yml build

# Run application
docker-compose -f docker-compose-dev.yml up -d
```

## License

Intelligent Document Classifcation is available under the Apache License V2.
