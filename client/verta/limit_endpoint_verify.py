from verta import Client
from verta._internal_utils._utils import generate_default_name

client = Client(host="trial.dev.verta.ai")

endpoint = client.set_endpoint(generate_default_name())
try:
    endpoint2 = client.create_endpoint(generate_default_name())
except Exception as err:
    print(err)
finally:
    endpoint.delete()