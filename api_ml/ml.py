from transformers import AutoTokenizer, AutoModelForTokenClassification, AutoModelForSeq2SeqLM
from transformers import pipeline
from transformers import AutoImageProcessor, AutoModelForImageClassification
import torch
from PIL import Image
import os

titleTokenizer = AutoTokenizer.from_pretrained("deep-learning-analytics/automatic-title-generation")
titleModel = AutoModelForSeq2SeqLM.from_pretrained("deep-learning-analytics/automatic-title-generation")

tokenizer = AutoTokenizer.from_pretrained("dslim/bert-base-NER")
nerModel = AutoModelForTokenClassification.from_pretrained("dslim/bert-base-NER")

processor = AutoImageProcessor.from_pretrained("microsoft/dit-base-finetuned-rvlcdip")
imageModel = AutoModelForImageClassification.from_pretrained("microsoft/dit-base-finetuned-rvlcdip")

def load_ocr_file(documentId):
    directory = os.environ.get('STORAGE_DIRECTORY', '/app/data')
    filename = os.path.join(directory, f"{documentId}/ocr.txt")
    print(f"loading file: {filename}")
    if os.path.isfile(filename):
        with open(filename, 'r') as f:
            data = f.read()
        return data
    else:
        return None

def named_entity_recognition(ocr):

  if ocr is not None:
    nlp = pipeline("ner", model=nerModel, tokenizer=tokenizer, aggregation_strategy="simple")
    ner_results = nlp(ocr)
    for x in ner_results:
      x["score"] = str(x["score"])
    return ner_results

  return []

def image_classification(path):

  try:
    # Open the image
    image = Image.open(path)

    # Convert to RGB if necessary
    if image.mode != "RGB":
        image = image.convert("RGB")

    inputs = processor(images=image, return_tensors="pt")
    outputs = imageModel(**inputs)
    logits = outputs.logits

    # model predicts one of the 16 RVL-CDIP classes
    predicted_class_idx = logits.argmax(-1).item()
    return imageModel.config.id2label[predicted_class_idx]
    
  except Exception as e:
    return "unknown"

def generate_title(text):
  max_len = 120
  tokenized_inputs = titleTokenizer(text, padding='max_length', truncation=True, max_length=max_len, return_attention_mask=True, return_tensors='pt')

  inputs={"input_ids": tokenized_inputs['input_ids'], "attention_mask": tokenized_inputs['attention_mask']}
  results= titleModel.generate(input_ids= inputs['input_ids'], attention_mask=inputs['attention_mask'], do_sample=True, max_length=120, top_k=120, top_p=0.98, early_stopping=True, num_return_sequences=1)
  answer = titleTokenizer.decode(results[0], skip_special_tokens=True)
  return answer