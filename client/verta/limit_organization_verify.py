from verta import Client
from verta._internal_utils._utils import generate_default_name

client = Client(host="trial.dev.verta.ai")
org_name = generate_default_name()
org = client._create_organization(org_name)

try:
    endpoint = client.set_endpoint(generate_default_name(), workspace=org_name)
    endpoint.delete()
except Exception as err:
    print(err)
finally:
    org.delete()
