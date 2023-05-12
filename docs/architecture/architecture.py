from diagrams import Cluster, Diagram
from diagrams.aws.compute import ECS, EKS, Lambda
from diagrams.aws.database import Redshift
from diagrams.aws.integration import SQS
from diagrams.aws.storage import S3
from diagrams.custom import Custom
from diagrams.onprem.queue import Kafka
from diagrams.aws.network import APIGateway
from diagrams.elastic.elasticsearch import Elasticsearch, Kibana

with Diagram("FormKiQ Automated Document Classification", show=False):

    react = [Custom("UI","./react.png")]
    api = APIGateway("API")
    kafka = Kafka("Kafka")

    with Cluster("Document Classification"):
        with Cluster("Micronaut Workers"):
            workers0 = [Custom("Worker","./micronaut-sally.png")]
            workers1 = [Custom("Worker","./micronaut-sally.png")]
            
        pytorch = Custom("","./pytorch.png")
        tesseract = Custom("","./tesseract.png")

    with Cluster("Document Search"):
         db = Elasticsearch("Elasticsearch")
         db -Kibana("Kibana")

    react >> api >> kafka >> workers0 >> pytorch >> workers0
    react >> api >> kafka >> workers1 >> tesseract >> workers1

    pytorch >> db
    api >> db