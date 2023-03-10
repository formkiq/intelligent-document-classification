from transformers import AutoTokenizer, AutoModelForTokenClassification
from transformers import pipeline
from transformers import AutoImageProcessor, AutoModelForImageClassification
import torch
from PIL import Image
import os

tokenizer = AutoTokenizer.from_pretrained("dslim/bert-base-NER")
nerModel = AutoModelForTokenClassification.from_pretrained("dslim/bert-base-NER")

processor = AutoImageProcessor.from_pretrained("microsoft/dit-base-finetuned-rvlcdip")
imageModel = AutoModelForImageClassification.from_pretrained("microsoft/dit-base-finetuned-rvlcdip")

def load_ocr_file(documentId):
    directory = os.environ.get('FILE_DIRECTORY', '/app/data')
    filename = os.path.join(directory, f"{documentId}/ocr.txt")
    print(f"loading file: {filename}")
    if os.path.isfile(filename):
        with open(filename, 'r') as f:
            data = f.read()
        return data
    else:
        return None

def named_entity_recognition(documentId):

  nlp = pipeline("ner", model=nerModel, tokenizer=tokenizer, aggregation_strategy="simple")
  example = load_ocr_file(documentId)

  if example is not None:
    ner_results = nlp(example)
    for x in ner_results:
      x["score"] = str(x["score"])
    return ner_results

  return []

def image_classification(documentId):
  image = Image.open('/app/receipt.png').convert('RGB')

  inputs = processor(images=image, return_tensors="pt")
  outputs = imageModel(**inputs)
  logits = outputs.logits

  # model predicts one of the 16 RVL-CDIP classes
  predicted_class_idx = logits.argmax(-1).item()
  return imageModel.config.id2label[predicted_class_idx]