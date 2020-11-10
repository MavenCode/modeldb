from verta import Client

client = Client(host="trial.dev.verta.ai")

endpoint = client.set_endpoint("/first-endpoint")
try:
    endpoint2 = client.create_endpoint("/second-endpoint")
except Exception as err:
    print(err)
finally:
    endpoint.delete()