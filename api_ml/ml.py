from transformers import AutoTokenizer, AutoModelForTokenClassification, AutoModelForSeq2SeqLM
from transformers import pipeline
from transformers import AutoImageProcessor, AutoModelForImageClassification
import torch
from PIL import Image
import os

titleTokenizer = AutoTokenizer.from_pretrained("snrspeaks/t5-one-line-summary")
titleModel = AutoModelForSeq2SeqLM.from_pretrained("snrspeaks/t5-one-line-summary")

tokenizer = AutoTokenizer.from_pretrained("dslim/bert-large-NER")
nerModel = AutoModelForTokenClassification.from_pretrained("dslim/bert-large-NER")

vision_classifier = pipeline(model="microsoft/dit-base-finetuned-rvlcdip")

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

def group_by_key(lst, key):
    grouped = {}
    for d in lst:
        grouped.setdefault(d[key], []).append(d)
    return grouped

def named_entity_recognition(ocr):

  if ocr is not None:
    nlp = pipeline("ner", model=nerModel, tokenizer=tokenizer)
    ner_results = nlp(ocr)

    newlist = []
    element = {}

    for ner_dict in ner_results:

        if ner_dict["word"].startswith("##"):
            ner_dict["word"] = ner_dict["word"].replace("##","")

        if ner_dict["entity"].startswith("B"):

            element = {}
            newlist.append(element)
            element["word"] = ner_dict["word"]
            element["score"] = str(ner_dict["score"])
            element["start"] = ner_dict["start"]
            element["end"] = ner_dict["end"]
            element["entity"] = ner_dict["entity"].replace("B-","")

        elif ner_dict['entity'].startswith("I"):

            if element["end"] != ner_dict["start"]:
                element["word"] += " " + ner_dict["word"]
            else:
                element["word"] += ner_dict["word"]

            element["start"] = ner_dict["start"]
            element["end"] = ner_dict["end"]

    groupby = group_by_key(newlist, "entity")

    for group in groupby:
        list = groupby[group]
        for item in list:
            del item["start"]
            del item["end"]
            del item["entity"]

    return groupby

  return {}

def image_classification(path):

  try:
    # Open the image
    image = Image.open(path)

    # Convert to RGB if necessary
    if image.mode != "RGB":
        image = image.convert("RGB")

    result = vision_classifier(images=image)[0]
    return {"score": str(result["score"]), "label": result["label"]}
    
  except Exception as e:
    return {"score": "1.0", "label": "uncategorized"}

def generate_title(text):

  input_ids = titleTokenizer.encode("summarize: " + text, return_tensors="pt", add_special_tokens=True)

  generated_ids = titleModel.generate(
    input_ids=input_ids,
    num_beams=5,
    max_length=50,
    repetition_penalty=2.5,
    length_penalty=1,
    early_stopping=True,
    num_return_sequences=3,
  )

  preds = [
    titleTokenizer.decode(g, skip_special_tokens=True, clean_up_tokenization_spaces=True)
    for g in generated_ids
  ]

  return preds[0]
